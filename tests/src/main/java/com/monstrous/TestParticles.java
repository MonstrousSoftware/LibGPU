package com.monstrous;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.ShaderProgram;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.webgpu.*;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.utils.ScreenUtils;
import com.monstrous.webgpu.*;
import jnr.ffi.Pointer;

// further ideas:
// emitter on mouse button

public class TestParticles extends ApplicationAdapter {

    private SpriteBatch batch;
    private BitmapFont font;
    private long startTime;
    private int frames;
    private String fps;
    private Texture particleTexture;

    @Override
    public void create() {
        startTime = System.nanoTime();
        frames = 0;
        fps = "";

        particleTexture = new Texture("textures/particle.png", false);


        batch = new SpriteBatch();
        font = new BitmapFont();

        initParticles();
    }

    @Override
    public void render(){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){
            LibGPU.app.exit();
            return;
        }

        if(LibGPU.input.isButtonPressed(Input.Buttons.LEFT)){
            emitParticle( LibGPU.input.getX(), LibGPU.graphics.getHeight()-LibGPU.input.getY(), 100, Color.GREEN_YELLOW, 2.0f, 4.0f);
        }
        if(LibGPU.input.isButtonPressed(Input.Buttons.RIGHT)){
            emitParticle( LibGPU.input.getX(), LibGPU.graphics.getHeight()-LibGPU.input.getY(), 500, Color.RED, 0.05f, 2.0f);
        }

        ScreenUtils.clear(Color.BLACK);
        updateParticles();
        renderParticles();
        batch.begin();
        font.draw(batch, "Particles: "+numParticles, 10, 70);
        font.draw(batch, fps, 10, 50);
        batch.end();

        // At the end of the frame
        if (System.nanoTime() - startTime > 1000000000) {
            fps = "SpriteBatch : fps: " + frames; // + " GPU: "+LibGPU.app.getAverageGPUtime()+" microseconds"  );
            frames = 0;
            startTime = System.nanoTime();
        }
        frames++;
    }

    @Override
    public void dispose(){
        // cleanup
        batch.dispose();
    }

    @Override
    public void resize(int width, int height) {

        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        screenSize = new Vector3(width, height, 100);
    }

    private UniformBuffer uniformBuffer;
    private Buffer[] particleBuffers;
    private int flip;
    private Pointer floatData;
    private int numParticles;
    private int maxParticles;
    private int writeIndex;
    private int structSize;
    private Pointer computePipeline;
    private ShaderProgram shader;
    private Vector3 particleScale;
    private Vector3 screenSize;

    private void initParticles(){

        maxParticles = 8000;
        numParticles = 0;
        writeIndex = 0;
        float speed = 9f;
        float quadSize = 0.06f;
        Color color = new Color(Color.YELLOW);

        structSize = 16 * Float.BYTES;
        float aspectRatio = (float)LibGPU.graphics.getWidth()/LibGPU.graphics.getHeight();
        particleScale = new Vector3(quadSize, quadSize*aspectRatio, quadSize);
        screenSize = new Vector3(LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight(), 100);


        uniformBuffer = new UniformBuffer((16+4+4+1)*Float.BYTES,WGPUBufferUsage.CopyDst | WGPUBufferUsage.Uniform  );

        particleBuffers = new Buffer[2];
        int usage = WGPUBufferUsage.Storage | WGPUBufferUsage.CopyDst | WGPUBufferUsage.CopySrc;
        particleBuffers[0] = new Buffer("particles", usage , maxParticles * structSize);
        particleBuffers[1] = new Buffer("next particles", usage , maxParticles * structSize);

        flip = 0;


        // working buffer in native memory to use as input to WriteBuffer
        floatData = JavaWebGPU.createDirectPointer(structSize);


        int W = LibGPU.graphics.getWidth();
        int H = LibGPU.graphics.getHeight();

        for(int i = 0; i < 200 ; i++){
            emitParticle((float) (Math.random()*W),(float)(Math.random()*H),speed,  color, 1.0f, 2.0f);
        }

        shader = new ShaderProgram(Files.internal("shaders/particles.wgsl"));

        BindGroupLayout bindGroupLayout = createBindGroupLayout();
        PipelineLayout pipelineLayout = new PipelineLayout("particle compute pipeline", bindGroupLayout);
        computePipeline = makeComputePipeline(shader, pipelineLayout);
    }

    private void emitParticle(float x, float y, float speed, Color color, float scale, float age){
        if(writeIndex >= maxParticles)
            writeIndex = 0; // start overwriting oldest particles

        Vector3 pos = new Vector3();
        Vector3 vel = new Vector3();

        pos.set(x, y,0);
        float angle = (float)(2*Math.PI * Math.random());
        vel.set( speed*(float)(Math.cos(angle)),speed*((float)Math.sin(angle)), 0);


        int off = 0;
        floatData.putFloat(off++*Float.BYTES, pos.x);
        floatData.putFloat(off++*Float.BYTES, pos.y);
        floatData.putFloat(off++*Float.BYTES, pos.z);
        floatData.putFloat(off++*Float.BYTES, 1.0f);

        floatData.putFloat(off++*Float.BYTES, vel.x);
        floatData.putFloat(off++*Float.BYTES, vel.y);
        floatData.putFloat(off++*Float.BYTES, vel.z);
        floatData.putFloat(off++*Float.BYTES, 0f);

        floatData.putFloat(off++*Float.BYTES, color.r);
        floatData.putFloat(off++*Float.BYTES, color.g);
        floatData.putFloat(off++*Float.BYTES, color.b);
        floatData.putFloat(off++*Float.BYTES, color.a);

        floatData.putFloat(off++*Float.BYTES, age);
        floatData.putFloat(off++*Float.BYTES, scale);

        // pad to a multiple of 16
        floatData.putFloat(off++*Float.BYTES, 0);
        floatData.putFloat(off++*Float.BYTES, 0);

        particleBuffers[flip].write(writeIndex*structSize, floatData, structSize);
        writeIndex++;
        if(writeIndex > numParticles)
            numParticles = writeIndex;
        // should we do one big write instead?
    }



    private void renderParticles(){

        Matrix4 projectionMatrix = new Matrix4();
        projectionMatrix.setToOrtho(0f, LibGPU.graphics.getWidth(), 0f, LibGPU.graphics.getHeight(), -1f, 1f);
        setUniforms(batch.getProjectionMatrix(), screenSize, particleScale, LibGPU.graphics.getDeltaTime());   // PV matrix?

        RenderPass renderPass = RenderPassBuilder.create(null, LibGPU.app.configuration.numSamples);

        BindGroupLayout bindGroupLayout = createBindGroupLayout();
        BindGroup bg = makeBindGroup(bindGroupLayout, uniformBuffer, particleBuffers[flip], particleBuffers[1-flip], particleTexture);

        PipelineLayout pipelineLayout = new PipelineLayout("particle render pipeline", bindGroupLayout);

        PipelineSpecification pipeSpec = new PipelineSpecification();
        pipeSpec.numSamples =  LibGPU.app.configuration.numSamples;
        pipeSpec.topology = WGPUPrimitiveTopology.TriangleList;
        pipeSpec.shader = shader;
        pipeSpec.blendSrcColor = WGPUBlendFactor.SrcAlpha;
        pipeSpec.blendDstColor = WGPUBlendFactor.One;
        pipeSpec.blendOpColor = WGPUBlendOperation.Add;
        pipeSpec.blendSrcAlpha = WGPUBlendFactor.Src;
        pipeSpec.blendDstAlpha = WGPUBlendFactor.One;
        pipeSpec.blendOpAlpha = WGPUBlendOperation.Add;
        pipeSpec.disableDepthTest();    // disable depth test to avoid transparent pixels appearing as black

        Pipeline pipeline = new Pipeline(pipelineLayout.getHandle(), pipeSpec);

        renderPass.setPipeline(pipeline.getHandle());
        renderPass.setBindGroup( 0, bg.getHandle(), 0, JavaWebGPU.createNullPointer());


        renderPass.draw( 6* numParticles ); // 6 vertices per particle
        bg.dispose();
        renderPass.end();

    }

    private void updateParticles(){
        setUniforms(batch.getProjectionMatrix(), screenSize, particleScale, 0.01f); //LibGPU.graphics.getDeltaTime());   // PV matrix?

        // create a command encoder
        CommandEncoder encoder = new CommandEncoder(LibGPU.device);
        ComputePass pass = encoder.beginComputePass();

        // set pipeline
        pass.setPipeline(computePipeline);

        BindGroupLayout bindGroupLayout = createBindGroupLayout();
        BindGroup bindGroup = makeBindGroup(bindGroupLayout, uniformBuffer, particleBuffers[flip], particleBuffers[1-flip], particleTexture);
        flip = 1 - flip;
        // set bind group
        pass.setBindGroup(0, bindGroup);

        int invocationCountX = numParticles;
        int workgroupSizePerDim = 8;
        int workgroupCountX = (invocationCountX + workgroupSizePerDim - 1) / workgroupSizePerDim;

        // dispatch workgroups
        pass.dispatchWorkGroups(workgroupCountX, 1, 1);
        bindGroup.dispose();
        bindGroupLayout.dispose();

        pass.end();

        CommandBuffer commandBuffer = encoder.finish();
        encoder.dispose();

        // feed the command buffer to the queue
        LibGPU.queue.submit(commandBuffer);
        commandBuffer.dispose();
    }


    private void setUniforms(Matrix4 projectionMatrix, Vector3 enclosureSize, Vector3 scale, float deltaTime){
        uniformBuffer.beginFill();
        uniformBuffer.append(projectionMatrix);
        uniformBuffer.append(enclosureSize);
        uniformBuffer.append(scale);
        uniformBuffer.append(deltaTime);
        uniformBuffer.endFill();
    }

    private BindGroupLayout createBindGroupLayout() {
        BindGroupLayout layout = new BindGroupLayout("particle render bind group layout");
        layout.begin();
        layout.addBuffer(0, WGPUShaderStage.Vertex|WGPUShaderStage.Compute, WGPUBufferBindingType.Uniform, uniformBuffer.getSize(), false);
        // note: vertex shader can only be bound to READ-ONLY storage buffers
        layout.addBuffer(1, WGPUShaderStage.Vertex|WGPUShaderStage.Compute, WGPUBufferBindingType.ReadOnlyStorage, particleBuffers[0].getSize(), false);
        // we ping-pong between 2 buffers for the compute shader
        layout.addBuffer(2, WGPUShaderStage.Compute, WGPUBufferBindingType.Storage, particleBuffers[0].getSize(), false);
        layout.addTexture(3, WGPUShaderStage.Fragment, WGPUTextureSampleType.Float, WGPUTextureViewDimension._2D, false);
        layout.addSampler(4,  WGPUShaderStage.Fragment, WGPUSamplerBindingType.Filtering);
        layout.end();
        return layout;
    }


    private BindGroup makeBindGroup(BindGroupLayout bindGroupLayout, Buffer uniformBuffer, Buffer in, Buffer out, Texture texture) {
        BindGroup bg = new BindGroup(bindGroupLayout);
        bg.begin();
        bg.addBuffer(0, uniformBuffer);
        bg.addBuffer(1, in);
        bg.addBuffer(2, out);
        bg.addTexture(3, texture.getTextureView());
        bg.addSampler(4, texture.getSampler());
        bg.end();
        return bg;
    }

    private Pointer makeComputePipeline(ShaderProgram shader, PipelineLayout pipelineLayout){

        WGPUComputePipelineDescriptor pipelineDescriptor = WGPUComputePipelineDescriptor.createDirect();
        pipelineDescriptor.setNextInChain();
        pipelineDescriptor.getCompute().setConstantCount(0);
        pipelineDescriptor.getCompute().setConstants();
        pipelineDescriptor.getCompute().setEntryPoint("updateParticles");
        pipelineDescriptor.getCompute().setModule(shader.getHandle());
        pipelineDescriptor.setLayout(pipelineLayout.getHandle());

        return LibGPU.webGPU.wgpuDeviceCreateComputePipeline(LibGPU.device.getHandle(), pipelineDescriptor);
    }

}
