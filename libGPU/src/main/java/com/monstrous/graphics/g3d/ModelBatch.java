package com.monstrous.graphics.g3d;

import com.monstrous.LibGPU;
import com.monstrous.graphics.*;
import com.monstrous.graphics.lights.DirectionalLight;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.graphics.lights.Light;
import com.monstrous.graphics.lights.PointLight;
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

    private final int FRAME_UB_SIZE = 1024; //384; //(3*16+8+1+MAX_DIR_LIGHTS*8) * Float.BYTES;
    private final int MATERIAL_UB_SIZE = 8 * Float.BYTES;
    private final int MAX_UB_SIZE = FRAME_UB_SIZE;  // max of the above

    private final int MAX_MATERIALS = 256;   // limits nr of materials!

    private final WGPU wgpu;
    private final Pointer device;
    private final int uniformAlignment;

    private ShaderProgram shaderStd;
    private ShaderProgram shaderNormalMap;
    private Pointer renderPass;

    private Pointer uniformData;            // scratch buffer in native memory
    private Pointer frameUniformBuffer;
    private Pointer materialUniformBuffer;
    private int materialUniformIndex;

    private Pointer frameBindGroupLayout;
    private Pointer materialBindGroupLayout;
    private Pointer instancingBindGroupLayout;
    private Pointer pipelineLayout;
    private Pointer frameBindGroup;
    private Pointer materialBindGroup;
    private Pointer instancingBindGroup;
    private Material prevMaterial;

    private final Pipelines pipelines;
    private Pipeline prevPipeline;
    private PipelineSpecification pipelineSpec;
    private final List<Renderable> renderables;
    private final RenderablePool pool;
    public int numPipelineSwitches;
    public Environment environment;

    private DirectionalLight defaultDirectionalLight;
    private Pointer instanceBuffer;
    private Pointer instanceData;




    public ModelBatch (){
        wgpu = LibGPU.wgpu;
        device = LibGPU.device;
        uniformAlignment = (int)LibGPU.supportedLimits.getLimits().getMinUniformBufferOffsetAlignment();


        pipelines = new Pipelines();
        renderables = new ArrayList<>();
        pool = new RenderablePool(1000);
        pipelineSpec = new PipelineSpecification();

        defaultDirectionalLight = new DirectionalLight(new Color(1,0,0,1), new Vector3(0, -1, 0));

        frameUniformBuffer = createUniformBuffer( FRAME_UB_SIZE, 1);
        frameBindGroupLayout = createFrameBindGroupLayout();
        materialBindGroupLayout = createMaterialBindGroupLayout();
        instancingBindGroupLayout = createInstancingBindGroupLayout();

        pipelineLayout = makePipelineLayout(frameBindGroupLayout, materialBindGroupLayout, instancingBindGroupLayout);

        materialUniformBuffer = createUniformBuffer( MATERIAL_UB_SIZE, MAX_MATERIALS);

        int instanceSize = 16*Float.BYTES;      // data size per instance
        instanceBuffer = createInstancingBuffer(instanceSize, MAX_INSTANCES);

        float[] uniforms = new float[MAX_UB_SIZE/Float.BYTES];
        uniformData = WgpuJava.createFloatArrayPointer(uniforms);       // native memory buffer for one instance to aid write buffer

        float[] floatData = new float[16];
        instanceData = WgpuJava.createFloatArrayPointer(floatData);       // native memory buffer for one instance to aid write buffer

        loadShaders();
    }

    // to allow hot-loading of shaders
    public void loadShaders(){
        shaderStd = new ShaderProgram("shaders/modelbatchPBRUber.wgsl","");      // todo get from library storage
        shaderNormalMap = new ShaderProgram("shaders/modelbatchPBRUber.wgsl","#define NORMAL_MAP");      // todo get from library storage

    }



    public void begin(Camera camera){
        begin(camera, null);
    }

    public void begin(Camera camera, Environment environment){
        this.environment = environment;
        this.renderPass = LibGPU.renderPass;

        materialUniformIndex = 0;       // reset offset into uniform buffer
        prevMaterial = null;
        prevPipeline = null;
        numPipelineSwitches = 0;

        writeFrameUniforms(frameUniformBuffer, camera, environment);
        frameBindGroup = makeFrameBindGroup(frameBindGroupLayout, frameUniformBuffer);
        wgpu.RenderPassEncoderSetBindGroup(renderPass, 0, frameBindGroup, 0, null);

        instancingBindGroup = createInstancingBindGroup(instancingBindGroupLayout, instanceBuffer, 16*Float.BYTES*MAX_INSTANCES);
        wgpu.RenderPassEncoderSetBindGroup(renderPass, 2, instancingBindGroup, 0, null);

        materialBindGroup = null;
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
        if(materialBindGroup != null)
            wgpu.BindGroupRelease(materialBindGroup);
        //System.out.println("materials: "+materialUniformIndex+"\t\tpipe switches: "+numPipelineSwitches);
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

        // make a new bind group every time we change texture
        if(material != prevMaterial) {
            prevMaterial = material;
            writeMaterialUniforms(materialUniformBuffer, materialUniformIndex, material);
            materialBindGroupLayout = createMaterialBindGroupLayout();
            if(materialBindGroup != null)
                wgpu.BindGroupRelease(materialBindGroup);
            materialBindGroup = makeMaterialBindGroup(material, materialBindGroupLayout, materialUniformBuffer, meshPart.mesh.vertexAttributes.hasNormalMap);   // bind group for textures and uniforms

            // set dynamic offset into uniform buffer
            int[] offset = new int[1];
            int uniformStride = ceilToNextMultiple(MATERIAL_UB_SIZE, uniformAlignment);
            offset[0] = materialUniformIndex*uniformStride;
            Pointer offsetPtr = WgpuJava.createIntegerArrayPointer(offset);
            wgpu.RenderPassEncoderSetBindGroup(renderPass, 1, materialBindGroup, 1, offsetPtr);
            materialUniformIndex++;
        }
    }

    private void emitMeshPart(MeshPart meshPart, int instanceCount, int renderablesCount) {
        if(meshPart == null)
            return;
        Pointer vertexBuffer = meshPart.mesh.getVertexBuffer();
        wgpu.RenderPassEncoderSetVertexBuffer(renderPass, 0, vertexBuffer, 0, wgpu.BufferGetSize(vertexBuffer));

        setPipeline(meshPart.mesh.vertexAttributes);


        if (meshPart.mesh.getIndexCount() > 0) { // indexed mesh?
            Pointer indexBuffer = meshPart.mesh.getIndexBuffer();
            wgpu.RenderPassEncoderSetIndexBuffer(renderPass, indexBuffer, meshPart.mesh.indexFormat, 0, wgpu.BufferGetSize(indexBuffer));
            wgpu.RenderPassEncoderDrawIndexed(renderPass, meshPart.size, instanceCount, meshPart.offset, 0, renderablesCount-instanceCount);
        } //meshPart.size
        else
            wgpu.RenderPassEncoderDraw(renderPass, meshPart.size, instanceCount, meshPart.offset, renderablesCount-instanceCount);

    }

    // create or reuse pipeline on demand when we know the model
    private void setPipeline(VertexAttributes vertexAttributes) {

        pipelineSpec.vertexAttributes = vertexAttributes;
        if(vertexAttributes.hasNormalMap)
           pipelineSpec.shader = shaderNormalMap;
        else
           pipelineSpec.shader = shaderStd;
        pipelineSpec.enableDepth();

        Pipeline pipeline = pipelines.getPipeline(pipelineLayout, pipelineSpec);
        if (pipeline != prevPipeline) { // avoid unneeded switches
            wgpu.RenderPassEncoderSetPipeline(renderPass, pipeline.getPipeline());
            prevPipeline = pipeline;
            numPipelineSwitches++;
        }
    }


    @Override
    public void dispose() {
        shaderStd.dispose();
        shaderNormalMap.dispose();
        pipelines.dispose();
        wgpu.BindGroupLayoutRelease(frameBindGroupLayout);
        wgpu.BindGroupLayoutRelease(materialBindGroupLayout);
        wgpu.BindGroupLayoutRelease(instancingBindGroupLayout);
        wgpu.BufferRelease(frameUniformBuffer);
        wgpu.BufferRelease(materialUniformBuffer);
        wgpu.BufferRelease(instanceBuffer);
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

    private Pointer createMaterialBindGroupLayout(){
        int location = 0;

        // Define binding layout
        WGPUBindGroupLayoutEntry uniformBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(uniformBindingLayout);
        uniformBindingLayout.setBinding(location++);
        uniformBindingLayout.setVisibility(WGPUShaderStage.Vertex | WGPUShaderStage.Fragment);
        uniformBindingLayout.getBuffer().setType(WGPUBufferBindingType.Uniform);
        uniformBindingLayout.getBuffer().setMinBindingSize(MATERIAL_UB_SIZE);
        uniformBindingLayout.getBuffer().setHasDynamicOffset(1L);

        WGPUBindGroupLayoutEntry texBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(texBindingLayout);
        texBindingLayout.setBinding(location++);
        texBindingLayout.setVisibility(WGPUShaderStage.Fragment);
        texBindingLayout.getTexture().setSampleType(WGPUTextureSampleType.Float);
        texBindingLayout.getTexture().setViewDimension(WGPUTextureViewDimension._2D);

        WGPUBindGroupLayoutEntry samplerBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(samplerBindingLayout);
        samplerBindingLayout.setBinding(location++);
        samplerBindingLayout.setVisibility(WGPUShaderStage.Fragment);
        samplerBindingLayout.getSampler().setType(WGPUSamplerBindingType.Filtering);

        // emissive texture binding is included even if it is not used
        WGPUBindGroupLayoutEntry emissiveTexBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(emissiveTexBindingLayout);
        emissiveTexBindingLayout.setBinding(location++);
        emissiveTexBindingLayout.setVisibility(WGPUShaderStage.Fragment);
        emissiveTexBindingLayout.getTexture().setSampleType(WGPUTextureSampleType.Float);
        emissiveTexBindingLayout.getTexture().setViewDimension(WGPUTextureViewDimension._2D);

        // normal texture binding is included even if it is not used
        WGPUBindGroupLayoutEntry normalTexBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(normalTexBindingLayout);
        normalTexBindingLayout.setBinding(location++);
        normalTexBindingLayout.setVisibility(WGPUShaderStage.Fragment);
        normalTexBindingLayout.getTexture().setSampleType(WGPUTextureSampleType.Float);
        normalTexBindingLayout.getTexture().setViewDimension(WGPUTextureViewDimension._2D);

        // Create a bind group layout
        WGPUBindGroupLayoutDescriptor bindGroupLayoutDesc = WGPUBindGroupLayoutDescriptor.createDirect();
        bindGroupLayoutDesc.setNextInChain();
        bindGroupLayoutDesc.setLabel("ModelBatch Bind Group Layout (Material)");
        bindGroupLayoutDesc.setEntryCount(location);

        bindGroupLayoutDesc.setEntries(uniformBindingLayout, texBindingLayout, samplerBindingLayout, emissiveTexBindingLayout, normalTexBindingLayout );
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


    // per material bind group
    private Pointer makeMaterialBindGroup(Material material, Pointer bindGroupLayout, Pointer materialUniformBuffer, boolean hasNormalMap) {
        // Create a binding
        WGPUBindGroupEntry uniformBinding = WGPUBindGroupEntry.createDirect();
        uniformBinding.setNextInChain();
        uniformBinding.setBinding(0);  // binding index
        uniformBinding.setBuffer(materialUniformBuffer);
        uniformBinding.setOffset(0);
        uniformBinding.setSize(MATERIAL_UB_SIZE);

        Texture diffuse = material.diffuseTexture;

        // A bind group contains one or multiple bindings
        WGPUBindGroupDescriptor bindGroupDesc = WGPUBindGroupDescriptor.createDirect();
        bindGroupDesc.setNextInChain();
        bindGroupDesc.setLayout(bindGroupLayout);
        // There must be as many bindings as declared in the layout!
        bindGroupDesc.setEntryCount(5);
        if(hasNormalMap && material.normalTexture != null) {
            bindGroupDesc.setEntries(uniformBinding, diffuse.getBinding(1), diffuse.getSamplerBinding(2), material.emissiveTexture.getBinding(3), material.normalTexture.getBinding(4) );
        }
        else {
            // use diffuse map as fake (ignored) normal map, so that we maintain the same layout
            bindGroupDesc.setEntries(uniformBinding, diffuse.getBinding(1),  diffuse.getSamplerBinding(2), material.emissiveTexture.getBinding(3), diffuse.getBinding(4) );
        }
        return wgpu.DeviceCreateBindGroup(device, bindGroupDesc);
    }



    private Pointer makePipelineLayout(Pointer frameBindGroupLayout, Pointer materialBindGroupLayout, Pointer instancingBindGroupLayout) {
        long[] layouts = new long[3];
        layouts[0] = frameBindGroupLayout.address();
        layouts[1] = materialBindGroupLayout.address();
        layouts[2] = instancingBindGroupLayout.address();
        Pointer layoutPtr = WgpuJava.createLongArrayPointer(layouts);

        // Create the pipeline layout to define the bind groups needed : 3 bind group
        WGPUPipelineLayoutDescriptor layoutDesc = WGPUPipelineLayoutDescriptor.createDirect();
        layoutDesc.setNextInChain();
        layoutDesc.setLabel("Pipeline Layout");
        layoutDesc.setBindGroupLayoutCount(3);
        layoutDesc.setBindGroupLayouts(layoutPtr);
        return LibGPU.wgpu.DeviceCreatePipelineLayout(LibGPU.device, layoutDesc);
    }

    private int ceilToNextMultiple(int value, int step){
        int d = value / step + (value % step == 0 ? 0 : 1);
        return step * d;
    }





    private int setUniformInteger(Pointer data, int offset, int value ){
        data.putInt(offset, value);
        return Integer.BYTES;
    }
    private int setUniformFloat(Pointer data, int offset, float value ){
        data.putFloat(offset, value);
        return Float.BYTES;
    }

    private int setUniformColor(Pointer data, int offset, float r, float g, float b, float a ){
        data.putFloat(offset+0*Float.BYTES, r);
        data.putFloat(offset+1*Float.BYTES, g);
        data.putFloat(offset+2*Float.BYTES, b);
        data.putFloat(offset+3*Float.BYTES, a);
        return 4*Float.BYTES;
    }

    private int setUniformColor(Pointer data, int offset, Color color ){
        data.putFloat(offset+0*Float.BYTES, color.r);
        data.putFloat(offset+1*Float.BYTES, color.g);
        data.putFloat(offset+2*Float.BYTES, color.b);
        data.putFloat(offset+3*Float.BYTES, color.a);
        return 4*Float.BYTES;
    }

    private int setUniformVec3(Pointer data, int offset, Vector3 vec ){
        data.putFloat(offset+0*Float.BYTES, vec.x);
        data.putFloat(offset+1*Float.BYTES, vec.y);
        data.putFloat(offset+2*Float.BYTES, vec.z);
        return 4*Float.BYTES;           // with padding!
    }

    private int setUniformMatrix(Pointer data, int offset, Matrix4 mat ){
//        for(int i = 0; i < 16; i++){
//            data.putFloat(offset+i*Float.BYTES, mat.val[i]);
//
//        }
        data.put(offset, mat.val, 0, 16);
        return 16*Float.BYTES;
    }



    private void writeFrameUniforms( Pointer uniformBuffer, Camera camera, Environment environment ){
        int offset = 0;
        offset += setUniformMatrix(uniformData, offset, camera.projectionMatrix);
        offset += setUniformMatrix(uniformData, offset, camera.viewMatrix);
        offset += setUniformMatrix(uniformData, offset, camera.combinedMatrix);
        offset += setUniformVec3(uniformData, offset, camera.position);

        offset += setUniformFloat(uniformData, offset, environment == null ? 0 : environment.ambientLightLevel);
        offset += 3*4; // padding (important)

        int numDirLights = 0;
        if(environment != null){
            DirectionalLight dirLight;
            for(Light light : environment.lights){
                if(light instanceof DirectionalLight && numDirLights < MAX_DIR_LIGHTS-1) {
                    numDirLights++;
                    dirLight = (DirectionalLight) light;
                    offset += setUniformColor(uniformData, offset, dirLight.color);
                    offset += setUniformVec3(uniformData, offset, dirLight.direction);
                }
            }
            // fixed length array, fill with placeholders up to MAX_DIR_LIGHTS
            for(int i = numDirLights; i < MAX_DIR_LIGHTS; i++) {
                dirLight = defaultDirectionalLight; // will be ignored anyway
                offset += setUniformColor(uniformData, offset, dirLight.color);
                offset += setUniformVec3(uniformData, offset, dirLight.direction);
            }
        }
        offset += setUniformInteger(uniformData, offset, numDirLights);
        offset += 3*4; // padding (important)

        int numPointLights = 0;
        if(environment != null){
            PointLight pointLight;
            for(Light light : environment.lights){
                if(light instanceof PointLight && numPointLights < MAX_POINT_LIGHTS-1) {
                    numPointLights++;
                    pointLight = (PointLight) light;
                    offset += setUniformColor(uniformData, offset, pointLight.color);
                    offset += setUniformVec3(uniformData, offset, pointLight.position);
                    offset += setUniformFloat(uniformData, offset, pointLight.intensity);
                    // need padding?
                    offset += 3*4;  // padding
                }
            }
            // fixed length array, fill with placeholders up to MAX
            pointLight = new PointLight(Color.BLACK, new Vector3(), 0);// will be ignored anyway
            for(int i = numPointLights; i < MAX_POINT_LIGHTS; i++) {
                offset += setUniformColor(uniformData, offset, pointLight.color);
                offset += setUniformVec3(uniformData, offset, pointLight.position);
                offset += setUniformFloat(uniformData, offset, pointLight.intensity);
                offset += 3*4;  // padding
            }
        }
        offset += setUniformInteger(uniformData, offset, numPointLights);
        offset += 3*4; // padding (important)



        // BEWARE of padding rules

        wgpu.QueueWriteBuffer(LibGPU.queue, uniformBuffer, 0, uniformData, FRAME_UB_SIZE);
    }

    private void writeMaterialUniforms( Pointer uniformBuffer, int uniformIndex, Material material){
        if(uniformIndex >= MAX_MATERIALS)
            throw new RuntimeException("ModelBatch: Too many models");

        int offset = 0;
        offset += setUniformFloat(uniformData, offset, material.metallicFactor);
        offset += setUniformFloat(uniformData, offset, material.roughnessFactor);
        offset += 2*4; // padding (important)
        offset += setUniformColor(uniformData, offset, material.baseColor);


        int uniformStride = ceilToNextMultiple(MATERIAL_UB_SIZE, uniformAlignment);
        wgpu.QueueWriteBuffer(LibGPU.queue, uniformBuffer, uniformIndex*uniformStride, uniformData, offset);
    }


    private Pointer createUniformBuffer(int bufferSize, int maxSlices) {
        int uniformStride = ceilToNextMultiple(bufferSize, uniformAlignment);

        // Create uniform buffer
        WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
        bufferDesc.setLabel("Uniform object buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.Uniform );
        bufferDesc.setSize((long) uniformStride * maxSlices);
        bufferDesc.setMappedAtCreation(0L);
        return wgpu.DeviceCreateBuffer(device, bufferDesc);
    }

    private Pointer createInstancingBuffer(int instanceSize, int maxRenderables) {

        // Create uniform buffer
        WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
        bufferDesc.setLabel("Instancing storage buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.Storage );
        bufferDesc.setSize((long) instanceSize * maxRenderables);
        bufferDesc.setMappedAtCreation(0L);
        return wgpu.DeviceCreateBuffer(device, bufferDesc);
    }


    // add an instance to the instance buffer
    private void addInstance(int instanceIndex, Matrix4 modelTransform){
        if(instanceIndex >= MAX_INSTANCES)
            throw new RuntimeException("Too many instances: "+instanceIndex);

        int instanceSize = 16*Float.BYTES;      // data size per instance
        instanceData.put(0, modelTransform.val, 0, 16);
        wgpu.QueueWriteBuffer(LibGPU.queue, instanceBuffer, instanceIndex*instanceSize, instanceData, instanceSize);
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
        bindingLayout.getBuffer().setHasDynamicOffset(1L);

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
