package com.monstrous.graphics;

import com.monstrous.LibGPU;
import com.monstrous.math.Matrix4;
import com.monstrous.wgpuUtils.WgpuJava;
import com.monstrous.wgpu.*;
import jnr.ffi.Pointer;

// todo optim: index data could be precalculated and static
// todo support packed color to reduce vertex size

public class SpriteBatch {
    private WGPU wgpu;
    private ShaderProgram shader;
    private int maxSprites;
    private boolean begun;
    private int vertexSize;
    private float[] vertFloats;
    private int[] indexValues;
    private int numRects;
    private Color tint;
    private Pointer vertexBuffer;
    private Pointer indexBuffer;
    private Pointer uniformBuffer;
    private Pointer bindGroupLayout;
    private Pointer pipeline;
    private int uniformBufferSize;
    private Texture texture;
    private Matrix4 projectionMatrix;
    private Pointer renderPass;
    private int vbOffset;
    private int ibOffset;




    public SpriteBatch() {
        this(8192); // default nr
    }

    public SpriteBatch(int maxSprites) {
        this.maxSprites = maxSprites;
        begun = false;
        wgpu = LibGPU.wgpu;

        // vertex: x, y, u, v, r, g, b, a
        vertexSize = 8; // floats
        shader = new ShaderProgram("sprite.wgsl");

        indexValues = new int[maxSprites * 6];    // 6 indices per sprite
        vertFloats = new float[maxSprites * 4 * vertexSize];

        projectionMatrix = new Matrix4();

        tint = new Color(1,1,1,1);

        createBuffers();
        makeBindGroupLayout();

        initializePipeline();

        resize(LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
    }

    public void resize(int w, int h) {
        projectionMatrix.setToOrtho(0f, w, 0f, h, -1f, 1f);
        setUniforms();
    }

    public void setColor(float r, float g, float b, float a){
        tint.set(r,g,b,a);
    }

    public void begin(Pointer renderPass) { // todo can we avoid this param?
        this.renderPass = renderPass;

        if (begun)
            throw new RuntimeException("Must end() before begin()");
        begun = true;
        numRects = 0;
        vbOffset = 0;
        ibOffset = 0;

        wgpu.RenderPassEncoderSetPipeline(renderPass, pipeline);
    }

    public void flush() {
        if(numRects == 0)
            return;

        // Upload geometry data to the buffer
        int numFloats = numRects * 4 * vertexSize;
        Pointer data = WgpuJava.createDirectPointer(numFloats * Float.BYTES);
        data.put(0, vertFloats, 0, numFloats);
        wgpu.QueueWriteBuffer(LibGPU.queue, vertexBuffer, vbOffset, data, (int) numFloats*Float.BYTES);


        // Upload index data to the buffer
        Pointer idata = WgpuJava.createIntegerArrayPointer(indexValues);
        wgpu.QueueWriteBuffer(LibGPU.queue, indexBuffer, ibOffset, idata, (int) numRects*6*Integer.BYTES);


        Pointer texBG = makeBindGroup(texture);

        // Set vertex buffer while encoding the render pass
        wgpu.RenderPassEncoderSetVertexBuffer(renderPass, 0, vertexBuffer, vbOffset, (long) numFloats *Float.BYTES);
        wgpu.RenderPassEncoderSetIndexBuffer(renderPass, indexBuffer, WGPUIndexFormat.Uint32, ibOffset, (long)numRects*6*Integer.BYTES);

        wgpu.RenderPassEncoderSetBindGroup(renderPass, 0, texBG, 0, WgpuJava.createNullPointer());
        wgpu.RenderPassEncoderDrawIndexed(renderPass, numRects * 6, 1, 0, 0, 0);
        wgpu.BindGroupRelease(texBG);


        vbOffset += numFloats*Float.BYTES;
        ibOffset += numRects*6*Integer.BYTES;
        numRects = 0;   // reset
    }

    public void end() {
        if (!begun)
            throw new RuntimeException("Cannot end() without begin()");
        begun = false;
        flush();

        wgpu.RenderPassEncoderEnd(renderPass);
        wgpu.RenderPassEncoderRelease(renderPass);
    }


    public void draw(Texture texture, float x, float y) {
        draw(texture, x, y, texture.getWidth(), texture.getHeight());
    }

    public void draw(Texture texture, float x, float y, float w, float h){
        this.draw(texture, x, y, w, h, 0f, 1f, 1f, 0f);
    }

    public void draw(TextureRegion region, float x, float y){
        this.draw(region.texture, x, y,
                (region.u2-region.u)*region.texture.getWidth(), (region.v-region.v2)*region.texture.getHeight(),
                region.u, region.v, region.u2, region.v2  );
    }

    public void draw(TextureRegion region, float x, float y, float w, float h){
        this.draw(region.texture, x, y, w, h, region.u, region.v, region.u2, region.v2  );
    }


    public void draw (Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2) {
        if (!begun)
            throw new RuntimeException("SpriteBatch: Must call begin() before draw().");

        if(numRects == maxSprites)
            throw new RuntimeException("SpriteBatch: Too many sprites.");

        if(texture != this.texture)  // changing texture, need to flush what we have so far
            flush();

        this.texture = texture;
        addRect(x, y, width, height, u, v, u2, v2);
    }

    private void addRect(float x, float y, float w, float h, float u, float v, float u2, float v2) {
        // u,v is 0 to 1 for now

        int i = numRects * 4 * vertexSize;
        vertFloats[i++] = x;
        vertFloats[i++] = y;
        vertFloats[i++] = u;
        vertFloats[i++] = v;
        vertFloats[i++] = tint.r;
        vertFloats[i++] = tint.g;
        vertFloats[i++] = tint.b;
        vertFloats[i++] = tint.a;

        vertFloats[i++] = x;
        vertFloats[i++] = y + h;
        vertFloats[i++] = u;
        vertFloats[i++] = v2;
        vertFloats[i++] = tint.r;
        vertFloats[i++] = tint.g;
        vertFloats[i++] = tint.b;
        vertFloats[i++] = tint.a;

        vertFloats[i++] = x + w;
        vertFloats[i++] = y + h;
        vertFloats[i++] = u2;
        vertFloats[i++] = v2;
        vertFloats[i++] = tint.r;
        vertFloats[i++] = tint.g;
        vertFloats[i++] = tint.b;
        vertFloats[i++] = tint.a;

        vertFloats[i++] = x + w;
        vertFloats[i++] = y;
        vertFloats[i++] = u2;
        vertFloats[i++] = v;
        vertFloats[i++] = tint.r;
        vertFloats[i++] = tint.g;
        vertFloats[i++] = tint.b;
        vertFloats[i++] = tint.a;

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
        bindingLayout.setVisibility(WGPUShaderStage.Vertex );
        bindingLayout.getBuffer().setType(WGPUBufferBindingType.Uniform);
        bindingLayout.getBuffer().setMinBindingSize(uniformBufferSize);
        bindingLayout.getBuffer().setHasDynamicOffset(0L);

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
        bindGroupLayoutDesc.setLabel("SpriteBatch texture binding group layout");
        bindGroupLayoutDesc.setEntryCount(3);
        bindGroupLayoutDesc.setEntries(bindingLayout, texBindingLayout, samplerBindingLayout);
        bindGroupLayout = LibGPU.wgpu.DeviceCreateBindGroupLayout(LibGPU.device, bindGroupLayoutDesc);
    }


    private Pointer makeBindGroup(Texture texture) {
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
        return LibGPU.wgpu.DeviceCreateBindGroup(LibGPU.device, bindGroupDesc);
    }

    private void initializePipeline() {

        // DEFINE VERTEX ATTRIBUTES
        //
        //  create an array of WGPUVertexAttribute
        int attribCount = 3;

        WGPUVertexAttribute positionAttrib =  WGPUVertexAttribute.createDirect();

        int offset = 0;
        positionAttrib.setFormat(WGPUVertexFormat.Float32x2);   // XY only
        positionAttrib.setOffset(offset);
        positionAttrib.setShaderLocation(0);
        offset += 2 * Float.BYTES;

        WGPUVertexAttribute uvAttrib = WGPUVertexAttribute.createDirect();   // freed where?
        uvAttrib.setFormat(WGPUVertexFormat.Float32x2); // UV
        uvAttrib.setOffset(offset);
        uvAttrib.setShaderLocation(1);
        offset += 2 * Float.BYTES;

        WGPUVertexAttribute colorAttrib = WGPUVertexAttribute.createDirect();   // freed where?
        colorAttrib.setFormat(WGPUVertexFormat.Float32x4); // RGBA
        colorAttrib.setOffset(offset);
        colorAttrib.setShaderLocation(2);
        offset += 4 * Float.BYTES;


        WGPUVertexBufferLayout vertexBufferLayout = WGPUVertexBufferLayout.createDirect();
        vertexBufferLayout.setAttributeCount(attribCount);

        vertexBufferLayout.setAttributes(positionAttrib, uvAttrib, colorAttrib);
        vertexBufferLayout.setArrayStride(offset);
        vertexBufferLayout.setStepMode(WGPUVertexStepMode.Vertex);


        WGPURenderPipelineDescriptor pipelineDesc = WGPURenderPipelineDescriptor.createDirect();
        pipelineDesc.setNextInChain();
        pipelineDesc.setLabel("pipeline");

        pipelineDesc.getVertex().setBufferCount(1);
        pipelineDesc.getVertex().setBuffers(vertexBufferLayout);

        pipelineDesc.getVertex().setModule(shader.getShaderModule());
        pipelineDesc.getVertex().setEntryPoint("vs_main");
        pipelineDesc.getVertex().setConstantCount(0);
        pipelineDesc.getVertex().setConstants();

        pipelineDesc.getPrimitive().setTopology(WGPUPrimitiveTopology.TriangleList);
        pipelineDesc.getPrimitive().setStripIndexFormat(WGPUIndexFormat.Undefined);
        pipelineDesc.getPrimitive().setFrontFace(WGPUFrontFace.CCW);
        pipelineDesc.getPrimitive().setCullMode(WGPUCullMode.None);

        WGPUFragmentState fragmentState = WGPUFragmentState.createDirect();
        fragmentState.setNextInChain();
        fragmentState.setModule(shader.getShaderModule());
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

        pipelineDesc.setDepthStencil();


        pipelineDesc.getMultisample().setCount(1);
        pipelineDesc.getMultisample().setMask( 0xFFFFFFFF);
        pipelineDesc.getMultisample().setAlphaToCoverageEnabled(0);


        long[] layouts = new long[1];
        layouts[0] = bindGroupLayout.address();
        Pointer layoutPtr = WgpuJava.createLongArrayPointer(layouts);       // todo find better method

        // Create the pipeline layout
        WGPUPipelineLayoutDescriptor layoutDesc = WGPUPipelineLayoutDescriptor.createDirect();
        layoutDesc.setNextInChain();
        layoutDesc.setLabel("Pipeline Layout");
        layoutDesc.setBindGroupLayoutCount(1);
        layoutDesc.setBindGroupLayouts(layoutPtr);
        Pointer layout = wgpu.DeviceCreatePipelineLayout(LibGPU.device, layoutDesc);

        pipelineDesc.setLayout(layout);
        pipeline = wgpu.DeviceCreateRenderPipeline(LibGPU.device, pipelineDesc);
//        wgpu.ShaderModuleRelease(shaderModule);


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

    public void dispose(){

        wgpu.BufferRelease(vertexBuffer);
        wgpu.BufferRelease(indexBuffer);
        wgpu.BufferRelease(uniformBuffer);
        wgpu.BindGroupLayoutRelease(bindGroupLayout);
        shader.dispose();
    }
}
