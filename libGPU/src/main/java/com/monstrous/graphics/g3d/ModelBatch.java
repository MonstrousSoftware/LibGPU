package com.monstrous.graphics.g3d;

import com.monstrous.LibGPU;
import com.monstrous.graphics.*;
import com.monstrous.graphics.lights.DirectionalLight;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.graphics.lights.Light;
import com.monstrous.graphics.lights.PointLight;
import com.monstrous.graphics.webgpu.*;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;
import com.monstrous.utils.Disposable;
import com.monstrous.wgpu.*;
import com.monstrous.wgpuUtils.WgpuJava;
import jnr.ffi.Pointer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModelBatch implements Disposable {

    private final int MAX_DIR_LIGHTS = 5;
    private final int MAX_POINT_LIGHTS = 5;
    private final int MAX_INSTANCES = 4096;

    private final int FRAME_UB_SIZE = 816;

    private final WGPU wgpu;
    private final Pointer device;

    private RenderPass pass;

    private UniformBuffer frameUniformBuffer;
    private Pointer frameBindGroupLayout;
    private Pointer instancingBindGroupLayout;
    private Pointer shadowBindGroupLayout;
    private Pointer pipelineLayout;
    private Pointer frameBindGroup;
    private Pointer instancingBindGroup;
    private Pointer shadowBindGroup;
    private Material prevMaterial;

    private final Pipelines pipelines;
    private Pipeline prevPipeline;
    private PipelineSpecification pipelineSpec;
    private final List<Renderable> renderables;
    private final RenderablePool pool;
    public int numPipelineSwitches;
    public Environment environment;

    private DirectionalLight defaultDirectionalLight;
    private UniformBuffer instanceBuffer;




    public ModelBatch (){
        wgpu = LibGPU.wgpu;
        device = LibGPU.device;


        pipelines = new Pipelines();
        renderables = new ArrayList<>();
        pool = new RenderablePool(1000);
        pipelineSpec = new PipelineSpecification();

        defaultDirectionalLight = new DirectionalLight(new Color(1,0,0,1), new Vector3(0, -1, 0));

        frameUniformBuffer = new UniformBuffer(FRAME_UB_SIZE, WGPUBufferUsage.CopyDst | WGPUBufferUsage.Uniform);
        frameBindGroupLayout = createFrameBindGroupLayout();
        instancingBindGroupLayout = createInstancingBindGroupLayout();
        shadowBindGroupLayout = createShadowBindGroupLayout();

        int instanceSize = 16*Float.BYTES;      // data size per instance
        instanceBuffer = new UniformBuffer(instanceSize*MAX_INSTANCES, WGPUBufferUsage.CopyDst | WGPUBufferUsage.Storage);
    }

    // todo allow hot-loading of shaders
    public void invalidatePipelines(){
        pipelines.clear();
    }


    public void begin(Camera camera){
        begin(camera, null, null, null, null);
    }

    public void begin(Camera camera, Environment environment) {
        begin(camera, environment, null, null, null);
    }

    public void begin(Camera camera, Environment environment, Color clearColor) {
        begin(camera, environment, clearColor, null, null);
    }

    public void begin(Camera camera, Environment environment, Color clearColor, Texture outputTexture, Texture depthTexture){
        this.environment = environment;

        pipelineLayout = makePipelineLayout(frameBindGroupLayout, Material.getBindGroupLayout(), instancingBindGroupLayout, shadowBindGroupLayout);

        // create a new render pass
        pass = RenderPassBuilder.create(clearColor, outputTexture,  depthTexture, LibGPU.app.configuration.numSamples);

        prevMaterial = null;
        prevPipeline = null;
        numPipelineSwitches = 0;

        writeFrameUniforms(frameUniformBuffer, camera, environment);
        frameBindGroup = makeFrameBindGroup(frameBindGroupLayout, frameUniformBuffer.getHandle());
        pass.setBindGroup(0, frameBindGroup);

        instancingBindGroup = createInstancingBindGroup(instancingBindGroupLayout, instanceBuffer.getHandle(), 16*Float.BYTES*MAX_INSTANCES);
        pass.setBindGroup(2, instancingBindGroup);

        if(environment != null && environment.renderShadows) {
            shadowBindGroup = makeShadowBindGroup(shadowBindGroupLayout);
            pass.setBindGroup(3, shadowBindGroup);
        }
    }

    public void render(ArrayList<ModelInstance> instances) {
        for(ModelInstance instance : instances)
            render(instance);
    }

    public void render(ModelInstance instance){
        instance.getRenderables((ArrayList<Renderable>) renderables, pool);
    }

    public void render(Renderable renderable) {
        renderables.add( renderable );
    }


    public void render(MeshPart meshPart, Material material, Matrix4 modelMatrix) {
        renderables.add( new Renderable(meshPart, material, modelMatrix));
    }


    public void end(){
        flush();

        wgpu.BindGroupRelease(frameBindGroup);
        wgpu.BindGroupRelease(instancingBindGroup);
        //System.out.println("materials: "+materialUniformIndex+"\t\tpipe switches: "+numPipelineSwitches);
        pass.end();
        pass = null;

        if(environment != null && environment.renderShadows) {
            wgpu.BindGroupRelease(shadowBindGroup);
        }
    }


    // sort comparator to sort on material
    //
    private static class RenderableComparator implements Comparator<Renderable> {
        @Override
        public int compare(Renderable r1, Renderable r2) {
            return r1.meshPart.hashCode() - r2.meshPart.hashCode();
        }
    }

    private final RenderableComparator comparator = new RenderableComparator();

    private MeshPart prevMeshPart;
    private int instanceCount;
    private int renderablesCount;

    private void flush() {
        // sort renderables to minimize material switching, to do: depth sorting etc.
        renderables.sort(comparator);

        prevMeshPart = null;
        instanceCount = 0;
        renderablesCount = 0;

        for(Renderable renderable : renderables) {
            emit(renderable);
            pool.free(renderable);
        }
        emitMeshPart(prevMeshPart, instanceCount, renderablesCount);
        renderables.clear();
    }

    // this will actually generate the draw calls for the renderable
    private void emit(Renderable renderable) {
        emit(renderable.meshPart, renderable.material, renderable.modelTransform);
    }

    public void emit(MeshPart meshPart, Material material, Matrix4 modelMatrix){

        // gather identical meshParts to be drawn in one call using instancing
        if(meshPart != prevMeshPart) {
            emitMeshPart(prevMeshPart, instanceCount, renderablesCount);
            instanceCount = 0;
            prevMeshPart = meshPart;
        }
        addInstance(renderablesCount, modelMatrix);
        renderablesCount++; // nr of renderables in buffer
        instanceCount++;    // nr of instances of the same meshPart

        // if we change material, bind new material
        if(material != prevMaterial) {
            prevMaterial = material;
            material.bindGroup(pass, 1);    // group 1 is material bind group
        }
    }

    // make a draw call
    private void emitMeshPart(MeshPart meshPart, int instanceCount, int renderablesCount) {
        if(meshPart == null)
            return;
        Pointer vertexBuffer = meshPart.mesh.getVertexBuffer();
        pass.setVertexBuffer(0, vertexBuffer, 0, wgpu.BufferGetSize(vertexBuffer));

        setPipeline(pass, meshPart.mesh.vertexAttributes, environment);

        if (meshPart.mesh.getIndexCount() > 0) { // indexed mesh?
            Pointer indexBuffer = meshPart.mesh.getIndexBuffer();
            pass.setIndexBuffer(indexBuffer, meshPart.mesh.indexFormat, 0, wgpu.BufferGetSize(indexBuffer));
            pass.drawIndexed( meshPart.size, instanceCount, meshPart.offset, 0, renderablesCount-instanceCount);
        }
        else
            pass.draw(meshPart.size, instanceCount, meshPart.offset, renderablesCount-instanceCount);
    }

    private String selectShaderSourceFile() {

        if (environment != null && environment.depthPass) {
            return "shaders/modelbatchDepth.wgsl";
        }
        return "shaders/modelbatchPBRUber.wgsl";
    }



    // create or reuse pipeline on demand when we know the model
    private void setPipeline(RenderPass pass, VertexAttributes vertexAttributes, Environment environment ) {

        pipelineSpec.vertexAttributes = vertexAttributes;
        pipelineSpec.environment = environment;
        pipelineSpec.shader = null;
        pipelineSpec.shaderSourceFile = selectShaderSourceFile();
        pipelineSpec.enableDepth();
        pipelineSpec.setCullMode(WGPUCullMode.Back);
        pipelineSpec.colorFormat = pass.getColorFormat();    // pixel format of render pass output
        pipelineSpec.depthFormat = pass.getDepthFormat();
        pipelineSpec.numSamples = pass.getSampleCount();

        Pipeline pipeline = pipelines.getPipeline(pipelineLayout, pipelineSpec);
        if (pipeline != prevPipeline) { // avoid unneeded switches
            pass.setPipeline(pipeline.getPipeline());
            prevPipeline = pipeline;
            numPipelineSwitches++;
        }
    }


    @Override
    public void dispose() {
        pipelines.dispose();
        wgpu.BindGroupLayoutRelease(frameBindGroupLayout);
        wgpu.BindGroupLayoutRelease(instancingBindGroupLayout);

        frameUniformBuffer.dispose();
        instanceBuffer.dispose();
        // todo
    }



    // Bind Group Layout:
    //  uniforms

    private Pointer createFrameBindGroupLayout(){

        // Define binding layout
        WGPUBindGroupLayoutEntry uniformBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(uniformBindingLayout);
        uniformBindingLayout.setBinding(0);
        uniformBindingLayout.setVisibility(WGPUShaderStage.Vertex | WGPUShaderStage.Fragment);
        uniformBindingLayout.getBuffer().setType(WGPUBufferBindingType.Uniform);
        uniformBindingLayout.getBuffer().setMinBindingSize(FRAME_UB_SIZE);
        uniformBindingLayout.getBuffer().setHasDynamicOffset(0L);

        // Create a bind group layout
        WGPUBindGroupLayoutDescriptor bindGroupLayoutDesc = WGPUBindGroupLayoutDescriptor.createDirect();
        bindGroupLayoutDesc.setNextInChain();
        bindGroupLayoutDesc.setLabel("ModelBatch Bind Group Layout (Frame)");
        bindGroupLayoutDesc.setEntryCount(1);

        bindGroupLayoutDesc.setEntries(uniformBindingLayout);
        return wgpu.DeviceCreateBindGroupLayout(device, bindGroupLayoutDesc);

    }

    private Pointer createShadowBindGroupLayout(){
        int location = 0;

        // Define binding layout
        WGPUBindGroupLayoutEntry shadowMapBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(shadowMapBindingLayout);
        shadowMapBindingLayout.setBinding(location++);
        shadowMapBindingLayout.setVisibility(WGPUShaderStage.Fragment);
        shadowMapBindingLayout.getTexture().setSampleType(WGPUTextureSampleType.Depth);
        shadowMapBindingLayout.getTexture().setViewDimension(WGPUTextureViewDimension._2D);

        WGPUBindGroupLayoutEntry samplerBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(samplerBindingLayout);
        samplerBindingLayout.setBinding(location++);
        samplerBindingLayout.setVisibility(WGPUShaderStage.Fragment);
        samplerBindingLayout.getSampler().setType(WGPUSamplerBindingType.Comparison);

        // Create a bind group layout
        WGPUBindGroupLayoutDescriptor bindGroupLayoutDesc = WGPUBindGroupLayoutDescriptor.createDirect();
        bindGroupLayoutDesc.setNextInChain();
        bindGroupLayoutDesc.setLabel("ModelBatch Bind Group Layout (Shadow)");
        bindGroupLayoutDesc.setEntryCount(location);

        bindGroupLayoutDesc.setEntries( shadowMapBindingLayout, samplerBindingLayout);
        return wgpu.DeviceCreateBindGroupLayout(device, bindGroupLayoutDesc);
    }


    // per frame bind group
    private Pointer makeFrameBindGroup(Pointer frameBindGroupLayout, Pointer uniformBuffer) {
        // Create a binding
        WGPUBindGroupEntry uniformBinding = WGPUBindGroupEntry.createDirect();
        uniformBinding.setNextInChain();
        uniformBinding.setBinding(0);  // binding index
        uniformBinding.setBuffer(uniformBuffer);
        uniformBinding.setOffset(0);
        uniformBinding.setSize(FRAME_UB_SIZE);


        // A bind group contains one or multiple bindings
        WGPUBindGroupDescriptor bindGroupDesc = WGPUBindGroupDescriptor.createDirect();
        bindGroupDesc.setNextInChain();
        bindGroupDesc.setLayout(frameBindGroupLayout);
        // There must be as many bindings as declared in the layout!
        bindGroupDesc.setEntryCount(1);
        bindGroupDesc.setEntries(uniformBinding);
        return wgpu.DeviceCreateBindGroup(device, bindGroupDesc);
    }



     // shadow bind group
    private Pointer makeShadowBindGroup(Pointer shadowBindGroupLayout) {
        if(environment.shadowMap == null)
            throw new RuntimeException("Shadow Bind Group needs shadow map in environment.");

        WGPUBindGroupEntry samplerBinding = null;

        // Create a sampler
        WGPUSamplerDescriptor samplerDesc = WGPUSamplerDescriptor.createDirect();
        samplerDesc.setAddressModeU(WGPUAddressMode.ClampToEdge);
        samplerDesc.setAddressModeV(WGPUAddressMode.ClampToEdge);
        samplerDesc.setAddressModeW(WGPUAddressMode.ClampToEdge);
        samplerDesc.setMagFilter(WGPUFilterMode.Linear);
        samplerDesc.setMinFilter(WGPUFilterMode.Linear);
        samplerDesc.setMipmapFilter(WGPUMipmapFilterMode.Linear);

        samplerDesc.setLodMinClamp(0);
        samplerDesc.setLodMaxClamp(1);
        samplerDesc.setCompare(WGPUCompareFunction.Less);
        samplerDesc.setMaxAnisotropy(1);
        Pointer sampler = LibGPU.wgpu.DeviceCreateSampler(LibGPU.device, samplerDesc);

        samplerBinding = WGPUBindGroupEntry.createDirect();      // causes GC
        samplerBinding.setNextInChain();
        samplerBinding.setBinding(1);  // binding index
        samplerBinding.setSampler(sampler);


        // A bind group contains one or multiple bindings
        WGPUBindGroupDescriptor bindGroupDesc = WGPUBindGroupDescriptor.createDirect();
        bindGroupDesc.setNextInChain();
        bindGroupDesc.setLayout(shadowBindGroupLayout);

        // There must be as many bindings as declared in the layout!
        bindGroupDesc.setEntryCount(2);
        bindGroupDesc.setEntries( environment.shadowMap.getBinding(0), samplerBinding);

        return wgpu.DeviceCreateBindGroup(device, bindGroupDesc);
    }



    private Pointer makePipelineLayout(Pointer frameBindGroupLayout, Pointer materialBindGroupLayout, Pointer instancingBindGroupLayout, Pointer shadowBindGroupLayout) {

        long[] layouts = new long[4];
        int groups = 0;
        layouts[groups++] = frameBindGroupLayout.address();
        layouts[groups++] = materialBindGroupLayout.address();
        layouts[groups++] = instancingBindGroupLayout.address();
        if(environment != null && environment.renderShadows)
            layouts[groups++] = shadowBindGroupLayout.address();
        Pointer layoutPtr = WgpuJava.createLongArrayPointer(layouts);

        // Create the pipeline layout to define the bind groups needed : 3 or 4 bind groups
        WGPUPipelineLayoutDescriptor layoutDesc = WGPUPipelineLayoutDescriptor.createDirect();
        layoutDesc.setNextInChain();
        layoutDesc.setLabel("Pipeline Layout");
        layoutDesc.setBindGroupLayoutCount(groups);
        layoutDesc.setBindGroupLayouts(layoutPtr);
        return LibGPU.wgpu.DeviceCreatePipelineLayout(LibGPU.device, layoutDesc);
    }


    private void writeFrameUniforms( UniformBuffer uniformBuffer, Camera camera, Environment environment ){
        uniformBuffer.beginFill();
        uniformBuffer.append(camera.projection);
        uniformBuffer.append(camera.view);
        uniformBuffer.append(camera.combined);
        uniformBuffer.append(camera.position);
        uniformBuffer.append(environment == null ? 0f : environment.ambientLightLevel);
        uniformBuffer.pad(3*4);

        int numDirLights = 0;
        if(environment != null){
            DirectionalLight dirLight;
            for(Light light : environment.lights){
                if(light instanceof DirectionalLight && numDirLights < MAX_DIR_LIGHTS-1) {
                    numDirLights++;
                    dirLight = (DirectionalLight) light;
                    uniformBuffer.append(dirLight.color);
                    uniformBuffer.append(dirLight.direction);
                    uniformBuffer.append(dirLight.intensity);
                    uniformBuffer.pad(3*4); // padding
                }
            }
            // fixed length array, fill with placeholders up to MAX_DIR_LIGHTS
            for(int i = numDirLights; i < MAX_DIR_LIGHTS; i++) {
                dirLight = defaultDirectionalLight; // will be ignored anyway
                uniformBuffer.append(dirLight.color);
                uniformBuffer.append(dirLight.direction);
                uniformBuffer.append(dirLight.intensity);
                uniformBuffer.pad(3*4); // padding
            }
        }
        uniformBuffer.append(numDirLights);
        uniformBuffer.pad(3*4); // padding


        int numPointLights = 0;
        if(environment != null){
            PointLight pointLight;
            for(Light light : environment.lights){
                if(light instanceof PointLight && numPointLights < MAX_POINT_LIGHTS-1) {
                    numPointLights++;
                    pointLight = (PointLight) light;
                    uniformBuffer.append(pointLight.color);
                    uniformBuffer.append(pointLight.position);
                    uniformBuffer.append(pointLight.intensity);
                    // need padding?
                    uniformBuffer.pad(3*4); // padding
                }
            }
            // fixed length array, fill with placeholders up to MAX
            pointLight = new PointLight(Color.BLACK, new Vector3(), 0);// will be ignored anyway
            for(int i = numPointLights; i < MAX_POINT_LIGHTS; i++) {
                uniformBuffer.append(pointLight.color);
                uniformBuffer.append(pointLight.position);
                uniformBuffer.append(pointLight.intensity);
                uniformBuffer.pad(3*4); // padding
            }
        }
        uniformBuffer.append(numPointLights);
        uniformBuffer.pad(3*4); // padding


        if(environment != null && environment.shadowCamera != null) {
            uniformBuffer.append(environment.shadowCamera.combined);
            uniformBuffer.append(environment.shadowCamera.position);
        }

        uniformBuffer.endFill();   // write to GPU buffer
    }

    // add an instance to the instance buffer
    private void addInstance(int instanceIndex, Matrix4 modelTransform){
        if(instanceIndex >= MAX_INSTANCES)
            throw new RuntimeException("Too many instances: "+instanceIndex);

        instanceBuffer.beginFill();
        instanceBuffer.append(modelTransform);
        instanceBuffer.endFill(instanceIndex*16*Float.BYTES);   // write to GPU buffer at offset for this instance
    }



    private Pointer createInstancingBindGroupLayout(){

        // Define binding layout
        WGPUBindGroupLayoutEntry instancingBindGroupLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(instancingBindGroupLayout);
        instancingBindGroupLayout.setBinding(0);
        instancingBindGroupLayout.setVisibility(WGPUShaderStage.Vertex );
        instancingBindGroupLayout.getBuffer().setType(WGPUBufferBindingType.ReadOnlyStorage);       // must be read-only
        instancingBindGroupLayout.getBuffer().setMinBindingSize(16*Float.BYTES);
        instancingBindGroupLayout.getBuffer().setHasDynamicOffset(0L);

        // Create a bind group layout
        WGPUBindGroupLayoutDescriptor bindGroupLayoutDesc = WGPUBindGroupLayoutDescriptor.createDirect();
        bindGroupLayoutDesc.setNextInChain();
        bindGroupLayoutDesc.setLabel("ModelBatch Binding Group Layout (Instance)");
        bindGroupLayoutDesc.setEntryCount(1);

        bindGroupLayoutDesc.setEntries(instancingBindGroupLayout);
        return wgpu.DeviceCreateBindGroupLayout(device, bindGroupLayoutDesc);
    }

    private Pointer createInstancingBindGroup(Pointer instanceBindGroupLayout, Pointer instanceBuffer, int bufferSize) {
        // Create a binding
        WGPUBindGroupEntry binding = WGPUBindGroupEntry.createDirect();
        binding.setNextInChain();
        binding.setBinding(0);  // binding index
        binding.setBuffer(instanceBuffer);
        binding.setOffset(0);
        binding.setSize(bufferSize);

        // A bind group contains one or multiple bindings
        WGPUBindGroupDescriptor bindGroupDesc = WGPUBindGroupDescriptor.createDirect();
        bindGroupDesc.setNextInChain();
        bindGroupDesc.setLayout(instanceBindGroupLayout);
        // There must be as many bindings as declared in the layout!
        bindGroupDesc.setEntryCount(1);
        bindGroupDesc.setEntries(binding);
        return wgpu.DeviceCreateBindGroup(device, bindGroupDesc);
    }


    private void setDefault(WGPUBindGroupLayoutEntry bindingLayout) {

        bindingLayout.getBuffer().setNextInChain();
        bindingLayout.getBuffer().setType(WGPUBufferBindingType.Undefined);
        bindingLayout.getBuffer().setHasDynamicOffset(0L);

        bindingLayout.getSampler().setNextInChain();
        bindingLayout.getSampler().setType(WGPUSamplerBindingType.Undefined);

        bindingLayout.getStorageTexture().setNextInChain();
        bindingLayout.getStorageTexture().setAccess(WGPUStorageTextureAccess.Undefined);
        bindingLayout.getStorageTexture().setFormat(WGPUTextureFormat.Undefined);
        bindingLayout.getStorageTexture().setViewDimension(WGPUTextureViewDimension.Undefined);

        bindingLayout.getTexture().setNextInChain();
        bindingLayout.getTexture().setMultisampled(0L);
        bindingLayout.getTexture().setSampleType(WGPUTextureSampleType.Undefined);
        bindingLayout.getTexture().setViewDimension(WGPUTextureViewDimension.Undefined);
    }

}
