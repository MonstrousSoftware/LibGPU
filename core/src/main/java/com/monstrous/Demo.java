package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.math.Matrix4;
import com.monstrous.wgpu.*;
import com.monstrous.wgpuUtils.WgpuJava;
import jnr.ffi.Pointer;


public class Demo implements ApplicationListener {
    private WGPU wgpu;

    private Pointer device;
    private Pointer queue;
    private Pointer pipeline;

    private Mesh mesh;
    private ShaderProgram shader;
    private Pointer uniformBuffer;
    private Pointer layout;
    private Pointer bindGroupLayout;
    private Pointer bindGroup;
    private Pointer bindGroup2;
    private int uniformBufferSize;  // in bytes
    private int uniformStride;
    private int uniformInstances;
    private Pointer uniformData;

    private Camera camera;
//    private Matrix4 projectionMatrix;
//    private Matrix4 viewMatrix;
    private Matrix4 modelMatrix;
    private Texture texture;
    private Texture texture2;
    private Texture textureFont;
    private float currentTime;
    private SpriteBatch batch;
    private long startTime;
    private int frames;

    public void create() {

        startTime = System.nanoTime();
        frames = 0;

        wgpu = LibGPU.wgpu;
        device = LibGPU.device;
        queue = LibGPU.queue;



        shader = new ShaderProgram("shader.wgsl");

        mesh = new Mesh("pyramid.txt");

        initializePipeline();


        texture = new Texture("monstrous.png", false);
        texture2 = new Texture("jackRussel.png", false);
        textureFont = new Texture("lsans-15.png", false);


        camera = new Camera();
        float aspectRatio = (float)LibGPU.graphics.getWidth()/(float)LibGPU.graphics.getHeight();
        camera.projectionMatrix.setToPerspective(1.5f, 0.01f, 9.0f, aspectRatio);
        camera.position.set(0, 1, -3);
        camera.direction.set(0,0f, 1f);
        camera.update();


        modelMatrix = new Matrix4();
//        modelMatrix.scale(0.5f, 0.5f, 0.5f);
//        modelMatrix.translate(1,0,0);
//        modelMatrix.setToYRotation(0.59f);
        System.out.println(modelMatrix.toString());
        //viewMatrix = new Matrix4();

        // P matrix: 16 float
        // M matrix: 16 float
        // V matrix: 16 float
        // time: 1 float
        // 3 floats padding
        // color: 4 floats
        uniformBufferSize = (3*16+8) * Float.BYTES;
        float[] uniforms = new float[uniformBufferSize];
        uniformData = WgpuJava.createFloatArrayPointer(uniforms);

         int minAlign = (int)LibGPU.supportedLimits.getLimits().getMinUniformBufferOffsetAlignment();
        uniformStride = ceilToNextMultiple(uniformBufferSize, minAlign);
        uniformInstances = 1;   // how many sets of uniforms?

        System.out.println("min uniform alignment: "+minAlign);
        System.out.println("uniform stride: "+uniformStride);
        System.out.println("uniformBufferSize: "+uniformBufferSize);
        makeUniformBuffer();




        bindGroup = initBindGroups(texture);
        bindGroup2 = initBindGroups(texture2);

        batch = new SpriteBatch();

    }

    private int ceilToNextMultiple(int value, int step){
        int d = value / step + (value % step == 0 ? 0 : 1);
        return step * d;
    }



    private void makeUniformBuffer() {

        // Create uniform buffer
        WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
        bufferDesc.setLabel("Uniform buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.Uniform );
        bufferDesc.setSize((long) uniformStride * uniformInstances);
        bufferDesc.setMappedAtCreation(0L);
        uniformBuffer = wgpu.DeviceCreateBuffer(device, bufferDesc);

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

    private void initializePipeline() {

        Pointer shaderModule = shader.getShaderModule();

        WGPURenderPipelineDescriptor pipelineDesc = WGPURenderPipelineDescriptor.createDirect();
        pipelineDesc.setNextInChain();
        pipelineDesc.setLabel("pipeline");

        pipelineDesc.getVertex().setBufferCount(1);
        pipelineDesc.getVertex().setBuffers(mesh.getVertexBufferLayout());

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
        bindGroupLayoutDesc.setLabel("My BG Layout");
        bindGroupLayoutDesc.setEntryCount(3);
        bindGroupLayoutDesc.setEntries(bindingLayout, texBindingLayout, samplerBindingLayout);
        bindGroupLayout = wgpu.DeviceCreateBindGroupLayout(device, bindGroupLayoutDesc);


        long[] layouts = new long[1];
        layouts[0] = bindGroupLayout.address();
        Pointer layoutPtr = WgpuJava.createLongArrayPointer(layouts);

        // Create the pipeline layout
        WGPUPipelineLayoutDescriptor layoutDesc = WGPUPipelineLayoutDescriptor.createDirect();
        layoutDesc.setNextInChain();
        layoutDesc.setLabel("Pipeline Layout");
        layoutDesc.setBindGroupLayoutCount(1);
        layoutDesc.setBindGroupLayouts(layoutPtr);
        layout = wgpu.DeviceCreatePipelineLayout(device, layoutDesc);

        pipelineDesc.setLayout(layout);
        pipeline = wgpu.DeviceCreateRenderPipeline(device, pipelineDesc);
        wgpu.ShaderModuleRelease(shaderModule);


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

    private void updateMatrices(float currentTime){
        camera.projectionMatrix.setToOrtho(-1.1f, 1.1f, -1.1f, 1.1f, -1, 1);

        modelMatrix.setToXRotation((float) ( -0.5f*Math.PI ));  // tilt to face camera
        camera.viewMatrix.idt();

    }

    private void updateMatrices2(float currentTime){

        float aspectRatio = (float)LibGPU.graphics.getWidth()/(float)LibGPU.graphics.getHeight();
        camera.projectionMatrix.setToPerspective(1.5f, 0.01f, 9.0f, aspectRatio);
        //projectionMatrix.setToProjection(0.001f, 3.0f, 60f, 640f/480f);
        //modelMatrix.setToYRotation(currentTime*0.2f).scale(0.5f);
        modelMatrix.idt();//.setToXRotation((float) ( -0.5f*Math.PI ));

        Matrix4 RT = new Matrix4().setToXRotation((float) ( -0.5f*Math.PI ));

        //modelMatrix.idt().scale(0.5f);
        camera.viewMatrix.idt();
        Matrix4 R1 = new Matrix4().setToYRotation(currentTime*0.6f);
        Matrix4 R2 = new Matrix4().setToXRotation((float) (-0.5* Math.PI / 4.0)); // tilt the view
        Matrix4 S = new Matrix4().scale(1.6f);
        Matrix4 T = new Matrix4().translate(0.8f, 0f, 0f);
        Matrix4 TC = new Matrix4().translate(0.0f, -1f, 3f);


        modelMatrix.idt().mul(R1).mul(T).mul(RT);

        TC.mul(S);
        R2.mul(TC); // tilt
        camera.viewMatrix.set(R2);
        //viewMatrix.translate(0,0.2f, 0);
        //viewMatrix.setToZRotation((float) (Math.PI*0.5f));
        //viewMatrix.translate(0, 0, (float)Math.cos(currentTime)*0.5f );
    }

    private void updateMatrices3(float currentTime){
        Matrix4 RT = new Matrix4().setToXRotation((float) ( -0.5f*Math.PI ));
        Matrix4 R1 = new Matrix4().setToYRotation(currentTime*0.6f);
        Matrix4 T = new Matrix4().translate(0.8f, 0f, 0f);
        modelMatrix.idt().mul(R1).mul(T).mul(RT);
    }

    private void setUniforms(){


        updateMatrices3(currentTime);

        int offset = 0;
        setUniformMatrix(uniformData, offset, camera.projectionMatrix);
        offset += 16*Float.BYTES;
        setUniformMatrix(uniformData, offset, camera.viewMatrix);
        offset += 16*Float.BYTES;
        setUniformMatrix(uniformData, offset, modelMatrix);
        offset += 16*Float.BYTES;
        uniformData.putFloat(offset, currentTime);
        offset += 4*Float.BYTES;
        // 3 floats of padding
        setUniformColor(uniformData, offset, 0.0f, 1.0f, 0.4f, 1.0f);
        wgpu.QueueWriteBuffer(queue, uniformBuffer, 0, uniformData, uniformBufferSize);
    }

    public void render( float deltaTime ){
        currentTime += deltaTime;


        setUniforms();

        WGPUCommandEncoderDescriptor encoderDescriptor = WGPUCommandEncoderDescriptor.createDirect();
        encoderDescriptor.setNextInChain();
        encoderDescriptor.setLabel("My Encoder");

        Pointer encoder = wgpu.DeviceCreateCommandEncoder(device, encoderDescriptor);

        WGPURenderPassColorAttachment renderPassColorAttachment = WGPURenderPassColorAttachment.createDirect();
        renderPassColorAttachment.setNextInChain();
        renderPassColorAttachment.setView(LibGPU.application.targetView);
        renderPassColorAttachment.setResolveTarget(WgpuJava.createNullPointer());
        renderPassColorAttachment.setLoadOp(WGPULoadOp.Clear);
        renderPassColorAttachment.setStoreOp(WGPUStoreOp.Store);

        renderPassColorAttachment.getClearValue().setR(0.25);
        renderPassColorAttachment.getClearValue().setG(0.25);
        renderPassColorAttachment.getClearValue().setB(0.25);
        renderPassColorAttachment.getClearValue().setA(1.0);

        renderPassColorAttachment.setDepthSlice(wgpu.WGPU_DEPTH_SLICE_UNDEFINED);


        WGPURenderPassDepthStencilAttachment depthStencilAttachment = WGPURenderPassDepthStencilAttachment.createDirect();
        depthStencilAttachment.setView( LibGPU.application.depthTextureView );
        depthStencilAttachment.setDepthClearValue(1.0f);
        depthStencilAttachment.setDepthLoadOp(WGPULoadOp.Clear);
        depthStencilAttachment.setDepthStoreOp(WGPUStoreOp.Store);
        depthStencilAttachment.setDepthReadOnly(0L);
        depthStencilAttachment.setStencilClearValue(0);
        depthStencilAttachment.setStencilLoadOp(WGPULoadOp.Undefined);
        depthStencilAttachment.setStencilStoreOp(WGPUStoreOp.Undefined);
        depthStencilAttachment.setStencilReadOnly(1L);



        WGPURenderPassDescriptor renderPassDescriptor = WGPURenderPassDescriptor.createDirect();
        renderPassDescriptor.setNextInChain();

        renderPassDescriptor.setLabel("Main Render Pass");

        renderPassDescriptor.setColorAttachmentCount(1);
        renderPassDescriptor.setColorAttachments( renderPassColorAttachment );
        renderPassDescriptor.setOcclusionQuerySet(WgpuJava.createNullPointer());
        renderPassDescriptor.setDepthStencilAttachment( depthStencilAttachment );
        renderPassDescriptor.setTimestampWrites();


        Pointer renderPass = wgpu.CommandEncoderBeginRenderPass(encoder, renderPassDescriptor);
// [...] Use Render Pass

        boolean testSprites = false;
        if(testSprites) {


            // SpriteBatch testing
            batch.begin(renderPass);    // todo param for now
//char id=65 x=80 y=33 width=11 height=13 xoffset=-1 yoffset=2 xadvance=9 page=0 chnl=0

//        TextureRegion letterA = new TextureRegion(textureFont, 80f/256f, (33f+13f)/128f, (80+11f)/256f, 33f/128f);
//        batch.draw(letterA, 100, 100);

            batch.setColor(1, 0, 0, 0.1f);
            batch.draw(texture, 0, 0, 100, 100);

            batch.draw(texture, 0, 0, 300, 300, 0.5f, 0.5f, 0.9f, 0.1f);
            batch.draw(texture, 300, 300, 50, 50);
            batch.setColor(1, 1, 1, 1);

            batch.draw(texture2, 400, 100, 100, 100);

            TextureRegion region = new TextureRegion(texture2, 0, 0, 512, 512);
            batch.draw(region, 200, 300, 64, 64);

            TextureRegion region2 = new TextureRegion(texture2, 0f, 1f, .5f, 0.5f);
            batch.draw(region2, 400, 300, 64, 64);

            int W = LibGPU.graphics.getWidth();
            int H = LibGPU.graphics.getHeight();
            batch.setColor(0, 1, 0, 1);
            for (int i = 0; i < 8000; i++) {
                batch.draw(texture2, (int) (Math.random() * W), (int) (Math.random() * H), 32, 32);
            }
            batch.end();
        }
        else {

            wgpu.RenderPassEncoderSetPipeline(renderPass, pipeline);

            Pointer vertexBuffer = mesh.getVertexBuffer();
            Pointer indexBuffer = mesh.getIndexBuffer();
            int indexCount = mesh.getIndexCount();

            // Set vertex buffer while encoding the render pass
            wgpu.RenderPassEncoderSetVertexBuffer(renderPass, 0, vertexBuffer, 0, wgpu.BufferGetSize(vertexBuffer));
            wgpu.RenderPassEncoderSetIndexBuffer(renderPass, indexBuffer, WGPUIndexFormat.Uint32, 0, wgpu.BufferGetSize(indexBuffer));

            Pointer bg = initBindGroups(texture);
            wgpu.RenderPassEncoderSetBindGroup(renderPass, 0, bg, 0, null);
            wgpu.RenderPassEncoderDrawIndexed(renderPass, 3, 1, 0, 0, 0);
            wgpu.BindGroupRelease(bg);

            wgpu.RenderPassEncoderSetVertexBuffer(renderPass, 0, vertexBuffer, 0, wgpu.BufferGetSize(vertexBuffer));
            wgpu.RenderPassEncoderSetIndexBuffer(renderPass, indexBuffer, WGPUIndexFormat.Uint32, 0, wgpu.BufferGetSize(indexBuffer));

            bg = initBindGroups(texture2);
            wgpu.RenderPassEncoderSetBindGroup(renderPass, 0, bg, 0, null);
            wgpu.RenderPassEncoderDrawIndexed(renderPass, indexCount, 1, 0, 0, 0);
            wgpu.BindGroupRelease(bg);

            wgpu.RenderPassEncoderEnd(renderPass);

            wgpu.RenderPassEncoderRelease(renderPass);
        }


        WGPUCommandBufferDescriptor bufferDescriptor =  WGPUCommandBufferDescriptor.createDirect();
        bufferDescriptor.setNextInChain();
        bufferDescriptor.setLabel("Command Buffer");
        Pointer commandBuffer = wgpu.CommandEncoderFinish(encoder, bufferDescriptor);
        wgpu.CommandEncoderRelease(encoder);


        long[] buffers = new long[1];
        buffers[0] = commandBuffer.address();
        Pointer bufferPtr = WgpuJava.createLongArrayPointer(buffers);
        //System.out.println("Pointer: "+bufferPtr.toString());
        //System.out.println("Submitting command...");
        wgpu.QueueSubmit(queue, 1, bufferPtr);

        wgpu.CommandBufferRelease(commandBuffer);
        //System.out.println("Command submitted...");


        // At the end of the frame


        if (System.nanoTime() - startTime > 1000000000) {
            System.out.println("SpriteBatch : fps: " + frames  );
            frames = 0;
            startTime = System.nanoTime();
        }
        frames++;

    }

    public void dispose(){
        // cleanup
        System.out.println("demo exit");
        texture.dispose();
        texture2.dispose();
        batch.dispose();
        mesh.dispose();

        wgpu.PipelineLayoutRelease(layout);
        wgpu.BindGroupLayoutRelease(bindGroupLayout);
        wgpu.BindGroupRelease(bindGroup);
        wgpu.BufferRelease(uniformBuffer);
        wgpu.RenderPipelineRelease(pipeline);

    }

    @Override
    public void resize(int width, int height) {
        System.out.println("demo got resize");
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
