package com.monstrous.graphics;

import com.monstrous.LibGPU;
import com.monstrous.math.Matrix4;
import com.monstrous.utils.Disposable;
import com.monstrous.wgpu.*;
import com.monstrous.wgpuUtils.WgpuJava;
import jnr.ffi.Pointer;

public class ModelBatch implements Disposable {

    private WGPU wgpu;
    private Pointer device;
    private Pointer queue;

    private Camera camera;
    private Mesh mesh;

    private ShaderProgram shader;
    private Pointer pipeline;
    private Pointer renderPass;

    private int uniformBufferSize;  // in bytes
    private Pointer uniformData;
    private Pointer uniformBuffer;

    private WGPUVertexBufferLayout vertexBufferLayout;  // assumes all meshes will confirm to this
    private Pointer pipelineLayout;
    private Pointer bindGroupLayout;

    public ModelBatch () {
        wgpu = LibGPU.wgpu;
        device = LibGPU.device;
        queue = LibGPU.queue;

        shader = new ShaderProgram("shader.wgsl");

        makeUniformBuffer();
        defineBindGroupLayout();
        initializePipeline();
    }


    public void begin(Camera camera){
        this.camera = camera;
        this.renderPass = LibGPU.renderPass;
        wgpu.RenderPassEncoderSetPipeline(renderPass, pipeline);
    }

    public void render(Mesh mesh, Texture texture, Matrix4 modelMatrix){      // todo
        this.mesh = mesh;

        writeUniforms(camera, modelMatrix);  // e.g. projection and view matrix   // todo split into per frame vs per object

        Pointer vertexBuffer = mesh.getVertexBuffer();
        Pointer indexBuffer = mesh.getIndexBuffer();
        int indexCount = mesh.getIndexCount();

        // Set vertex buffer while encoding the render pass
        wgpu.RenderPassEncoderSetVertexBuffer(renderPass, 0, vertexBuffer, 0, wgpu.BufferGetSize(vertexBuffer));
        wgpu.RenderPassEncoderSetIndexBuffer(renderPass, indexBuffer, WGPUIndexFormat.Uint32, 0, wgpu.BufferGetSize(indexBuffer));

        Pointer bg = initBindGroups(texture);
        wgpu.RenderPassEncoderSetBindGroup(renderPass, 0, bg, 0, null);
        wgpu.RenderPassEncoderDrawIndexed(renderPass, indexCount, 1, 0, 0, 0);
        wgpu.BindGroupRelease(bg);      // we can release straight away?
    }

    public void end(){


    }


    @Override
    public void dispose() {
        shader.dispose();

        wgpu.RenderPipelineRelease(pipeline);
        wgpu.PipelineLayoutRelease(pipelineLayout);
        wgpu.BindGroupLayoutRelease(bindGroupLayout);
        wgpu.BufferRelease(uniformBuffer);
    }


    private Pointer initBindGroups(Texture texture) {
        // Create a binding
        WGPUBindGroupEntry binding = WGPUBindGroupEntry.createDirect();
        binding.setNextInChain();
        binding.setBinding(0);  // binding index
        binding.setBuffer(uniformBuffer);
        binding.setOffset(0);
        binding.setSize(uniformBufferSize);

        // A bind group contains one or multiple bindings
        WGPUBindGroupDescriptor bindGroupDesc = WGPUBindGroupDescriptor.createDirect();
        bindGroupDesc.setNextInChain();
        bindGroupDesc.setLayout(bindGroupLayout);
        // There must be as many bindings as declared in the layout!
        bindGroupDesc.setEntryCount(3);
        bindGroupDesc.setEntries(binding, texture.getBinding(1), texture.getSamplerBinding(2));
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
        uniformBufferSize = (3*16+4) * Float.BYTES;
        float[] uniforms = new float[uniformBufferSize];
        uniformData = WgpuJava.createFloatArrayPointer(uniforms);       // native memory buffer

        int minAlign = (int)LibGPU.supportedLimits.getLimits().getMinUniformBufferOffsetAlignment();
        int uniformStride = ceilToNextMultiple(uniformBufferSize, minAlign);
        int uniformInstances = 1;   // how many sets of uniforms?

        System.out.println("min uniform alignment: "+minAlign);
        System.out.println("uniform stride: "+uniformStride);
        System.out.println("uniformBufferSize: "+uniformBufferSize);


        // Create uniform buffer
        WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
        bufferDesc.setLabel("Uniform buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.Uniform );
        bufferDesc.setSize((long) uniformStride * uniformInstances);
        bufferDesc.setMappedAtCreation(0L);
        uniformBuffer = wgpu.DeviceCreateBuffer(device, bufferDesc);

    }
    private void setUniformColor(Pointer data, int offset, float r, float g, float b, float a ){
        data.putFloat(offset+0*Float.BYTES, r);
        data.putFloat(offset+1*Float.BYTES, g);
        data.putFloat(offset+2*Float.BYTES, b);
        data.putFloat(offset+3*Float.BYTES, a);
    }
    private void setUniformMatrix(Pointer data, int offset, Matrix4 mat ){
        for(int i = 0; i < 16; i++){
            data.putFloat(offset+i*Float.BYTES, mat.val[i]);
        }
    }

//    private void updateMatrices(float currentTime){
//        camera.projectionMatrix.setToOrtho(-1.1f, 1.1f, -1.1f, 1.1f, -1, 1);
//
//        modelMatrix.setToXRotation((float) ( -0.5f*Math.PI ));  // tilt to face camera
//        camera.viewMatrix.idt();
//
//    }
//
//    private void updateMatrices2(float currentTime){
//
//        float aspectRatio = (float)LibGPU.graphics.getWidth()/(float)LibGPU.graphics.getHeight();
//        camera.projectionMatrix.setToPerspective(1.5f, 0.01f, 9.0f, aspectRatio);
//        //projectionMatrix.setToProjection(0.001f, 3.0f, 60f, 640f/480f);
//        //modelMatrix.setToYRotation(currentTime*0.2f).scale(0.5f);
//        modelMatrix.idt();//.setToXRotation((float) ( -0.5f*Math.PI ));
//
//        Matrix4 RT = new Matrix4().setToXRotation((float) ( -0.5f*Math.PI ));
//
//        //modelMatrix.idt().scale(0.5f);
//        camera.viewMatrix.idt();
//        Matrix4 R1 = new Matrix4().setToYRotation(currentTime*0.6f);
//        Matrix4 R2 = new Matrix4().setToXRotation((float) (-0.5* Math.PI / 4.0)); // tilt the view
//        Matrix4 S = new Matrix4().scale(1.6f);
//        Matrix4 T = new Matrix4().translate(0.8f, 0f, 0f);
//        Matrix4 TC = new Matrix4().translate(0.0f, -1f, 3f);
//
//
//        modelMatrix.idt().mul(R1).mul(T).mul(RT);
//
//        TC.mul(S);
//        R2.mul(TC); // tilt
//        camera.viewMatrix.set(R2);
//        //viewMatrix.translate(0,0.2f, 0);
//        //viewMatrix.setToZRotation((float) (Math.PI*0.5f));
//        //viewMatrix.translate(0, 0, (float)Math.cos(currentTime)*0.5f );
//    }

//    private void updateMatrices3(float currentTime){
//        Matrix4 RT = new Matrix4().setToXRotation((float) ( -0.5f*Math.PI ));
//        Matrix4 R1 = new Matrix4().setToYRotation(currentTime*0.6f);
//        Matrix4 T = new Matrix4().translate(0.8f, 0f, 0f);
//        modelMatrix.idt().mul(R1).mul(T).mul(RT);
//    }

    private void writeUniforms(Camera camera, Matrix4 modelMatrix){
//        updateMatrices3(currentTime);

        int offset = 0;
        setUniformMatrix(uniformData, offset, camera.projectionMatrix);
        offset += 16*Float.BYTES;
        setUniformMatrix(uniformData, offset, camera.viewMatrix);
        offset += 16*Float.BYTES;
        setUniformMatrix(uniformData, offset, modelMatrix);
        offset += 16*Float.BYTES;
//        uniformData.putFloat(offset, currentTime);
//        offset += 4*Float.BYTES;
        // 3 floats of padding
        setUniformColor(uniformData, offset, 0.0f, 1.0f, 0.4f, 1.0f);
        wgpu.QueueWriteBuffer(LibGPU.queue, uniformBuffer, 0, uniformData, uniformBufferSize);
    }

    public WGPUVertexBufferLayout getVertexBufferLayout(){
        if(vertexBufferLayout == null) {
            VertexAttributes vertexAttributes = new VertexAttributes();
            vertexAttributes.add("position", WGPUVertexFormat.Float32x3, 0);
            vertexAttributes.add("normal", WGPUVertexFormat.Float32x3, 1);
            vertexAttributes.add("color", WGPUVertexFormat.Float32x3, 2);
            vertexAttributes.add("uv", WGPUVertexFormat.Float32x2, 3);
            vertexAttributes.end();

            vertexBufferLayout = vertexAttributes.getVertexBufferLayout();
        }
        return vertexBufferLayout;
    }

    // Bind Group Layout:
    //  uniforms
    //  texture
    //  sampler
    private void defineBindGroupLayout(){
        // Define binding layout
        WGPUBindGroupLayoutEntry bindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(bindingLayout);
        bindingLayout.setBinding(0);
        bindingLayout.setVisibility(WGPUShaderStage.Vertex | WGPUShaderStage.Fragment);
        bindingLayout.getBuffer().setType(WGPUBufferBindingType.Uniform);
        bindingLayout.getBuffer().setMinBindingSize(uniformBufferSize);

        WGPUBindGroupLayoutEntry texBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(texBindingLayout);
        texBindingLayout.setBinding(1);
        texBindingLayout.setVisibility(WGPUShaderStage.Fragment);
        texBindingLayout.getTexture().setSampleType(WGPUTextureSampleType.Float);
        texBindingLayout.getTexture().setViewDimension(WGPUTextureViewDimension._2D);

        WGPUBindGroupLayoutEntry samplerBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(samplerBindingLayout);
        samplerBindingLayout.setBinding(2);
        samplerBindingLayout.setVisibility(WGPUShaderStage.Fragment);
        samplerBindingLayout.getSampler().setType(WGPUSamplerBindingType.Filtering);

        // Create a bind group layout
        WGPUBindGroupLayoutDescriptor bindGroupLayoutDesc = WGPUBindGroupLayoutDescriptor.createDirect();
        bindGroupLayoutDesc.setNextInChain();
        bindGroupLayoutDesc.setLabel("ModelBatch Bind Group Layout");
        bindGroupLayoutDesc.setEntryCount(3);
        bindGroupLayoutDesc.setEntries(bindingLayout, texBindingLayout, samplerBindingLayout);
        bindGroupLayout = wgpu.DeviceCreateBindGroupLayout(device, bindGroupLayoutDesc);

    }

    private void initializePipeline() {

        Pointer shaderModule = shader.getShaderModule();

        WGPURenderPipelineDescriptor pipelineDesc = WGPURenderPipelineDescriptor.createDirect();
        pipelineDesc.setNextInChain();
        pipelineDesc.setLabel("pipeline");

        pipelineDesc.getVertex().setBufferCount(1);
        pipelineDesc.getVertex().setBuffers(getVertexBufferLayout());

        pipelineDesc.getVertex().setModule(shaderModule);
        pipelineDesc.getVertex().setEntryPoint("vs_main");
        pipelineDesc.getVertex().setConstantCount(0);
        pipelineDesc.getVertex().setConstants();

        pipelineDesc.getPrimitive().setTopology(WGPUPrimitiveTopology.TriangleList);
        pipelineDesc.getPrimitive().setStripIndexFormat(WGPUIndexFormat.Undefined);
        pipelineDesc.getPrimitive().setFrontFace(WGPUFrontFace.CCW);
        pipelineDesc.getPrimitive().setCullMode(WGPUCullMode.None);

        WGPUFragmentState fragmentState = WGPUFragmentState.createDirect();
        fragmentState.setNextInChain();
        fragmentState.setModule(shaderModule);
        fragmentState.setEntryPoint("fs_main");
        fragmentState.setConstantCount(0);
        fragmentState.setConstants();

        // blend
        WGPUBlendState blendState = WGPUBlendState.createDirect();
        blendState.getColor().setSrcFactor(WGPUBlendFactor.SrcAlpha);
        blendState.getColor().setDstFactor(WGPUBlendFactor.OneMinusSrcAlpha);
        blendState.getColor().setOperation(WGPUBlendOperation.Add);
        blendState.getAlpha().setSrcFactor(WGPUBlendFactor.Zero);
        blendState.getAlpha().setDstFactor(WGPUBlendFactor.One);
        blendState.getAlpha().setOperation(WGPUBlendOperation.Add);

        WGPUColorTargetState colorTarget = WGPUColorTargetState.createDirect();
        colorTarget.setFormat(LibGPU.surfaceFormat);
        colorTarget.setBlend(blendState);
        colorTarget.setWriteMask(WGPUColorWriteMask.All);

        fragmentState.setTargetCount(1);
        fragmentState.setTargets(colorTarget);

        pipelineDesc.setFragment(fragmentState);

        WGPUDepthStencilState depthStencilState = WGPUDepthStencilState.createDirect();
        setDefault(depthStencilState);
        depthStencilState.setDepthCompare(WGPUCompareFunction.Less);
        depthStencilState.setDepthWriteEnabled(1L);

        //
        WGPUTextureFormat depthTextureFormat = WGPUTextureFormat.Depth24Plus;       // todo
        depthStencilState.setFormat(depthTextureFormat);
        // deactivate stencil
        depthStencilState.setStencilReadMask(0L);
        depthStencilState.setStencilWriteMask(0L);

        pipelineDesc.setDepthStencil(depthStencilState);


        pipelineDesc.getMultisample().setCount(1);
        pipelineDesc.getMultisample().setMask( 0xFFFFFFFF);
        pipelineDesc.getMultisample().setAlphaToCoverageEnabled(0);



        long[] layouts = new long[1];
        layouts[0] = bindGroupLayout.address();
        Pointer layoutPtr = WgpuJava.createLongArrayPointer(layouts);

        // Create the pipeline layout: 1 bind group
        WGPUPipelineLayoutDescriptor layoutDesc = WGPUPipelineLayoutDescriptor.createDirect();
        layoutDesc.setNextInChain();
        layoutDesc.setLabel("Pipeline Layout");
        layoutDesc.setBindGroupLayoutCount(1);
        layoutDesc.setBindGroupLayouts(layoutPtr);
        pipelineLayout = wgpu.DeviceCreatePipelineLayout(device, layoutDesc);

        pipelineDesc.setLayout(pipelineLayout);
        pipeline = wgpu.DeviceCreateRenderPipeline(device, pipelineDesc);
        wgpu.ShaderModuleRelease(shaderModule);
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

    private void setDefault(WGPUStencilFaceState stencilFaceState) {
        stencilFaceState.setCompare( WGPUCompareFunction.Always);
        stencilFaceState.setFailOp( WGPUStencilOperation.Keep);
        stencilFaceState.setDepthFailOp( WGPUStencilOperation.Keep);
        stencilFaceState.setPassOp( WGPUStencilOperation.Keep);
    }


    private void setDefault(WGPUDepthStencilState  depthStencilState ) {
        depthStencilState.setFormat(WGPUTextureFormat.Undefined);
        depthStencilState.setDepthWriteEnabled(0L);
        depthStencilState.setDepthCompare(WGPUCompareFunction.Always);
        depthStencilState.setStencilReadMask(0xFFFFFFFF);
        depthStencilState.setStencilWriteMask(0xFFFFFFFF);
        depthStencilState.setDepthBias(0);
        depthStencilState.setDepthBiasSlopeScale(0);
        depthStencilState.setDepthBiasClamp(0);
        setDefault(depthStencilState.getStencilFront());
        setDefault(depthStencilState.getStencilBack());
    }

}
