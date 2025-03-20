package com.monstrous.graphics.g2d;

import com.monstrous.LibGPU;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.ShaderProgram;
import com.monstrous.graphics.VertexAttribute;
import com.monstrous.graphics.VertexAttributes;
import com.monstrous.graphics.webgpu.*;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector2;
import com.monstrous.utils.Disposable;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.webgpu.*;
import jnr.ffi.Pointer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

// todo handle small rectangles e.g. 10x10

public class RoundedRectangleBatch implements Disposable {
    private WebGPU_JNI webGPU;
    private ShaderProgram specificShader;
    private int maxSprites;
    private boolean begun;
    private int vertexSize;
    private final FloatBuffer vertexData;     // float buffer view on byte buffer
    private final Pointer vertexDataPtr;      // Pointer wrapped around the byte buffer
    private int numRects;
    private final Color tint;
    private final Vector2 dropShadow;
    private Buffer vertexBuffer;
    private Buffer indexBuffer;
    private UniformBuffer uniformBuffer;
    private final BindGroupLayout bindGroupLayout;
    private VertexAttributes vertexAttributes;
    private VertexAttributes defaultVertexAttributes;
    private final PipelineLayout pipelineLayout;
    private PipelineSpecification pipelineSpec;
    private int uniformBufferSize;
    private final Matrix4 projectionMatrix;
    private RenderPass renderPass;
    private int vbOffset;
    private final Pipelines pipelines;
    private Pipeline prevPipeline;
    private boolean blendingEnabled;
    public int maxSpritesInBatch;
    public int renderCalls;


    public RoundedRectangleBatch() {
        this(1000); // default nr
    }

    public RoundedRectangleBatch(int maxSprites) {
        this(maxSprites, null);
    }

    public RoundedRectangleBatch(int maxSprites, ShaderProgram specificShader) {
        this.maxSprites = maxSprites;
        this.specificShader = specificShader;

        dropShadow = new Vector2();
        begun = false;
        webGPU = LibGPU.webGPU;

        vertexAttributes = new VertexAttributes();
        vertexAttributes.add(VertexAttribute.Usage.POSITION, "position",    WGPUVertexFormat.Float32x2, 0 );
        vertexAttributes.add(VertexAttribute.Usage.COLOR,   "color",        WGPUVertexFormat.Float32x4, 1 );
        vertexAttributes.add(VertexAttribute.Usage.GENERIC, "center",       WGPUVertexFormat.Float32x2, 2 );
        vertexAttributes.add(VertexAttribute.Usage.GENERIC, "size",         WGPUVertexFormat.Float32x2, 3 );
        vertexAttributes.add(VertexAttribute.Usage.GENERIC, "radius",       WGPUVertexFormat.Float32x2, 4 );
        vertexAttributes.add(VertexAttribute.Usage.GENERIC, "dropShadow",   WGPUVertexFormat.Float32x2, 5 );

        vertexAttributes.end();
        defaultVertexAttributes = vertexAttributes;

        vertexSize = vertexAttributes.getVertexSizeInBytes(); // bytes

        // allocate data buffers based on default vertex attributes which are assumed to be the worst case.
        // i.e. with setVertexAttributes() you can specify a subset
        createBuffers();
        fillIndexBuffer();

        ByteBuffer vertexBB = ByteBuffer.allocateDirect(maxSprites * 4 * vertexSize);
        vertexBB.order(ByteOrder.nativeOrder());  // important
        vertexData = vertexBB.asFloatBuffer();
        vertexDataPtr = Pointer.wrap(JavaWebGPU.getRuntime(), vertexBB);

        projectionMatrix = new Matrix4();
        projectionMatrix.setToOrtho(0f, LibGPU.graphics.getWidth(),  0, LibGPU.graphics.getHeight(), -1f, 1f);

        tint = new Color(Color.WHITE);

        bindGroupLayout = createBindGroupLayout();
        pipelineLayout = new PipelineLayout("RRBatch pipeline layout", bindGroupLayout);

        pipelines = new Pipelines();
        pipelineSpec = new PipelineSpecification(vertexAttributes, this.specificShader);
        pipelineSpec.shaderFilePath = "shaders/roundedRectangles.wgsl"; //Files.classpath("shaders/roundedRectangles.wgsl");
    }

    // the index buffer is fixed and only has to be filled on start-up
    private void fillIndexBuffer(){
        ByteBuffer bb = ByteBuffer.allocateDirect(maxSprites*6*Short.BYTES);
        bb.order(ByteOrder.nativeOrder());  // important
        ShortBuffer indexData = bb.asShortBuffer();
        for(int i = 0; i < maxSprites; i++){
            short vertexOffset = (short)(i * 4);
            // two triangles per sprite
            indexData.put(vertexOffset);
            indexData.put((short)(vertexOffset + 1));
            indexData.put((short)(vertexOffset + 2));

            indexData.put(vertexOffset);
            indexData.put((short)(vertexOffset + 2));
            indexData.put((short)(vertexOffset + 3));
        }
        indexData.flip();
        Pointer indexDataPtr = Pointer.wrap(JavaWebGPU.getRuntime(), bb);
        webGPU.wgpuQueueWriteBuffer(LibGPU.queue, indexBuffer.getHandle(), 0, indexDataPtr, (long) maxSprites *6*Short.BYTES);
    }


    public void setColor(float r, float g, float b, float a){
        tint.set(r,g,b,a);
    }

    public void setColor(Color color){
        tint.set(color);
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
        vertexData.clear();
        vertexAttributes = defaultVertexAttributes;
        vertexSize = vertexAttributes.getVertexSizeInBytes(); // bytes
        maxSpritesInBatch = 0;
        renderCalls = 0;

        prevPipeline = null;

        // set default state
        tint.set(Color.WHITE);
        blendingEnabled = true;
        pipelineSpec.enableBlending();
        pipelineSpec.disableDepth();
        pipelineSpec.shader = specificShader;
        pipelineSpec.vertexAttributes = vertexAttributes;
        pipelineSpec.numSamples =  LibGPU.app.configuration.numSamples;
        setPipeline();
        setUniforms();
    }

    public void flush() {
        if(numRects == 0)
            return;
        if(numRects > maxSpritesInBatch)
            maxSpritesInBatch = numRects;
        renderCalls++;

        // Add number of vertices to the GPU's vertex buffer
        //
        int numBytes = numRects * 4 * vertexSize;

        // append new vertex data to GPU vertex buffer
        webGPU.wgpuQueueWriteBuffer(LibGPU.queue, vertexBuffer.getHandle(), vbOffset, vertexDataPtr, numBytes);

        // bind uniforms
        BindGroup bg = makeBindGroup(bindGroupLayout, uniformBuffer.getBuffer());

        // Set vertex buffer while encoding the render pass
        // use an offset to set the vertex buffer for this batch
        renderPass.setVertexBuffer( 0, vertexBuffer.getHandle(), vbOffset, numBytes);
        renderPass.setIndexBuffer( indexBuffer.getHandle(), WGPUIndexFormat.Uint16, 0, (long)numRects*6*Short.BYTES);

        renderPass.setBindGroup( 0, bg.getHandle(), 0, JavaWebGPU.createNullPointer());


        renderPass.drawIndexed( numRects*6, 1, 0, 0, 0);

        bg.dispose();

        vbOffset += numBytes;

        vertexData.clear(); // reset fill position for next batch
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
        Pipeline pipeline = pipelines.findPipeline( pipelineLayout.getHandle(), pipelineSpec);
        if (pipeline != prevPipeline) { // avoid unneeded switches
            renderPass.setPipeline(pipeline.getPipeline());
            prevPipeline = pipeline;
        }
    }

    public void setShader(ShaderProgram shaderProgram){
        flush();
        if(shaderProgram == null)
            pipelineSpec.shader = specificShader;
        else {
            pipelineSpec.shader = shaderProgram;
        }
        setPipeline();
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

    public void setDropShadow( float x, float y ){
        dropShadow.set(x, y);
    }


    public void draw ( float x, float y, float width, float height, float radius) {
        if (!begun)
            throw new RuntimeException("RRBatch: Must call begin() before draw().");

        if(numRects == maxSprites)
            throw new RuntimeException("RRBatch: Too many sprites.");

        addRect(tint, x, y, width, height, radius);
        numRects++;
    }



    private void addRect(Color color, float x, float y, float w, float h, float radius) {
        float cx = x + w/2;
        float cy = LibGPU.graphics.getHeight() - (y+h/2);

        addVertex(x, y, color, cx, cy, w, h, radius, dropShadow);
        addVertex(x, y+h, color, cx, cy, w, h, radius, dropShadow);
        addVertex(x+w, y+h, color, cx, cy, w, h, radius, dropShadow);
        addVertex(x+w, y, color, cx, cy, w, h, radius, dropShadow);
    }

    private void addVertex(float x, float y, Color tint, float cx, float cy, float w, float h, float radius, Vector2 dropShadow) {
        // vertex position
        vertexData.put(x);
        vertexData.put(y);

        // color
        vertexData.put(tint.r);
        vertexData.put(tint.g);
        vertexData.put(tint.b);
        vertexData.put(tint.a);

        // centre
        vertexData.put(cx);
        vertexData.put(cy);

        // size
        vertexData.put(w);
        vertexData.put(h);

        // radius
        vertexData.put(radius);
        vertexData.put(0f);

        // drop shadow offset
        vertexData.put(dropShadow.x);
        vertexData.put(dropShadow.y);
    }

//    private void setUniformMatrix(Pointer data, int offset, Matrix4 mat) {
//        for (int i = 0; i < 16; i++) {
//            data.putFloat(offset + i * Float.BYTES, mat.val[i]);
//        }
//    }

    private void createBuffers() {

        long indexSize = (long) maxSprites * 6 * Short.BYTES;
        indexSize = (indexSize + 3) & ~3; // round up to the next multiple of 4

        // Create vertex buffer and index buffer
        vertexBuffer = new Buffer("Vertex buffer", WGPUBufferUsage.CopyDst | WGPUBufferUsage.Vertex, (long) maxSprites * 4 * vertexSize);
        indexBuffer = new Buffer("Index buffer", WGPUBufferUsage.CopyDst | WGPUBufferUsage.Index, indexSize);

        // Create uniform buffer
        uniformBufferSize = 16 * Float.BYTES;
        uniformBuffer = new UniformBuffer(uniformBufferSize,WGPUBufferUsage.CopyDst |WGPUBufferUsage.Uniform  );
    }

    private void setUniforms(){
        uniformBuffer.beginFill();
        uniformBuffer.append(projectionMatrix);
        uniformBuffer.endFill();
    }

    private BindGroupLayout createBindGroupLayout() {
        BindGroupLayout layout = new BindGroupLayout("RRBatch bind group layout");
        layout.begin();
        layout.addBuffer(0, WGPUShaderStage.Vertex, WGPUBufferBindingType.Uniform, uniformBufferSize, false);
        layout.end();
        return layout;
    }


    private BindGroup makeBindGroup(BindGroupLayout bindGroupLayout, Buffer uniformBuffer) {
        BindGroup bg = new BindGroup(bindGroupLayout);
        bg.begin();
        bg.addBuffer(0, uniformBuffer);
        bg.end();
        return bg;
    }


    @Override
    public void dispose(){
        pipelines.dispose();
        vertexBuffer.dispose();
        indexBuffer.dispose();
        uniformBuffer.dispose();
        bindGroupLayout.dispose();
        pipelineLayout.dispose();
    }
}
