package com.monstrous;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.ShaderProgram;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.webgpu.BindGroup;
import com.monstrous.graphics.webgpu.BindGroupLayout;
import com.monstrous.graphics.webgpu.Buffer;
import com.monstrous.graphics.webgpu.PipelineLayout;
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
        Buffer mapBuffer = new Buffer("Map buffer", BUFFER_SIZE,WGPUBufferUsage.CopyDst | WGPUBufferUsage.MapRead );

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

        return webGPU.wgpuDeviceCreateComputePipeline(LibGPU.device, pipelineDescriptor);
    }

    private void compute(BindGroup bindGroup, Buffer inputBuffer, Buffer outputBuffer, Buffer mapBuffer) {

        // Fill input buffer
        int numFloats = BUFFER_SIZE / Float.BYTES;
        for (int i = 0; i < numFloats; i++)
            inputData[i] = 0.1f * i;
        // copy float array to native memory
        Pointer input = JavaWebGPU.createFloatArrayPointer(inputData);
        // write to input buffer
        webGPU.wgpuQueueWriteBuffer(LibGPU.queue, inputBuffer.getHandle(), 0, input, BUFFER_SIZE);

        // create a command encoder
        WGPUCommandEncoderDescriptor encoderDesc = WGPUCommandEncoderDescriptor.createDirect();
        encoderDesc.setNextInChain();
        Pointer encoder = webGPU.wgpuDeviceCreateCommandEncoder(LibGPU.device, encoderDesc);

        // Create a compute pass
        WGPUComputePassDescriptor passDesc = WGPUComputePassDescriptor.createDirect();
        passDesc.setNextInChain();
        passDesc.setTimestampWrites();
        Pointer computePass = webGPU.wgpuCommandEncoderBeginComputePass(encoder, passDesc);

        // set pipeline
        webGPU.wgpuComputePassEncoderSetPipeline(computePass, pipeline);
        // set bind group
        webGPU.wgpuComputePassEncoderSetBindGroup(computePass, 0, bindGroup.getHandle(), 0, JavaWebGPU.createNullPointer());

        int workGroupSize = 32;
        int invocationCount = BUFFER_SIZE / Float.BYTES;    // nr of input values
        // This ceils invocationCount / workgroupSize
        int workgroupCount = (invocationCount + workGroupSize - 1) / workGroupSize;
        webGPU.wgpuComputePassEncoderDispatchWorkgroups(computePass, workgroupCount, 1, 1);

        webGPU.wgpuComputePassEncoderEnd(computePass);

        // copy output buffer to map buffer so that we can read it back
        webGPU.wgpuCommandEncoderCopyBufferToBuffer(encoder, outputBuffer.getHandle(), 0, mapBuffer.getHandle(), 0, BUFFER_SIZE);

        // finish the encoder to give use command buffer
        WGPUCommandBufferDescriptor bufferDescriptor = WGPUCommandBufferDescriptor.createDirect();
        bufferDescriptor.setNextInChain();
        Pointer commandBuffer = webGPU.wgpuCommandEncoderFinish(encoder, bufferDescriptor);
        webGPU.wgpuCommandEncoderRelease(encoder);

        // feed the command buffer to the queue
        long[] buffers = new long[1];
        buffers[0] = commandBuffer.address();
        Pointer bufferPtr = JavaWebGPU.createLongArrayPointer(buffers);
        webGPU.wgpuQueueSubmit(LibGPU.queue, 1, bufferPtr);

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
            webGPU.wgpuDeviceTick(LibGPU.device);   // Dawn
        }

        System.out.println("output: ");
        for(int i = 0; i < 5; i++)
            System.out.print(" "+outputData[i]);
        System.out.println();

        webGPU.wgpuCommandBufferRelease(commandBuffer);
        webGPU.wgpuCommandEncoderRelease(encoder);
        //webGPU.wgpuComputePassEncoderRelease(computePass); // causes crash
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
