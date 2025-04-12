package com.monstrous;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.ShaderProgram;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.webgpu.*;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.utils.ScreenUtils;
import com.monstrous.webgpu.*;
import jnr.ffi.Pointer;

// further ideas:
// convert dots to quads to make them more visible and to add a blended texture
// emitter on mouse button
// use delta time
// make particles wrap around or bounce on screen edges
// 3d view
// aging, color change
public class TestParticles extends ApplicationAdapter {

    private SpriteBatch batch;
    private BitmapFont font;
    private long startTime;
    private int frames;
    private String fps;


    @Override
    public void create() {
        startTime = System.nanoTime();
        frames = 0;
        fps = "";

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
    }

    private UniformBuffer uniformBuffer;
    private Buffer[] particleBuffers;
    private int flip;
    private Pointer floatData;
    private int numParticles;
    private int structSize;
    private Pointer computePipeline;
    private ShaderProgram shader;

    private void initParticles(){

        numParticles = 50000;
        structSize = 12 * Float.BYTES;


        uniformBuffer = new UniformBuffer(16*Float.BYTES,WGPUBufferUsage.CopyDst | WGPUBufferUsage.Uniform  );

        particleBuffers = new Buffer[2];
        int usage = WGPUBufferUsage.Storage | WGPUBufferUsage.CopyDst | WGPUBufferUsage.CopySrc;
        particleBuffers[0] = new Buffer("particles", usage , numParticles * structSize);
        particleBuffers[1] = new Buffer("next particles", usage , numParticles * structSize);

        flip = 0;


        // working buffer in native memory to use as input to WriteBuffer
        floatData = JavaWebGPU.createDirectPointer(structSize);
        Vector3 pos = new Vector3();
        Vector3 vel = new Vector3();
        Color color = new Color(Color.WHITE);
        for(int i = 0; i < numParticles; i++){
            pos.set(i * LibGPU.graphics.getWidth()/numParticles, LibGPU.graphics.getHeight()/2, 0);
            vel.set( 5f*(float)Math.random()-2.5f,5f*(float)Math.random()-2.5f, 0);

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

            particleBuffers[flip].write(i*structSize, floatData, structSize);
            // should we do one big write instead?
        }

        shader = new ShaderProgram(Files.internal("shaders/particles.wgsl"));

        BindGroupLayout bindGroupLayout = createBindGroupLayout();
        PipelineLayout pipelineLayout = new PipelineLayout("particle compute pipeline", bindGroupLayout);
        computePipeline = makeComputePipeline(shader, pipelineLayout);
    }


    private void renderParticles(){
        setUniforms(batch.getProjectionMatrix());   // PV matrix?

        RenderPass renderPass = RenderPassBuilder.create(null, LibGPU.app.configuration.numSamples);

        BindGroupLayout bindGroupLayout = createBindGroupLayout();
        BindGroup bg = makeBindGroup(bindGroupLayout, uniformBuffer, particleBuffers[flip], particleBuffers[1-flip]);

        PipelineLayout pipelineLayout = new PipelineLayout("particle render pipeline", bindGroupLayout);

        PipelineSpecification pipeSpec = new PipelineSpecification();
        pipeSpec.numSamples =  LibGPU.app.configuration.numSamples;
        pipeSpec.topology = WGPUPrimitiveTopology.PointList;
        pipeSpec.shader = shader;

        Pipeline pipeline = new Pipeline(pipelineLayout.getHandle(), pipeSpec);

        renderPass.setPipeline(pipeline.getHandle());
        renderPass.setBindGroup( 0, bg.getHandle(), 0, JavaWebGPU.createNullPointer());


        renderPass.draw( numParticles );
        bg.dispose();
        renderPass.end();

    }

    private void updateParticles(){

        // create a command encoder
        CommandEncoder encoder = new CommandEncoder(LibGPU.device);
        ComputePass pass = encoder.beginComputePass();

        // set pipeline
        pass.setPipeline(computePipeline);

        BindGroupLayout bindGroupLayout = createBindGroupLayout();
        BindGroup bindGroup = makeBindGroup(bindGroupLayout, uniformBuffer, particleBuffers[flip], particleBuffers[1-flip]);
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


    private void setUniforms(Matrix4 projectionMatrix){
        uniformBuffer.beginFill();
        uniformBuffer.append(projectionMatrix);
        uniformBuffer.endFill();
    }

    private BindGroupLayout createBindGroupLayout() {
        BindGroupLayout layout = new BindGroupLayout("particle render bind group layout");
        layout.begin();
        layout.addBuffer(0, WGPUShaderStage.Vertex, WGPUBufferBindingType.Uniform, uniformBuffer.getSize(), false);
        // note: vertex shader can only be bound to READ-ONLY storage buffers
        layout.addBuffer(1, WGPUShaderStage.Vertex|WGPUShaderStage.Compute, WGPUBufferBindingType.ReadOnlyStorage, particleBuffers[0].getSize(), false);
        // we ping-pong between 2 buffers for the compute shader
        layout.addBuffer(2, WGPUShaderStage.Compute, WGPUBufferBindingType.Storage, particleBuffers[0].getSize(), false);
        layout.end();
        return layout;
    }


    private BindGroup makeBindGroup(BindGroupLayout bindGroupLayout, Buffer uniformBuffer, Buffer in, Buffer out) {
        BindGroup bg = new BindGroup(bindGroupLayout);
        bg.begin();
        bg.addBuffer(0, uniformBuffer);
        bg.addBuffer(1, in);
        bg.addBuffer(2, out);
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
