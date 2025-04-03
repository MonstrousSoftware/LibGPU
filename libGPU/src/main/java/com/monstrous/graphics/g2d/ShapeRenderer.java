package com.monstrous.graphics.g2d;

import com.monstrous.LibGPU;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.VertexAttribute;
import com.monstrous.graphics.VertexAttributes;
import com.monstrous.graphics.webgpu.*;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector2;
import com.monstrous.utils.Disposable;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.webgpu.*;
import jnr.ffi.Pointer;


// based on SpriteBatch but without textures.
// for now does very limited set of shapes
// only does line mode, not fill mode.

public class ShapeRenderer implements Disposable {
    private WebGPU_JNI webGPU;
    private int maxShapes;
    private boolean begun;
    private int vertexSize;
    private float[] vertFloats;
    private short[] indexValues;
    private int numRects;
    private Color tint;
    private Pointer vertexBuffer;
    private Pointer indexBuffer;
    private Pointer uniformBuffer;
    private Pointer bindGroupLayout;
    private VertexAttributes vertexAttributes;
    private Pointer pipelineLayout;
    private PipelineSpecification pipelineSpec;
    private int uniformBufferSize;
    private Matrix4 projectionMatrix;
    private RenderPass renderPass;
    private int vbOffset;
    private int ibOffset;
    private Pipelines pipelines;
    private Pipeline prevPipeline;
    private boolean blendingEnabled;
    private float lineWidth;


    public ShapeRenderer() {
        this(8192); // default nr
    }

    public ShapeRenderer(int maxShapes) {
        this.maxShapes = maxShapes;
        begun = false;
        webGPU = LibGPU.webGPU;

        vertexAttributes = new VertexAttributes(VertexAttribute.Usage.POSITION_2D | VertexAttribute.Usage.COLOR_PACKED);
        vertexSize = vertexAttributes.getVertexSizeInBytes() / Float.BYTES;

        indexValues = new short[maxShapes * 6];    // 6 indices per rectangle
        vertFloats = new float[maxShapes * 4 * vertexSize]; // 4 points per rectangle

        projectionMatrix = new Matrix4();

        tint = new Color(Color.WHITE);
        lineWidth = 1f;

        createBuffers();

        bindGroupLayout = createBindGroupLayout();
        pipelineLayout = makePipelineLayout(bindGroupLayout);

        pipelines = new Pipelines();
        pipelineSpec = new PipelineSpecification(vertexAttributes, "shaders/shape.wgsl");
        pipelineSpec.name = "shape renderer pipeline";
        pipelineSpec.numSamples = LibGPU.app.configuration.numSamples;

        resize(LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
    }

    public Matrix4 getProjectionMatrix() {
        return projectionMatrix;
    }

    public void setProjectionMatrix(Matrix4 projection) {
        if(begun)
            flush();
        projectionMatrix.set(projection);
        setUniforms();
    }

    public void resize(int w, int h) {
        projectionMatrix.setToOrtho(0f, w, 0f, h, -1f, 1f);
    }

    public void setColor(float r, float g, float b, float a){
        tint.set(r,g,b,a);
    }

    public void setColor(Color color){
        tint.set(color);
    }

    public void enableBlending(){
        if(blendingEnabled)
           return;
        blendingEnabled = true;
        flush();
        pipelineSpec.enableBlending();
        setPipeline();
    }

    public void disableBlending(){
        if(!blendingEnabled)
            return;
        blendingEnabled = false;
        flush();
        pipelineSpec.disableBlending();
        setPipeline();
    }

    public void begin(){
        begin(null);
    }

    public void begin(Color clearColor) {
        renderPass = RenderPassBuilder.create(clearColor, LibGPU.app.configuration.numSamples);

        if (begun)
            throw new RuntimeException("Must end() before begin()");
        begun = true;
        numRects = 0;
        vbOffset = 0;
        ibOffset = 0;

        prevPipeline = null;
        // set default state
        tint.set(1,1,1,1);
        blendingEnabled = true;
        pipelineSpec.enableBlending();
        pipelineSpec.disableDepthTest();
        setPipeline();
        setUniforms();
    }

    public void flush() {
        if(numRects == 0)
            return;

        // Add number of rectangles from vertFloats[] and indexValues[] the GPU's vertex and index buffer
        //
        int numFloats = numRects * 4 * vertexSize;
        Pointer data = JavaWebGPU.createDirectPointer(numFloats * Float.BYTES);
        data.put(0, vertFloats, 0, numFloats);
        webGPU.wgpuQueueWriteBuffer(LibGPU.queue, vertexBuffer, vbOffset, data, (int) numFloats*Float.BYTES);


        // Upload index data to the buffer
        Pointer idata = JavaWebGPU.createDirectPointer( numRects*6*Short.BYTES);
        idata.put(0, indexValues, 0, numRects*6);
        webGPU.wgpuQueueWriteBuffer(LibGPU.queue, indexBuffer, ibOffset, idata, (int) numRects*6*Short.BYTES);


        Pointer bg = makeBindGroup();

        // Set vertex buffer while encoding the render pass
        renderPass.setVertexBuffer( 0, vertexBuffer, vbOffset, (long) numFloats *Float.BYTES);
        renderPass.setIndexBuffer( indexBuffer, WGPUIndexFormat.Uint16, ibOffset, (long)numRects*6*Short.BYTES);

        renderPass.setBindGroup( 0, bg, 0, JavaWebGPU.createNullPointer());
        renderPass.drawIndexed( numRects * 6, 1, 0, 0, 0);
        webGPU.wgpuBindGroupRelease(bg);


        vbOffset += numFloats*Float.BYTES;
        ibOffset += numRects*6*Short.BYTES;
        numRects = 0;   // reset
    }

    public void end() {
        if (!begun) // catch incorrect usage
            throw new RuntimeException("Cannot end() without begin()");
        begun = false;
        flush();
        renderPass.end();
        renderPass = null;
    }

    // create or reuse pipeline on demand to match the pipeline spec
    private void setPipeline() {
        Pipeline pipeline = pipelines.findPipeline( pipelineLayout, pipelineSpec);
        if (pipeline != prevPipeline) { // avoid unneeded switches
            renderPass.setPipeline( pipeline.getPipeline());
            prevPipeline = pipeline;
        }
    }

    public void setLineWidth(float w){
        this.lineWidth = w;
    }

    public void rect(float x, float y, float w, float h){
        addRect(x,y,w,h);
    }

    public void box(float x1, float y1, float x2, float y2){
        addRect(x1, y1, lineWidth+x2-x1, lineWidth);
        addRect(x1, y2, lineWidth+x2-x1, lineWidth);
        addRect(x1, y1, lineWidth, lineWidth+y2-y1);
        addRect(x2, y1,  lineWidth, lineWidth+y2-y1);
    }

    public void triangle (float x1, float y1, float x2, float y2, float x3, float y3){
        line(x1, y1, x2, y2);
        line(x2, y2, x3, y3);
        line(x3, y3, x1, y1);
    }

    private Vector2 N = new Vector2();

    public void line(float x1, float y1, float x2, float y2){
        float dx = x2 - x1;
        float dy = y2 - y1;
        N.set(dy,dx).nor().scl(lineWidth);

        int i = numRects * 4 * vertexSize;
        vertFloats[i++] = x1-N.x;
        vertFloats[i++] = y1+N.y;
        vertFloats[i++] = tint.toFloatBits();

        vertFloats[i++] = x2-N.x;
        vertFloats[i++] = y2+N.y;
        vertFloats[i++] = tint.toFloatBits();

        vertFloats[i++] = x2+N.x;
        vertFloats[i++] = y2-N.y;
        vertFloats[i++] = tint.toFloatBits();

        vertFloats[i++] = x1+N.x;
        vertFloats[i++] = y1-N.y;
        vertFloats[i++] = tint.toFloatBits();;

        int k = numRects * 6;
        short start = (short)(numRects * 4);
        indexValues[k++] = start;
        indexValues[k++] = (short)(start + 1);
        indexValues[k++] = (short)(start + 2);

        indexValues[k++] = start;
        indexValues[k++] = (short)(start + 2);
        indexValues[k++] = (short)(start + 3);
        numRects++;

    }


    private void addRect(float x, float y, float w, float h) {
        int i = numRects * 4 * vertexSize;
        vertFloats[i++] = x;
        vertFloats[i++] = y;
        vertFloats[i++] = tint.toFloatBits();

        vertFloats[i++] = x;
        vertFloats[i++] = y + h;
        vertFloats[i++] = tint.toFloatBits();

        vertFloats[i++] = x + w;
        vertFloats[i++] = y + h;
        vertFloats[i++] = tint.toFloatBits();;

        vertFloats[i++] = x + w;
        vertFloats[i++] = y;
        vertFloats[i++] = tint.toFloatBits();

        int k = numRects * 6;
        short start = (short)(numRects * 4);
        indexValues[k++] = start;
        indexValues[k++] = (short)(start + 1);
        indexValues[k++] = (short)(start + 2);

        indexValues[k++] = start;
        indexValues[k++] = (short)(start + 2);
        indexValues[k++] = (short)(start + 3);
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
        bufferDesc.setSize((long) maxShapes * 4 * vertexSize * Float.BYTES);
        bufferDesc.setMappedAtCreation(0L);
        vertexBuffer = webGPU.wgpuDeviceCreateBuffer(LibGPU.device, bufferDesc);

        // Create index buffer
        bufferDesc.setLabel("Index buffer");
        bufferDesc.setUsage(WGPUBufferUsage.CopyDst | WGPUBufferUsage.Index);
        long sz = (long) maxShapes * 6 * Short.BYTES;
        sz = (sz + 3) & ~3; // round up to the next multiple of 4
        bufferDesc.setSize(sz);
        bufferDesc.setMappedAtCreation(0L);
        indexBuffer = webGPU.wgpuDeviceCreateBuffer(LibGPU.device, bufferDesc);

        // Create uniform buffer
        uniformBufferSize = 16 * Float.BYTES;

        //WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
        bufferDesc.setLabel("Uniform buffer");
        bufferDesc.setUsage(WGPUBufferUsage.CopyDst |WGPUBufferUsage.Uniform );
        bufferDesc.setSize(16 * Float.BYTES);
        bufferDesc.setMappedAtCreation(0L);
        uniformBuffer =LibGPU.webGPU.wgpuDeviceCreateBuffer(LibGPU.device,bufferDesc);
    }

    private void setUniforms(){
        // P matrix: 16 float
        float[] uniforms = new float[16];
        Pointer uniformData = JavaWebGPU.createFloatArrayPointer(uniforms);   // copy to native memory


        int offset = 0;
        setUniformMatrix(uniformData, offset, projectionMatrix);
        offset += 16*Float.BYTES;

        LibGPU.webGPU.wgpuQueueWriteBuffer(LibGPU.queue, uniformBuffer, 0, uniformData, uniformBufferSize);
    }

    private Pointer createBindGroupLayout() {
        // Define binding layout
        WGPUBindGroupLayoutEntry bindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(bindingLayout);
        bindingLayout.setBinding(0);
        bindingLayout.setVisibility(WGPUShaderStage.Vertex );
        bindingLayout.getBuffer().setType(WGPUBufferBindingType.Uniform);
        bindingLayout.getBuffer().setMinBindingSize(uniformBufferSize);
        bindingLayout.getBuffer().setHasDynamicOffset(0L);

        // Create a bind group layout
        WGPUBindGroupLayoutDescriptor bindGroupLayoutDesc = WGPUBindGroupLayoutDescriptor.createDirect();
        bindGroupLayoutDesc.setNextInChain();
        bindGroupLayoutDesc.setLabel("ShapeRenderer binding group layout");
        bindGroupLayoutDesc.setEntryCount(1);
        bindGroupLayoutDesc.setEntries(bindingLayout); //, texBindingLayout, samplerBindingLayout);
        return LibGPU.webGPU.wgpuDeviceCreateBindGroupLayout(LibGPU.device, bindGroupLayoutDesc);
    }


    private Pointer makeBindGroup() {
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
        bindGroupDesc.setEntryCount(1);
        bindGroupDesc.setEntries(binding);
        return LibGPU.webGPU.wgpuDeviceCreateBindGroup(LibGPU.device, bindGroupDesc);
    }

    private Pointer makePipelineLayout(Pointer bindGroupLayout) {
        long[] layouts = new long[1];
        layouts[0] = bindGroupLayout.address();
        Pointer layoutPtr = JavaWebGPU.createLongArrayPointer(layouts);

        // Create the pipeline layout to define the bind groups needed : 1 bind group
        WGPUPipelineLayoutDescriptor layoutDesc = WGPUPipelineLayoutDescriptor.createDirect();
        layoutDesc.setNextInChain();
        layoutDesc.setLabel("ShapeRenderer Pipeline Layout");
        layoutDesc.setBindGroupLayoutCount(1);
        layoutDesc.setBindGroupLayouts(layoutPtr);
        return LibGPU.webGPU.wgpuDeviceCreatePipelineLayout(LibGPU.device, layoutDesc);
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

    @Override
    public void dispose(){
        pipelines.dispose();
        webGPU.wgpuBufferRelease(vertexBuffer);
        webGPU.wgpuBufferRelease(indexBuffer);
        webGPU.wgpuBufferRelease(uniformBuffer);
        webGPU.wgpuBindGroupLayoutRelease(bindGroupLayout);
        //shader.dispose();
    }
}
