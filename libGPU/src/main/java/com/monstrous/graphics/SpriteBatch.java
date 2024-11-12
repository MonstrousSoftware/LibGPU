package com.monstrous.graphics;

import com.monstrous.LibGPU;
import com.monstrous.math.Matrix4;
import com.monstrous.utils.WgpuJava;
import com.monstrous.wgpu.*;
import jnr.ffi.Pointer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SpriteBatch {
    private WGPU wgpu;
    private int maxSprites;
    private boolean begun;
    private int vertexSize;
    private float[] vertFloats;
    private int[] indexValues;
    private int numRects;
    private Pointer vertexBuffer;
    private Pointer indexBuffer;
    private Pointer uniformBuffer;
    private Pointer bindGroupLayout;
    private Pointer bindGroup;
    private Pointer pipeline;
    private int uniformBufferSize;
    private Texture texture;
    private Matrix4 projectionMatrix;




    public SpriteBatch() {
        this(8192); // default nr
    }

    public SpriteBatch(int maxSprites) {
        this.maxSprites = maxSprites;
        begun = false;
        wgpu = LibGPU.wgpu;

        // vertex: x, y, u, v
        vertexSize = 4; // floats

        indexValues = new int[maxSprites * 6];    // 6 indices per sprite
        vertFloats = new float[maxSprites * 4 * vertexSize];
        numRects = 0;

        projectionMatrix = new Matrix4();

        texture = new Texture("monstrous.png");
        createBuffers();
        makeBindGroupLayout();
        initBindGroups();
        initializePipeline();

        resize(640, 480);    // todo
    }

    public void resize(int w, int h) {
        projectionMatrix.setToOrtho(0f, w, 0f, h, -1f, 1f);
        setUniforms();
    }


    public void begin() {
        if (begun)
            throw new RuntimeException("Must end() before begin()");
        begun = true;

    }

    public void end() {
        if (!begun)
            throw new RuntimeException("Cannot end() without begin()");
        begun = false;
        flush();

    }

    public void draw(Texture texture, int x, int y, int w, int h) {
        if (!begun)
            throw new RuntimeException("Must call begin() first");

        addRect(x, y, w, h);
        // todo for now all with the same texture
        //this.texture = texture;
    }

    private void addRect(int x, int y, int w, int h) {
        // u,v is 0 to 1 for now

        int i = numRects * 4 * vertexSize;
        vertFloats[i++] = x;
        vertFloats[i++] = y;
        vertFloats[i++] = 0;    // u
        vertFloats[i++] = 1;    // v

        vertFloats[i++] = x;
        vertFloats[i++] = y + h;
        vertFloats[i++] = 0;
        vertFloats[i++] = 0;

        vertFloats[i++] = x + h;
        vertFloats[i++] = y + h;
        vertFloats[i++] = 1;
        vertFloats[i++] = 0;

        vertFloats[i++] = x + h;
        vertFloats[i++] = y;
        vertFloats[i++] = 1;
        vertFloats[i++] = 1;

        int k = numRects * 6;
        int start = numRects * 4;
        indexValues[k++] = start;
        indexValues[k++] = start + 1;
        indexValues[k++] = start + 2;

        indexValues[k++] = start;
        indexValues[k++] = start + 2;
        indexValues[k++] = start + 3;
        numRects++;
    }

    private void flush() {

        Pointer data = WgpuJava.createFloatArrayPointer(vertFloats);
        // Upload geometry data to the buffer
        wgpu.QueueWriteBuffer(LibGPU.queue, vertexBuffer, 0, data, (int) numRects*4*vertexSize*Float.BYTES);


        Pointer idata = WgpuJava.createIntegerArrayPointer(indexValues);

        // Upload data to the buffer
        wgpu.QueueWriteBuffer(LibGPU.queue, indexBuffer, 0, idata, (int) numRects*6*Integer.BYTES);

    }

    private void setUniformMatrix(Pointer data, int offset, Matrix4 mat) {
        for (int i = 0; i < 16; i++) {
            data.putFloat(offset + i * Float.BYTES, mat.val[i]);
        }
    }

    private void createBuffers() {
        // Create vertex buffer
        WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
        bufferDesc.setLabel("Vertex buffer");
        bufferDesc.setUsage(WGPUBufferUsage.CopyDst | WGPUBufferUsage.Vertex);
        bufferDesc.setSize((long) maxSprites * 4 * vertexSize * Float.BYTES);
        bufferDesc.setMappedAtCreation(0L);
        vertexBuffer = wgpu.DeviceCreateBuffer(LibGPU.device, bufferDesc);

        // Create index buffer
        bufferDesc.setLabel("Index buffer");
        bufferDesc.setUsage(WGPUBufferUsage.CopyDst | WGPUBufferUsage.Index);
        long sz = (long) maxSprites * 6 * Integer.BYTES;
        sz = (sz + 3) & ~3; // round up to the next multiple of 4
        bufferDesc.setSize(sz);
        bufferDesc.setMappedAtCreation(0L);
        indexBuffer = wgpu.DeviceCreateBuffer(LibGPU.device, bufferDesc);

        // Create uniform buffer
        uniformBufferSize = 16 * Float.BYTES;

        //WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
        bufferDesc.setLabel("Uniform buffer");
        bufferDesc.setUsage(WGPUBufferUsage.CopyDst |WGPUBufferUsage.Uniform );
        bufferDesc.setSize(16 * Float.BYTES);
        bufferDesc.setMappedAtCreation(0L);
        uniformBuffer =LibGPU.wgpu.DeviceCreateBuffer(LibGPU.device,bufferDesc);
    }

    private void setUniforms(){
        // P matrix: 16 float
        float[] uniforms = new float[16];
        Pointer uniformData = WgpuJava.createFloatArrayPointer(uniforms);   // copy to native memory


        int offset = 0;
        setUniformMatrix(uniformData, offset, projectionMatrix);
        offset += 16*Float.BYTES;

        LibGPU.wgpu.QueueWriteBuffer(LibGPU.queue, uniformBuffer, 0, uniformData, uniformBufferSize);
    }

    private void makeBindGroupLayout() {
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
        bindGroupLayoutDesc.setLabel("SpriteBatch binding group layout");
        bindGroupLayoutDesc.setEntryCount(3);
        bindGroupLayoutDesc.setEntries(bindingLayout, texBindingLayout, samplerBindingLayout);
        bindGroupLayout = LibGPU.wgpu.DeviceCreateBindGroupLayout(LibGPU.device, bindGroupLayoutDesc);
    }

    private void initBindGroups() {
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
        bindGroup = LibGPU.wgpu.DeviceCreateBindGroup(LibGPU.device, bindGroupDesc);
    }

    private void initializePipeline() {

        // Create Shader Module
        WGPUShaderModuleDescriptor shaderDesc = WGPUShaderModuleDescriptor.createDirect();
        shaderDesc.setLabel("My Shader");

        String shaderSource = null;
        try {
            shaderSource = Files.readString(Paths.get("sprite.wgsl"));
        } catch (IOException e) {
            e.printStackTrace();
        }


        WGPUShaderModuleWGSLDescriptor shaderCodeDesc = WGPUShaderModuleWGSLDescriptor.createDirect();
        shaderCodeDesc.getChain().setNext();
        shaderCodeDesc.getChain().setSType(WGPUSType.ShaderModuleWGSLDescriptor);
        shaderCodeDesc.setCode(shaderSource);

        shaderDesc.getNextInChain().set(shaderCodeDesc.getPointerTo());

        Pointer shaderModule = wgpu.DeviceCreateShaderModule(LibGPU.device, shaderDesc);

        // DEFINE VERTEX ATTRIBUTES
        //
        //  create an array of WGPUVertexAttribute
        int attribCount = 2;

        WGPUVertexAttribute positionAttrib =  WGPUVertexAttribute.createDirect();

        int offset = 0;
        positionAttrib.setFormat(WGPUVertexFormat.Float32x2);   // XY only
        positionAttrib.setOffset(offset);
        positionAttrib.setShaderLocation(0);
        offset += 2 * Float.BYTES;

//        WGPUVertexAttribute colorAttrib = WGPUVertexAttribute.createDirect();   // freed where?
//        colorAttrib.setFormat(WGPUVertexFormat.Float32x3);  // RGB
//        colorAttrib.setOffset(offset);
//        colorAttrib.setShaderLocation(1);
//        offset += 3 * Float.BYTES;

        WGPUVertexAttribute uvAttrib = WGPUVertexAttribute.createDirect();   // freed where?
        uvAttrib.setFormat(WGPUVertexFormat.Float32x2); // UV
        uvAttrib.setOffset(offset);
        uvAttrib.setShaderLocation(1);
        offset += 2 * Float.BYTES;


        WGPUVertexBufferLayout vertexBufferLayout = WGPUVertexBufferLayout.createDirect();
        vertexBufferLayout.setAttributeCount(attribCount);

        vertexBufferLayout.setAttributes(positionAttrib, uvAttrib);
        vertexBufferLayout.setArrayStride(offset);
        vertexBufferLayout.setStepMode(WGPUVertexStepMode.Vertex);


        WGPURenderPipelineDescriptor pipelineDesc = WGPURenderPipelineDescriptor.createDirect();
        pipelineDesc.setNextInChain();
        pipelineDesc.setLabel("pipeline");

        pipelineDesc.getVertex().setBufferCount(1);
        pipelineDesc.getVertex().setBuffers(vertexBufferLayout);

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
        colorTarget.setFormat(WGPUTextureFormat.BGRA8Unorm); //  surfaceFormat);            // todo
        colorTarget.setBlend(blendState);
        colorTarget.setWriteMask(WGPUColorWriteMask.All);

        fragmentState.setTargetCount(1);
        fragmentState.setTargets(colorTarget);

        pipelineDesc.setFragment(fragmentState);

//        WGPUDepthStencilState depthStencilState = WGPUDepthStencilState.createDirect();
//        setDefault(depthStencilState);
//        depthStencilState.setDepthCompare(WGPUCompareFunction.Less);
//        depthStencilState.setDepthWriteEnabled(1L);
//        WGPUTextureFormat depthTextureFormat = WGPUTextureFormat.Depth24Plus;
//        depthStencilState.setFormat(depthTextureFormat);
//        // deactivate stencil
//        depthStencilState.setStencilReadMask(0L);
//        depthStencilState.setStencilWriteMask(0L);

        pipelineDesc.setDepthStencil();


//        long[] formats = new long[1];
//        formats[0] = depthTextureFormat.ordinal();
//        Pointer formatPtr = WgpuJava.createLongArrayPointer(formats);
//
//        // Create the depth texture
//        WGPUTextureDescriptor depthTextureDesc = WGPUTextureDescriptor.createDirect();
//        depthTextureDesc.setNextInChain();
//        depthTextureDesc.setDimension( WGPUTextureDimension._2D);
//        depthTextureDesc.setFormat( depthTextureFormat );
//        depthTextureDesc.setMipLevelCount(1);
//        depthTextureDesc.setSampleCount(1);
//        depthTextureDesc.getSize().setWidth(640);           // todo
//        depthTextureDesc.getSize().setHeight(480);
//        depthTextureDesc.getSize().setDepthOrArrayLayers(1);
//        depthTextureDesc.setUsage( WGPUTextureUsage.RenderAttachment );
//        depthTextureDesc.setViewFormatCount(1);
//        depthTextureDesc.setViewFormats( formatPtr );
//        depthTexture = wgpu.DeviceCreateTexture(device, depthTextureDesc);


//        // Create the view of the depth texture manipulated by the rasterizer
//        WGPUTextureViewDescriptor depthTextureViewDesc = WGPUTextureViewDescriptor.createDirect();
//        depthTextureViewDesc.setAspect(WGPUTextureAspect.DepthOnly);
//        depthTextureViewDesc.setBaseArrayLayer(0);
//        depthTextureViewDesc.setArrayLayerCount(1);
//        depthTextureViewDesc.setBaseMipLevel(0);
//        depthTextureViewDesc.setMipLevelCount(1);
//        depthTextureViewDesc.setDimension( WGPUTextureViewDimension._2D);
//        depthTextureViewDesc.setFormat(depthTextureFormat);
//        depthTextureView = wgpu.TextureCreateView(depthTexture, depthTextureViewDesc);

        pipelineDesc.getMultisample().setCount(1);
        pipelineDesc.getMultisample().setMask( 0xFFFFFFFF);
        pipelineDesc.getMultisample().setAlphaToCoverageEnabled(0);


        long[] layouts = new long[1];
        layouts[0] = bindGroupLayout.address();
        Pointer layoutPtr = WgpuJava.createLongArrayPointer(layouts);       // todo need this?

        // Create the pipeline layout
        WGPUPipelineLayoutDescriptor layoutDesc = WGPUPipelineLayoutDescriptor.createDirect();
        layoutDesc.setNextInChain();
        layoutDesc.setLabel("Pipeline Layout");
        layoutDesc.setBindGroupLayoutCount(1);
        layoutDesc.setBindGroupLayouts(layoutPtr);
        Pointer layout = wgpu.DeviceCreatePipelineLayout(LibGPU.device, layoutDesc);

        pipelineDesc.setLayout(layout);
        pipeline = wgpu.DeviceCreateRenderPipeline(LibGPU.device, pipelineDesc);
        wgpu.ShaderModuleRelease(shaderModule);


    }

    public void renderPass(Pointer renderPass) {

        wgpu.RenderPassEncoderSetPipeline(renderPass, pipeline);

        // Set vertex buffer while encoding the render pass
        wgpu.RenderPassEncoderSetVertexBuffer(renderPass, 0, vertexBuffer, 0, wgpu.BufferGetSize(vertexBuffer));
        wgpu.RenderPassEncoderSetIndexBuffer(renderPass, indexBuffer, WGPUIndexFormat.Uint32, 0, wgpu.BufferGetSize(indexBuffer));

        int[] offset = new int[1];
        offset[0] = 0;
        Pointer offsetPtr = WgpuJava.createIntegerArrayPointer(offset);     // todo need this?


        wgpu.RenderPassEncoderSetBindGroup(renderPass, 0, bindGroup, 1, offsetPtr);
        wgpu.RenderPassEncoderDrawIndexed(renderPass, numRects * 6, 1, 0, 0, 0);


        wgpu.RenderPassEncoderEnd(renderPass);
        wgpu.RenderPassEncoderRelease(renderPass);
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

    public void dispose(){

        wgpu.BufferRelease(vertexBuffer);
        wgpu.BufferRelease(indexBuffer);
        wgpu.BufferRelease(uniformBuffer);
        wgpu.BindGroupLayoutRelease(bindGroupLayout);
        wgpu.BindGroupRelease(bindGroup);
        texture.dispose();
    }
}
