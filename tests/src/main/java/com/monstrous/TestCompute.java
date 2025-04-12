package com.monstrous;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.ShaderProgram;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.webgpu.*;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.webgpu.*;
import jnr.ffi.Pointer;

/**
 * Demonstration of using a compute shader.
 * Follows example from https://eliemichel.github.io/LearnWebGPU/basic-compute/compute-pipeline.html#
 * Uses some comfort classes to encapsulate WebGPU concepts.
 */

public class TestCompute extends ApplicationAdapter {

    private static final int BUFFER_SIZE = 64*Float.BYTES;    // bytes

    private SpriteBatch batch;
    private BitmapFont font;

    private WebGPU_JNI webGPU;
    private Pointer pipeline;
    float[] inputData = new float[BUFFER_SIZE/Float.BYTES];
    float[] outputData = new float[BUFFER_SIZE/Float.BYTES];


    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        webGPU = LibGPU.webGPU;

        onCompute();
    }

    private void onCompute() {

        // Create input and output buffers
        Buffer inputBuffer = new Buffer("Input storage buffer",WGPUBufferUsage.CopyDst | WGPUBufferUsage.Storage, BUFFER_SIZE );
        Buffer outputBuffer = new Buffer("Output storage buffer", WGPUBufferUsage.CopySrc | WGPUBufferUsage.Storage, BUFFER_SIZE );

        // Create an intermediary buffer to which we copy the output and that can be
        // used for reading into the CPU memory (because Storage is incompatible with MapRead).
        Buffer mapBuffer = new Buffer("Map buffer", WGPUBufferUsage.CopyDst | WGPUBufferUsage.MapRead, BUFFER_SIZE );

        // make a pipeline
        BindGroupLayout bindGroupLayout = makeBindGroupLayout();
        BindGroup bindGroup = makeBindGroup(bindGroupLayout, inputBuffer, outputBuffer);
        ShaderProgram shader = new ShaderProgram(Files.internal("shaders/compute.wgsl")); // from assets folder
        PipelineLayout pipelineLayout = new PipelineLayout("compute pipeline layout", bindGroupLayout);
        pipeline = makeComputePipeline(shader, pipelineLayout);

        compute(bindGroup, inputBuffer, outputBuffer, mapBuffer);

        // cleanup
        webGPU.wgpuComputePipelineRelease(pipeline);
        pipelineLayout.dispose();
        shader.dispose();
        bindGroup.dispose();
        bindGroupLayout.dispose();
        inputBuffer.dispose();
        outputBuffer.dispose();
        mapBuffer.dispose();
    }


    private BindGroupLayout makeBindGroupLayout(){
        BindGroupLayout layout = new BindGroupLayout();
        layout.begin();
        layout.addBuffer(0, WGPUShaderStage.Compute, WGPUBufferBindingType.ReadOnlyStorage, BUFFER_SIZE, false);// input buffer
        layout.addBuffer(1, WGPUShaderStage.Compute, WGPUBufferBindingType.Storage, BUFFER_SIZE, false);// output buffer
        layout.end();
        return layout;
    }

    private BindGroup makeBindGroup(BindGroupLayout bindGroupLayout, Buffer inputBuffer, Buffer outputBuffer){
        BindGroup bg = new BindGroup(bindGroupLayout);
        bg.begin();
        bg.addBuffer(0, inputBuffer);
        bg.addBuffer(1, outputBuffer);
        bg.end();
        return bg;
    }




    private Pointer makeComputePipeline(ShaderProgram shader, PipelineLayout pipelineLayout){

        WGPUComputePipelineDescriptor pipelineDescriptor = WGPUComputePipelineDescriptor.createDirect();
        pipelineDescriptor.setNextInChain();
        pipelineDescriptor.getCompute().setConstantCount(0);
        pipelineDescriptor.getCompute().setConstants();
        pipelineDescriptor.getCompute().setEntryPoint("computeStuff");
        pipelineDescriptor.getCompute().setModule(shader.getHandle());
        pipelineDescriptor.setLayout(pipelineLayout.getHandle());

        return webGPU.wgpuDeviceCreateComputePipeline(LibGPU.device.getHandle(), pipelineDescriptor);
    }



    private void compute(BindGroup bindGroup, Buffer inputBuffer, Buffer outputBuffer, Buffer mapBuffer) {

        Queue queue = new Queue(LibGPU.device);

        // Fill input buffer
        int numFloats = BUFFER_SIZE / Float.BYTES;
        for (int i = 0; i < numFloats; i++)
            inputData[i] = 0.1f * i;
        // copy float array to native memory
        Pointer input = JavaWebGPU.createFloatArrayPointer(inputData);

        // write to input buffer
        queue.writeBuffer(inputBuffer, 0, input, BUFFER_SIZE);

        CommandEncoder encoder = new CommandEncoder(LibGPU.device);

        ComputePass pass = encoder.beginComputePass();

        // set pipeline & bind group 0
        pass.setPipeline(pipeline);
        pass.setBindGroup(0, bindGroup);

        int workGroupSize = 32;
        int invocationCount = BUFFER_SIZE / Float.BYTES;    // nr of input values
        // This ceils invocationCount / workgroupSize
        int workgroupCount = (invocationCount + workGroupSize - 1) / workGroupSize;
        pass.dispatchWorkGroups( workgroupCount, 1, 1);

        pass.end();

        // copy output buffer to map buffer so that we can read it back
        encoder.copyBufferToBuffer(outputBuffer, 0, mapBuffer, 0, BUFFER_SIZE);

        // finish the encoder to give use command buffer
        CommandBuffer commandBuffer = encoder.finish();
        encoder.dispose();
        // feed the command buffer to the queue
        queue.submit(commandBuffer);

        boolean[] done = { false };
        WGPUBufferMapCallback callback = (WGPUBufferMapAsyncStatus status, Pointer userdata) -> {
            if (status == WGPUBufferMapAsyncStatus.Success) {
                Pointer buf = webGPU.wgpuBufferGetConstMappedRange(mapBuffer.getHandle(), 0, BUFFER_SIZE);
                for(int i = 0; i < numFloats; i++){
                    outputData[i] = buf.getFloat(i*Float.BYTES);
                }
            } else
                System.out.println("Buffer map async error: "+status);
            done[0] = true; // signal that the call back was executed
        };

        // note: there is a newer function for this and using this one will raise a warning,
        // but it requires a struct containing a pointer to a callback function...
        webGPU.wgpuBufferMapAsync(mapBuffer.getHandle(), WGPUMapMode.Read, 0, BUFFER_SIZE, callback, null);

        while(!done[0]) {
            System.out.println("Tick.");
            LibGPU.device.tick();
        }

        System.out.println("output: ");
        for(int i = 0; i < 5; i++)
            System.out.print(" "+outputData[i]);
        System.out.println();

        commandBuffer.dispose();
        encoder.dispose();
        pass.dispose();
        queue.dispose();
    }


    @Override
    public void render(){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){
            LibGPU.app.exit();
            return;
        }

        batch.begin(Color.TEAL);
        int y = 300;
        font.draw(batch, "Compute Shader", 10, y);
        y-=30;
        font.draw(batch, "Input", 10, y);
        for(int i = 0; i < 9; i++)
            font.draw(batch, " "+inputData[i], 100+30*i, y);
        y-=30;
        font.draw(batch, "Output", 10, y);
        for(int i = 0; i < 9; i++)
            font.draw(batch, " "+outputData[i], 100+30*i, y);
        batch.end();
    }

    @Override
    public void dispose(){
        // cleanup
        batch.dispose();
        font.dispose();
    }

    @Override
    public void resize(int width, int height) {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }


}
