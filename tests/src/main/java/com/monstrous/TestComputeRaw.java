package com.monstrous;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.webgpu.*;
import jnr.ffi.Pointer;

/**
 * Demonstration of using a compute shader.
 * Follows example from https://eliemichel.github.io/LearnWebGPU/basic-compute/compute-pipeline.html#
 * Uses pure (raw) webGPU calls to invoke the compute shader.
 */

public class TestComputeRaw extends ApplicationAdapter {

    private static int BUFFER_SIZE = 64*Float.BYTES;    // bytes

    private SpriteBatch batch;
    private BitmapFont font;

    private WebGPU_JNI webGPU;
    private Pointer inputBuffer, outputBuffer, mapBuffer;
    private Pointer bindGroupLayout;
    private Pointer bindGroup;
    private Pointer pipelineLayout;
    private Pointer pipeline;
    private Pointer shaderModule;
    float[] inputData = new float[BUFFER_SIZE/Float.BYTES];
    float[] outputData = new float[BUFFER_SIZE/Float.BYTES];

    private String source = "@group(0) @binding(0) var<storage,read> inputBuffer: array<f32,64>;\n" +
            "@group(0) @binding(1) var<storage,read_write> outputBuffer: array<f32,64>;\n" +
            "\n" +
            "// The function to evaluate for each element of the processed buffer\n" +
            "fn f(x: f32) -> f32 {\n" +
            "    return 2.0 * x + 1.0;\n" +
            "}\n" +
            "\n" +
            "@compute @workgroup_size(32)\n" +
            "fn computeStuff(@builtin(global_invocation_id) id: vec3<u32>) {\n" +
            "    // Apply the function f to the buffer element at index id.x:\n" +
            "    outputBuffer[id.x] = f(inputBuffer[id.x]);\n" +
            "}";

    @Override
    public void create() {

        batch = new SpriteBatch();
        font = new BitmapFont();

        webGPU = LibGPU.webGPU;
        makeBuffers();
        bindGroupLayout = makeBindGroupLayout();
        bindGroup = makeBindGroup(bindGroupLayout);
        shaderModule = compile(source);
        pipeline = makeComputePipeline(shaderModule, bindGroupLayout);

        compute();

    }

    private void makeBuffers(){
        // Create input and output buffers
        WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
        bufferDesc.setLabel("Input storage buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.Storage );
        bufferDesc.setSize( BUFFER_SIZE );
        bufferDesc.setMappedAtCreation(0L);
        inputBuffer = LibGPU.webGPU.wgpuDeviceCreateBuffer(LibGPU.device, bufferDesc);

        bufferDesc.setLabel("Output storage buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopySrc | WGPUBufferUsage.Storage );
        outputBuffer = LibGPU.webGPU.wgpuDeviceCreateBuffer(LibGPU.device, bufferDesc);

        // Create an intermediary buffer to which we copy the output and that can be
        // used for reading into the CPU memory.
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.MapRead );
        mapBuffer = webGPU.wgpuDeviceCreateBuffer(LibGPU.device, bufferDesc);
    }

    private Pointer makeBindGroupLayout(){

        // input buffer
        WGPUBindGroupLayoutEntry bindingLayout0 = WGPUBindGroupLayoutEntry.createDirect();
        setDefaultLayout(bindingLayout0);
        bindingLayout0.setBinding(0);
        bindingLayout0.setVisibility(WGPUShaderStage.Compute );
        bindingLayout0.getBuffer().setType(WGPUBufferBindingType.ReadOnlyStorage);

        // output buffer
        WGPUBindGroupLayoutEntry bindingLayout1 = WGPUBindGroupLayoutEntry.createDirect();
        setDefaultLayout(bindingLayout1);
        bindingLayout1.setBinding(1);
        bindingLayout1.setVisibility(WGPUShaderStage.Compute );
        bindingLayout1.getBuffer().setType(WGPUBufferBindingType.Storage);

        // Create a bind group layout
        WGPUBindGroupLayoutDescriptor bindGroupLayoutDesc = WGPUBindGroupLayoutDescriptor.createDirect();
        bindGroupLayoutDesc.setNextInChain();
        bindGroupLayoutDesc.setEntryCount(2);
        bindGroupLayoutDesc.setEntries(bindingLayout0, bindingLayout1);

        return webGPU.wgpuDeviceCreateBindGroupLayout(LibGPU.device, bindGroupLayoutDesc);
    }

    private Pointer makeBindGroup(Pointer bindGroupLayout){
        WGPUBindGroupEntry entry0 = WGPUBindGroupEntry.createDirect();
        entry0.setBinding(0);
        entry0.setBuffer(inputBuffer);
        entry0.setOffset(0);
        entry0.setSize(BUFFER_SIZE);

        WGPUBindGroupEntry entry1 = WGPUBindGroupEntry.createDirect();
        entry1.setBinding(1);
        entry1.setBuffer(outputBuffer);
        entry1.setOffset(0);
        entry1.setSize(BUFFER_SIZE);

        WGPUBindGroupDescriptor bindGroupDescriptor = WGPUBindGroupDescriptor.createDirect();
        bindGroupDescriptor.setNextInChain()
                .setLayout(bindGroupLayout)
                .setEntryCount(2)
                .setEntries(entry0, entry1);
        return webGPU.wgpuDeviceCreateBindGroup(LibGPU.device, bindGroupDescriptor);
    }

    private Pointer compile(String shaderSource){

        // Create Shader Module
        WGPUShaderModuleDescriptor shaderDesc = WGPUShaderModuleDescriptor.createDirect();

        WGPUShaderModuleWGSLDescriptor shaderCodeDesc = WGPUShaderModuleWGSLDescriptor.createDirect();
        shaderCodeDesc.getChain().setNext();
        shaderCodeDesc.getChain().setSType(WGPUSType.ShaderModuleWGSLDescriptor);
        shaderCodeDesc.setCode(shaderSource);

        shaderDesc.getNextInChain().set(shaderCodeDesc.getPointerTo());

        shaderModule = LibGPU.webGPU.wgpuDeviceCreateShaderModule(LibGPU.device, shaderDesc);
        if(shaderModule == null)
            throw new RuntimeException("ShaderModule: compile failed.");
        return shaderModule;
    }

    private Pointer makeComputePipeline(Pointer shaderModule, Pointer bindGroupLayout){
        long[] layouts = new long[1];
        layouts[0] = bindGroupLayout.address();
        Pointer layoutPtr = JavaWebGPU.createLongArrayPointer(layouts);

        WGPUPipelineLayoutDescriptor pipelineLayoutDesc = WGPUPipelineLayoutDescriptor.createDirect();
        pipelineLayoutDesc.setNextInChain();
        pipelineLayoutDesc.setBindGroupLayoutCount(1);
        pipelineLayoutDesc.setBindGroupLayouts(layoutPtr);  // expects an array of layouts
        pipelineLayout = webGPU.wgpuDeviceCreatePipelineLayout(LibGPU.device, pipelineLayoutDesc);

        WGPUComputePipelineDescriptor pipelineDescriptor = WGPUComputePipelineDescriptor.createDirect();
        pipelineDescriptor.setNextInChain();
        pipelineDescriptor.getCompute().setConstantCount(0);
        pipelineDescriptor.getCompute().setConstants();
        pipelineDescriptor.getCompute().setEntryPoint("computeStuff");
        pipelineDescriptor.getCompute().setModule(shaderModule);
        pipelineDescriptor.setLayout(pipelineLayout);

        return webGPU.wgpuDeviceCreateComputePipeline(LibGPU.device, pipelineDescriptor);
    }

    private void compute() {

        // Fill input buffer
        int numFloats = BUFFER_SIZE / Float.BYTES;
        for (int i = 0; i < numFloats; i++)
            inputData[i] = 0.1f * i;
        // copy float array to native memory
        Pointer input = JavaWebGPU.createFloatArrayPointer(inputData);
        // write to input buffer
        webGPU.wgpuQueueWriteBuffer(LibGPU.queue.getHandle(), inputBuffer, 0, input, BUFFER_SIZE);

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
        webGPU.wgpuComputePassEncoderSetBindGroup(computePass, 0, bindGroup, 0, JavaWebGPU.createNullPointer());

        int workGroupSize = 32;
        int invocationCount = BUFFER_SIZE / Float.BYTES;    // nr of input values
        // This ceils invocationCount / workgroupSize
        int workgroupCount = (invocationCount + workGroupSize - 1) / workGroupSize;
        webGPU.wgpuComputePassEncoderDispatchWorkgroups(computePass, workgroupCount, 1, 1);

        webGPU.wgpuComputePassEncoderEnd(computePass);

        // copy output buffer to map buffer so that we can read it back
        webGPU.wgpuCommandEncoderCopyBufferToBuffer(encoder, outputBuffer, 0, mapBuffer, 0, BUFFER_SIZE);

        // finish the encoder to give use command buffer
        WGPUCommandBufferDescriptor bufferDescriptor = WGPUCommandBufferDescriptor.createDirect();
        bufferDescriptor.setNextInChain();
        Pointer commandBuffer = webGPU.wgpuCommandEncoderFinish(encoder, bufferDescriptor);
        webGPU.wgpuCommandEncoderRelease(encoder);

        // feed the command buffer to the queue
        long[] buffers = new long[1];
        buffers[0] = commandBuffer.address();
        Pointer bufferPtr = JavaWebGPU.createLongArrayPointer(buffers);
        webGPU.wgpuQueueSubmit(LibGPU.queue.getHandle(), 1, bufferPtr);

        boolean[] done = { false };
        WGPUBufferMapCallback callback = (WGPUBufferMapAsyncStatus status, Pointer userdata) -> {
            if (status == WGPUBufferMapAsyncStatus.Success) {
                Pointer buf = webGPU.wgpuBufferGetConstMappedRange(mapBuffer, 0, BUFFER_SIZE);
                for(int i = 0; i < numFloats; i++){
                    outputData[i] = buf.getFloat(i*Float.BYTES);
                }
            } else
                System.out.println("Buffer map async error: "+status);
            done[0] = true; // signal that the call back was executed
        };



        // note: there is a newer function for this and using this one will raise a warning
        webGPU.wgpuBufferMapAsync(mapBuffer, WGPUMapMode.Read, 0, BUFFER_SIZE, callback, null);

        while(!done[0]) {
            System.out.println("Tick.");
            webGPU.wgpuDeviceTick(LibGPU.device);   // Dawn
        }

        System.out.println("output: ");
        for(int i = 0; i < 5; i++)
            System.out.print(" "+outputData[i]);
        System.out.println("");

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

        // SpriteBatch testing
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

        webGPU.wgpuBindGroupRelease(bindGroup);
        webGPU.wgpuBindGroupLayoutRelease(bindGroupLayout);
        webGPU.wgpuBufferDestroy(inputBuffer);
        webGPU.wgpuBufferRelease(inputBuffer);
        webGPU.wgpuBufferDestroy(outputBuffer);
        webGPU.wgpuBufferRelease(outputBuffer);
        webGPU.wgpuBufferDestroy(mapBuffer);
        webGPU.wgpuBufferRelease(mapBuffer);
        webGPU.wgpuComputePipelineRelease(pipeline);
        webGPU.wgpuPipelineLayoutRelease(pipelineLayout);
    }

    @Override
    public void resize(int width, int height) {

        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }


    private void setDefaultLayout(WGPUBindGroupLayoutEntry bindingLayout) {

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

}
