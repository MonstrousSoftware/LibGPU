package com.monstrous.graphics;

import com.monstrous.LibGPU;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;
import com.monstrous.utils.Disposable;
import com.monstrous.wgpu.*;
import com.monstrous.wgpuUtils.WgpuJava;
import jnr.ffi.Pointer;

public class ModelBatch implements Disposable {

    private final int maxDynamicUniformBuffers = 64;   // limits nr of renderables!

    private final WGPU wgpu;
    private final Pointer device;

    private Camera camera;

    private ShaderProgram shader;
    private Pointer renderPass;

    private int uniformBufferSize;  // in bytes, excluding stride
    private Pointer uniformData;
    private Pointer uniformObjectBuffer;
    private int uniformIndex;
    private int uniformStride;

    private WGPUVertexBufferLayout vertexBufferLayout;  // assumes all meshes will confirm to this, only used by pipeline
    private Pointer bindGroupLayout;
    private Pointer bindGroup;
    private Material prevMaterial;
    private boolean hasNormalMap;

    private final Pipelines pipelines;
    private Pipeline prevPipeline;

    public ModelBatch () {
        wgpu = LibGPU.wgpu;
        device = LibGPU.device;
        pipelines = new Pipelines();


        makeUniformBuffer();
    }

    // create or reuse pipeline on demand when we know the model
    private void setPipeline(VertexAttributes vertexAttributes) {

        bindGroupLayout = createBindGroupLayout(vertexAttributes.hasNormalMap);
        hasNormalMap = vertexAttributes.hasNormalMap;

        if (hasNormalMap)
            shader = new ShaderProgram("shaders/modelbatchN.wgsl");      // todo get from library storage
        else
            shader = new ShaderProgram("shaders/modelbatch.wgsl");      // todo get from library storage

        Pipeline pipeline = pipelines.getPipeline(vertexAttributes, bindGroupLayout, shader);
        if (pipeline != prevPipeline) { // avoid unneeded switches
            wgpu.RenderPassEncoderSetPipeline(renderPass, pipeline.getPipeline());
            prevPipeline = pipeline;
        }
    }


    public void begin(Camera camera){
        this.camera = camera;
        this.renderPass = LibGPU.renderPass;

        uniformIndex = 0;
        prevMaterial = null;
        prevPipeline = null;
    }

    public void render(ModelInstance instance){
        render(instance.model.rootNode);
    }

    public void render(ModelInstance instance, Material material){
        render(instance.model.rootNode.nodePart.meshPart, material, instance.modelTransform);
    }

    public void render(Node node){
        if(node.nodePart != null)
            render(node.nodePart, node.worldTransform);
        for(Node child : node.children)
            render(child);
    }

    public void render(NodePart nodePart, Matrix4 transform){
        render(nodePart.meshPart, nodePart.material, transform);
    }

    public void render(Renderable renderable) {
        render(renderable.meshPart, renderable.material, renderable.modelTransform);
    }


    public void render(MeshPart meshPart, Material material, Matrix4 modelMatrix){

        writeUniforms(camera, modelMatrix);  // renderable uniforms

        if(uniformIndex == maxDynamicUniformBuffers)
            throw new RuntimeException("Too many dynamic uniform buffers!");

        setPipeline(meshPart.mesh.vertexAttributes);

        Pointer vertexBuffer = meshPart.mesh.getVertexBuffer();
        wgpu.RenderPassEncoderSetVertexBuffer(renderPass, 0, vertexBuffer, 0, wgpu.BufferGetSize(vertexBuffer));

        // make a new bind group every time we change texture
        if(material != prevMaterial)
            bindGroup = makeBindGroup(material, bindGroupLayout);   // bind group for textures and uniforms

        // set dynamic offset into uniform buffer
        int[] offset = new int[1];
        offset[0] = uniformIndex*uniformStride;
        Pointer offsetPtr = WgpuJava.createIntegerArrayPointer(offset);
        wgpu.RenderPassEncoderSetBindGroup(renderPass, 0, bindGroup, 1, offsetPtr);

        if(meshPart.mesh.getIndexCount() > 0) { // indexed mesh?
            Pointer indexBuffer = meshPart.mesh.getIndexBuffer();
            wgpu.RenderPassEncoderSetIndexBuffer(renderPass, indexBuffer, meshPart.mesh.indexFormat, 0, wgpu.BufferGetSize(indexBuffer));
            wgpu.RenderPassEncoderDrawIndexed(renderPass, meshPart.size, 1, meshPart.offset, 0, 0);
        }
        else
            wgpu.RenderPassEncoderDraw(renderPass, meshPart.size, 1, meshPart.offset, 0);

        uniformIndex++;
    }

    public void end(){
        wgpu.BindGroupRelease(bindGroup);
    }


    @Override
    public void dispose() {
        shader.dispose();
        pipelines.dispose();
        wgpu.BindGroupLayoutRelease(bindGroupLayout);
        wgpu.BufferRelease(uniformObjectBuffer);
    }



    // Bind Group Layout:
    //  uniforms
    //  texture
    //  normal map
    //  sampler
    private Pointer createBindGroupLayout(boolean hasNormalMap){
        int location = 0;

        // Define binding layout
        WGPUBindGroupLayoutEntry bindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(bindingLayout);
        bindingLayout.setBinding(location++);
        bindingLayout.setVisibility(WGPUShaderStage.Vertex | WGPUShaderStage.Fragment);
        bindingLayout.getBuffer().setType(WGPUBufferBindingType.Uniform);
        bindingLayout.getBuffer().setMinBindingSize(uniformBufferSize);

        WGPUBindGroupLayoutEntry texBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(texBindingLayout);
        texBindingLayout.setBinding(location++);
        texBindingLayout.setVisibility(WGPUShaderStage.Fragment);
        texBindingLayout.getTexture().setSampleType(WGPUTextureSampleType.Float);
        texBindingLayout.getTexture().setViewDimension(WGPUTextureViewDimension._2D);

        WGPUBindGroupLayoutEntry normalTexBindingLayout = null;
        if(hasNormalMap) {
            normalTexBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
            setDefault(normalTexBindingLayout);
            normalTexBindingLayout.setBinding(location++);
            normalTexBindingLayout.setVisibility(WGPUShaderStage.Fragment);
            normalTexBindingLayout.getTexture().setSampleType(WGPUTextureSampleType.Float);
            normalTexBindingLayout.getTexture().setViewDimension(WGPUTextureViewDimension._2D);
        }


        WGPUBindGroupLayoutEntry samplerBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(samplerBindingLayout);
        samplerBindingLayout.setBinding(location++);
        samplerBindingLayout.setVisibility(WGPUShaderStage.Fragment);
        samplerBindingLayout.getSampler().setType(WGPUSamplerBindingType.Filtering);

        // Create a bind group layout
        WGPUBindGroupLayoutDescriptor bindGroupLayoutDesc = WGPUBindGroupLayoutDescriptor.createDirect();
        bindGroupLayoutDesc.setNextInChain();
        bindGroupLayoutDesc.setLabel("ModelBatch Bind Group Layout");
        bindGroupLayoutDesc.setEntryCount(location);

        if(hasNormalMap)
            bindGroupLayoutDesc.setEntries(bindingLayout, texBindingLayout, normalTexBindingLayout, samplerBindingLayout);
        else
            bindGroupLayoutDesc.setEntries(bindingLayout, texBindingLayout, samplerBindingLayout);
        return wgpu.DeviceCreateBindGroupLayout(device, bindGroupLayoutDesc);

    }

    // per renderabe bind group
    private Pointer makeBindGroup(Material material, Pointer bindGroupLayout) {
        // Create a binding
        WGPUBindGroupEntry binding = WGPUBindGroupEntry.createDirect();
        binding.setNextInChain();
        binding.setBinding(0);  // binding index
        binding.setBuffer(uniformObjectBuffer);
        binding.setOffset(0);
        binding.setSize(uniformBufferSize);

        Texture diffuse = material.diffuseTexture;

        // A bind group contains one or multiple bindings
        WGPUBindGroupDescriptor bindGroupDesc = WGPUBindGroupDescriptor.createDirect();
        bindGroupDesc.setNextInChain();
        bindGroupDesc.setLayout(bindGroupLayout);
        // There must be as many bindings as declared in the layout!
        if(hasNormalMap) {
            bindGroupDesc.setEntryCount(4);
            bindGroupDesc.setEntries(binding, diffuse.getBinding(1), material.normalTexture.getBinding(2),  diffuse.getSamplerBinding(3));
        }
        else {
            bindGroupDesc.setEntryCount(3);
            bindGroupDesc.setEntries(binding, diffuse.getBinding(1),  diffuse.getSamplerBinding(2));
        }
        return wgpu.DeviceCreateBindGroup(device, bindGroupDesc);
    }


    private int ceilToNextMultiple(int value, int step){
        int d = value / step + (value % step == 0 ? 0 : 1);
        return step * d;
    }

    private void makeUniformBuffer() {


        // P matrix: 16 float
        // M matrix: 16 float
        // V matrix: 16 float
        // color: 4 floats
        // camPos: 3 floats
        // 1 float padding
        uniformBufferSize = (3*16+4+4) * Float.BYTES;

        float[] uniforms = new float[uniformBufferSize];
        uniformData = WgpuJava.createFloatArrayPointer(uniforms);       // native memory buffer for one instance to aid write buffer


        int minAlign = (int)LibGPU.supportedLimits.getLimits().getMinUniformBufferOffsetAlignment();
        uniformStride = ceilToNextMultiple(uniformBufferSize, minAlign);

        System.out.println("min uniform alignment: "+minAlign);
        System.out.println("uniform stride: "+uniformStride);
        System.out.println("uniformBufferSize: "+uniformBufferSize);


        // Create uniform buffer
        WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
        bufferDesc.setLabel("Uniform object buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.Uniform );
        bufferDesc.setSize((long) uniformStride * maxDynamicUniformBuffers);
        bufferDesc.setMappedAtCreation(0L);
        uniformObjectBuffer = wgpu.DeviceCreateBuffer(device, bufferDesc);
    }

    private void setUniformColor(Pointer data, int offset, float r, float g, float b, float a ){
        data.putFloat(offset+0*Float.BYTES, r);
        data.putFloat(offset+1*Float.BYTES, g);
        data.putFloat(offset+2*Float.BYTES, b);
        data.putFloat(offset+3*Float.BYTES, a);
    }

    private void setUniformVec3(Pointer data, int offset, Vector3 vec ){
        data.putFloat(offset+0*Float.BYTES, vec.x);
        data.putFloat(offset+1*Float.BYTES, vec.y);
        data.putFloat(offset+2*Float.BYTES, vec.z);
    }

    private void setUniformMatrix(Pointer data, int offset, Matrix4 mat ){
        for(int i = 0; i < 16; i++){
            data.putFloat(offset+i*Float.BYTES, mat.val[i]);
        }
    }

    private void writeUniforms( Camera camera, Matrix4 modelMatrix){
        int offset = 0;
        setUniformMatrix(uniformData, offset, camera.projectionMatrix);
        offset += 16*Float.BYTES;
        setUniformMatrix(uniformData, offset, camera.viewMatrix);
        offset += 16*Float.BYTES;
        setUniformMatrix(uniformData, offset, modelMatrix);
        offset += 16*Float.BYTES;
        setUniformColor(uniformData, offset, 0.0f, 1.0f, 0.4f, 1.0f);
        offset += 4*Float.BYTES;
        setUniformVec3(uniformData, offset, camera.position);
        offset += 3*Float.BYTES;

        wgpu.QueueWriteBuffer(LibGPU.queue, uniformObjectBuffer, uniformIndex*uniformStride, uniformData, offset); //uniformBufferSize);
    }

    public WGPUVertexBufferLayout getVertexBufferLayout(){
        if(vertexBufferLayout == null) {
            VertexAttributes vertexAttributes = new VertexAttributes();
            vertexAttributes.add("position", WGPUVertexFormat.Float32x3, 0);
//            vertexAttributes.add("tangent", WGPUVertexFormat.Float32x3, 1);
//            vertexAttributes.add("bitangent", WGPUVertexFormat.Float32x3, 2);
            vertexAttributes.add("normal", WGPUVertexFormat.Float32x3, 1);
            vertexAttributes.add("color", WGPUVertexFormat.Float32x3, 2);
            vertexAttributes.add("uv", WGPUVertexFormat.Float32x2, 3);
            vertexAttributes.end();

            vertexBufferLayout = vertexAttributes.getVertexBufferLayout();
        }
        return vertexBufferLayout;
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
