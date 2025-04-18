package com.monstrous.graphics.g2d;

import com.monstrous.LibGPU;
import com.monstrous.graphics.*;
import com.monstrous.graphics.webgpu.*;
import com.monstrous.math.Matrix4;
import com.monstrous.utils.Disposable;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.webgpu.*;
import jnr.ffi.Pointer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Class to render textured rectangles in batches.
 */
public class SpriteBatch implements Disposable {

    private final static String DEFAULT_SHADER = "shaders/sprite.wgsl";

    private WebGPU_JNI webGPU;
    private ShaderProgram specificShader;
    private int maxSprites;
    private boolean begun;
    private int vertexSize;
    private final FloatBuffer vertexData;     // float buffer view on byte buffer
    private final Pointer vertexDataPtr;      // Pointer wrapped around the byte buffer
    private int numRects;
    private final Color tint;
    private Buffer vertexBuffer;
    private Buffer indexBuffer;
    private UniformBuffer uniformBuffer;
    private final BindGroupLayout bindGroupLayout;
    private VertexAttributes vertexAttributes;
    private VertexAttributes defaultVertexAttributes;
    private final PipelineLayout pipelineLayout;
    private PipelineSpecification pipelineSpec;
    private int uniformBufferSize;
    private Texture texture;
    private final Matrix4 projectionMatrix;
    private RenderPass renderPass;
    private int vbOffset;
    private final Pipelines pipelines;
    private Pipeline prevPipeline;
    private boolean blendingEnabled;
    public int maxSpritesInBatch;
    public int renderCalls;
    public int pipelineCount;


    public SpriteBatch() {
        this(1000); // default nr
    }

    public SpriteBatch(int maxSprites) {
        this(maxSprites, null);
    }

    /** Create a SpriteBatch.
     *
     * @param maxSprites        maximum number of sprite to be supported (default is 1000)
     * @param specificShader    specific ShaderProgram to use, must be compatible with "sprite.wgsl". Leave null to use the default shader.
     */
    public SpriteBatch(int maxSprites, ShaderProgram specificShader) {
        this.maxSprites = maxSprites;
        this.specificShader = specificShader;

        begun = false;
        webGPU = LibGPU.webGPU;

        vertexAttributes = new VertexAttributes(VertexAttribute.Usage.POSITION_2D|VertexAttribute.Usage.TEXTURE_COORDINATE|VertexAttribute.Usage.COLOR_PACKED);
        defaultVertexAttributes = vertexAttributes;

        // vertex: x, y, u, v, rgba
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
        projectionMatrix.setToOrtho(0f, LibGPU.graphics.getWidth(), 0f, LibGPU.graphics.getHeight(), -1f, 1f);
        setUniforms();

        tint = new Color(Color.WHITE);

        bindGroupLayout = createBindGroupLayout();
        pipelineLayout = new PipelineLayout("SpriteBatch pipeline layout", bindGroupLayout);

        pipelines = new Pipelines();
        pipelineSpec = new PipelineSpecification(vertexAttributes, this.specificShader);
        pipelineSpec.name = "SpriteBatch pipeline";
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
        webGPU.wgpuQueueWriteBuffer(LibGPU.queue.getHandle(), indexBuffer.getHandle(), 0, indexDataPtr, (long) maxSprites *6*Short.BYTES);
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
        flush();
        blendingEnabled = true;
        pipelineSpec.enableBlending();
        setPipeline();
    }

    public void disableBlending(){
        if(!blendingEnabled)
            return;
        flush();
        blendingEnabled = false;
        pipelineSpec.disableBlending();
        setPipeline();
    }

    public void setVertexAttributes(VertexAttributes vattr){
        if (!begun) // catch incorrect usage
            throw new RuntimeException("Call begin() before calling setVertexAttributes().");
        flush();
        vertexAttributes = vattr;
        vertexSize = vertexAttributes.getVertexSizeInBytes(); // bytes
        pipelineSpec.vertexAttributes = vattr;
        pipelineSpec.shader = null;     // force recompile of shader
        setPipeline();
    }

    public void begin(){
        begin(null);
    }

    public void begin(Color clearColor) {

        renderPass = RenderPassBuilder.create(clearColor, LibGPU.app.configuration.numSamples);
        //'renderPass = RenderPassBuilder.create("SpriteBatch pass", clearColor,  null, null, null, LibGPU.app.configuration.numSamples, RenderPassType.NO_DEPTH);

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
        pipelineSpec.disableDepthTest();
        pipelineSpec.shader = specificShader;
        if(specificShader == null)
            pipelineSpec.shaderFilePath = DEFAULT_SHADER;
        pipelineSpec.vertexAttributes = vertexAttributes;
        pipelineSpec.numSamples =  LibGPU.app.configuration.numSamples;

        setUniforms();

        // for testing
        //wgpu.RenderPassEncoderSetViewport(renderPass, 100, 500, 500, 200, 0, 1);
    }

    public void flush() {
        if(numRects == 0)
            return;
        if(numRects > maxSpritesInBatch)
            maxSpritesInBatch = numRects;
        renderCalls++;

        setPipeline();

        // Add number of vertices to the GPU's vertex buffer
        //
        int numBytes = numRects * 4 * vertexSize;

        // append new vertex data to GPU vertex buffer
        LibGPU.queue.writeBuffer(vertexBuffer, vbOffset, vertexDataPtr, numBytes);
        //webGPU.wgpuQueueWriteBuffer(LibGPU.queue, vertexBuffer.getHandle(), vbOffset, vertexDataPtr, numBytes);

        // bind texture
        BindGroup bg = makeBindGroup(bindGroupLayout, uniformBuffer, texture);

        // Set vertex buffer while encoding the render pass
        // use an offset to set the vertex buffer for this batch
        renderPass.setVertexBuffer( 0, vertexBuffer.getHandle(), vbOffset, numBytes);
        renderPass.setIndexBuffer( indexBuffer.getHandle(), WGPUIndexFormat.Uint16, 0, (long)numRects*6*Short.BYTES);

        renderPass.setBindGroup( 0, bg.getHandle(), 0, JavaWebGPU.createNullPointer());

        //renderPass.setScissorRect( 20, 20, 500, 500);

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
        pipelineCount = pipelines.size();   // statistics
    }

    // create or reuse pipeline on demand to match the pipeline spec
    private void setPipeline() {
        Pipeline pipeline = pipelines.findPipeline( pipelineLayout.getHandle(), pipelineSpec);
        if (pipeline != prevPipeline) { // avoid unneeded switches
            renderPass.setPipeline(pipeline.getHandle());
            prevPipeline = pipeline;
        }
    }

    public void setShader(ShaderProgram shaderProgram) {
        flush();
        if (shaderProgram == null) {
            pipelineSpec.shader = specificShader;
            if (specificShader == null)
                pipelineSpec.shaderFilePath = DEFAULT_SHADER;
            pipelineSpec.recalcHash();
        }
        else {
            pipelineSpec.shader = shaderProgram;
            pipelineSpec.shaderFilePath = shaderProgram.getName();
            pipelineSpec.recalcHash();
        }

        setPipeline();  // probably not needed because flush() will do it again
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

    public void draw(Texture texture, float x, float y) {
        draw(texture, x, y, texture.getWidth(), texture.getHeight());
    }

    public void draw(Texture texture, float x, float y, float w, float h){
        this.draw(texture, x, y, w, h, 0f, 1f, 1f, 0f);
    }

    public void draw(TextureRegion region, float x, float y){
        // note: v2 is top of glyph, v the bottom
        this.draw(region.texture, x, y, region.regionWidth, region.regionHeight, region.u, region.v2, region.u2, region.v  );
    }

    public void draw(TextureRegion region, float x, float y, float w, float h){
        this.draw(region.texture, x, y, w, h, region.u, region.v, region.u2, region.v2  );
    }


    public void draw (Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2) {
        if (!begun)
            throw new RuntimeException("SpriteBatch: Must call begin() before draw().");

        if(numRects == maxSprites)
            throw new RuntimeException("SpriteBatch: Too many sprites.");

        if(texture != this.texture) { // changing texture, need to flush what we have so far
            flush();
            this.texture = texture;
        }
        addRect(x, y, width, height, u, v, u2, v2);
        numRects++;
    }

    // used by Sprite class
    public void draw(Texture texture, float[] vertices){
        if(vertices.length != 20)
            throw new IllegalArgumentException("SpriteBatch.draw: vertices must have length 20");
        if (!begun)
            throw new RuntimeException("SpriteBatch: Must call begin() before draw().");

        if(numRects == maxSprites)
            throw new RuntimeException("SpriteBatch: Too many sprites.");

        if(texture != this.texture) { // changing texture, need to flush what we have so far
            flush();
            this.texture = texture;
        }
        for(int i = 0; i < vertices.length; i++){
            vertexData.put(vertices[i]);
        }
        numRects++;
    }


    private void addRect(float x, float y, float w, float h, float u, float v, float u2, float v2) {
        boolean hasColor = vertexAttributes.hasUsage(VertexAttribute.Usage.COLOR_PACKED);
        boolean hasUV = vertexAttributes.hasUsage(VertexAttribute.Usage.TEXTURE_COORDINATE);
        float col = tint.toFloatBits();

        vertexData.put(x);
        vertexData.put(y);
        if(hasColor) {
            vertexData.put(col);
        }
        if(hasUV) {
            vertexData.put(u);
            vertexData.put(v);
        }

        vertexData.put(x);
        vertexData.put(y+h);
        if(hasColor) {
            vertexData.put(col);
        }
        if(hasUV) {
            vertexData.put(u);
            vertexData.put(v2);
        }


        vertexData.put(x+w);
        vertexData.put(y+h);
        if(hasColor) {
            vertexData.put(col);
        }
        if(hasUV) {
            vertexData.put(u2);
            vertexData.put(v2);
        }


        vertexData.put(x+w);
        vertexData.put(y);
        if(hasColor) {
            vertexData.put(col);
        }
        if(hasUV) {
            vertexData.put(u2);
            vertexData.put(v);
        }

    }



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
        BindGroupLayout layout = new BindGroupLayout("SpriteBatch bind group layout");
        layout.begin();
        layout.addBuffer(0, WGPUShaderStage.Vertex, WGPUBufferBindingType.Uniform, uniformBufferSize, false);
        layout.addTexture(1, WGPUShaderStage.Fragment, WGPUTextureSampleType.Float, WGPUTextureViewDimension._2D, false);
        layout.addSampler(2, WGPUShaderStage.Fragment, WGPUSamplerBindingType.Filtering );

        layout.end();
        return layout;
    }


    private BindGroup makeBindGroup(BindGroupLayout bindGroupLayout, Buffer uniformBuffer, Texture texture) {
        BindGroup bg = new BindGroup(bindGroupLayout);
        bg.begin();
        bg.addBuffer(0, uniformBuffer);
        bg.addTexture(1, texture.getTextureView());
        bg.addSampler(2, texture.getSampler());
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
