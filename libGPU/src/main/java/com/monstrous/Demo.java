package com.monstrous;

import com.monstrous.utils.WgpuJava;
import com.monstrous.wgpu.*;
import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.glfwGetTime;


public class Demo {
    private String shaderSource = readShaderSource();

    private static Runtime runtime;
    private WGPU wgpu;
    private Pointer surface;
    private Pointer instance;
    private Pointer device;
    private Pointer queue;
    private Pointer pipeline;
    private WGPUTextureFormat surfaceFormat = WGPUTextureFormat.Undefined;
    private Pointer vertexBuffer;
    private int vertexCount;
    private Pointer indexBuffer;
    private int indexCount;
    private Pointer uniformBuffer;
    private Pointer layout;
    private Pointer bindGroupLayout;
    private Pointer bindGroup;
    private Pointer uniformData;

    public void init(long windowHandle) {
        wgpu = LibraryLoader.create(WGPU.class).load("wrapper"); // load the library into the libc variable
        runtime = Runtime.getRuntime(wgpu);
        WgpuJava.setRuntime(runtime);

        // debug test
        System.out.println("Hello world!");
        int sum = wgpu.add(1200, 34);
        System.out.println("sum = " + sum);

        instance = wgpu.CreateInstance();
        System.out.println("instance = " + instance);

        System.out.println("window = " + Long.toString(windowHandle, 16));
        surface = wgpu.glfwGetWGPUSurface(instance, windowHandle);
        System.out.println("surface = " + surface);

        System.out.println("define adapter options");
        WGPURequestAdapterOptions options = WGPURequestAdapterOptions.createDirect();
        options.setNextInChain();
        options.setCompatibleSurface(surface);
        options.setBackendType(WGPUBackendType.D3D12);

        System.out.println("defined adapter options");

        // Get Adapter
        Pointer adapter = wgpu.RequestAdapterSync(instance, options);
        System.out.println("adapter = " + adapter);

        WGPUSupportedLimits supportedLimits = WGPUSupportedLimits.createDirect();
        wgpu.AdapterGetLimits(adapter, supportedLimits);
        System.out.println("adapter maxVertexAttributes " + supportedLimits.getLimits().getMaxVertexAttributes());

        System.out.println("maxTextureDimension1D " + supportedLimits.getLimits().getMaxTextureDimension1D());
        System.out.println("maxTextureDimension2D " + supportedLimits.getLimits().getMaxTextureDimension2D());
        System.out.println("maxTextureDimension3D " + supportedLimits.getLimits().getMaxTextureDimension3D());
        System.out.println("maxTextureArrayLayers " + supportedLimits.getLimits().getMaxTextureArrayLayers());


        WGPUAdapterProperties adapterProperties = WGPUAdapterProperties.createDirect();
        adapterProperties.setNextInChain();

        wgpu.AdapterGetProperties(adapter, adapterProperties);

        System.out.println("VendorID: " + adapterProperties.getVendorID());
        System.out.println("Vendor name: " + adapterProperties.getVendorName());
        System.out.println("Device ID: " + adapterProperties.getDeviceID());
        System.out.println("Back end: " + adapterProperties.getBackendType());
        System.out.println("Description: " + adapterProperties.getDriverDescription());

        WGPURequiredLimits requiredLimits = WGPURequiredLimits.createDirect();
        setDefault(requiredLimits.getLimits());
        requiredLimits.getLimits().setMaxVertexAttributes(2);
        requiredLimits.getLimits().setMaxVertexBuffers(2);
        requiredLimits.getLimits().setMaxInterStageShaderComponents(3); // 3 floats from vert to frag
        requiredLimits.getLimits().setMaxBufferSize(300);
        requiredLimits.getLimits().setMaxVertexBufferArrayStride(12);

        requiredLimits.getLimits().setMaxBindGroups(1);        // We use at most 1 bind group for now
        requiredLimits.getLimits().setMaxUniformBuffersPerShaderStage(1);// We use at most 1 uniform buffer per stage
        // Uniform structs have a size of maximum 16 float (more than what we need)
        requiredLimits.getLimits().setMaxUniformBufferBindingSize(16*4);



        // Get Device
        WGPUDeviceDescriptor deviceDescriptor = WGPUDeviceDescriptor.createDirect();
        deviceDescriptor.setNextInChain();
        deviceDescriptor.setLabel("My Device");
        deviceDescriptor.setRequiredLimits(requiredLimits);

        device = wgpu.RequestDeviceSync(adapter, deviceDescriptor);
        wgpu.AdapterRelease(adapter);       // we can release our adapter as soon as we have a device

        // use a lambda expression to define a callback function
        WGPUErrorCallback deviceCallback = (WGPUErrorType type, String message, Pointer userdata) -> {
            System.out.println("*** Device error: " + type + " : " + message);
        };
        wgpu.DeviceSetUncapturedErrorCallback(device, deviceCallback, null);

        wgpu.DeviceGetLimits(device, supportedLimits);
        System.out.println("device maxVertexAttributes " + supportedLimits.getLimits().getMaxVertexAttributes());

        System.out.println("maxTextureDimension1D " + supportedLimits.getLimits().getMaxTextureDimension1D());
        System.out.println("maxTextureDimension2D " + supportedLimits.getLimits().getMaxTextureDimension2D());
        System.out.println("maxTextureDimension3D " + supportedLimits.getLimits().getMaxTextureDimension3D());
        System.out.println("maxTextureArrayLayers " + supportedLimits.getLimits().getMaxTextureArrayLayers());

        queue = wgpu.DeviceGetQueue(device);

        // use a lambda expression to define a callback function
        WGPUQueueWorkDoneCallback queueCallback = (WGPUQueueWorkDoneStatus status, Pointer userdata) -> {
            System.out.println("=== Queue work finished with status: " + status);
        };
        wgpu.QueueOnSubmittedWorkDone(queue, queueCallback, null);


        // configure the surface
        WGPUSurfaceConfiguration config = WGPUSurfaceConfiguration.createDirect();
        config.setNextInChain();

        config.setWidth(640);
        config.setHeight(480);

        surfaceFormat = wgpu.SurfaceGetPreferredFormat(surface, adapter);
        System.out.println("Using format: " + surfaceFormat);
        config.setFormat(surfaceFormat);
        // And we do not need any particular view format:
        config.setViewFormatCount(0);
        config.setViewFormats(WgpuJava.createNullPointer());
        config.setUsage(WGPUTextureUsage.RenderAttachment);
        config.setDevice(device);
        config.setPresentMode(WGPUPresentMode.Fifo);
        config.setAlphaMode(WGPUCompositeAlphaMode.Auto);

        wgpu.SurfaceConfigure(surface, config);

        initializePipeline();
        //playingWithBuffers();

        initBuffers();
        initBindGroups();

        float[] uniforms = new float[1];
        uniformData = WgpuJava.createFloatArrayPointer(uniforms);
    }

    private void playingWithBuffers() {

        WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
        bufferDesc.setLabel("Some GPU-side data buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.CopySrc );
        bufferDesc.setSize(16);
        bufferDesc.setMappedAtCreation(0L);
        Pointer buffer1 = wgpu.DeviceCreateBuffer(device, bufferDesc);

        bufferDesc.setLabel("Output buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.MapRead );
        bufferDesc.setSize(16);
        bufferDesc.setMappedAtCreation(0L);

        Pointer buffer2 = wgpu.DeviceCreateBuffer(device, bufferDesc);

        // Create some CPU-side data buffer (of size 16 bytes)
        byte[] numbers = new byte[16];
        for(int i = 0; i < 16; i++)
            numbers[i] = (byte)i;
        Pointer data = WgpuJava.createByteArrayPointer(numbers);
        // `numbers` now contains [ 0, 1, 2, ... ]

        // Copy this from `numbers` (RAM) to `buffer1` (VRAM)
        wgpu.QueueWriteBuffer(queue, buffer1, 0, data, numbers.length);

        Pointer encoder = wgpu.DeviceCreateCommandEncoder(device, null);

        // After creating the command encoder

        // [...] Copy buffer to buffer
        // size must be multiple of 4
        wgpu.CommandEncoderCopyBufferToBuffer(encoder, buffer1, 0, buffer2, 0, 16);


        Pointer command = wgpu.CommandEncoderFinish(encoder, null);
        wgpu.CommandEncoderRelease(encoder);

        // BEWARE: we need this convoluted call sequence or it will crash
        long[] buffers = new long[1];
        buffers[0] = command.address();
        Pointer bufferPtr = WgpuJava.createLongArrayPointer(buffers);
        wgpu.QueueSubmit(queue, 1, bufferPtr);


        wgpu.CommandBufferRelease(command);

        // use a lambda expression to define a callback function
        WGPUBufferMapCallback onBuffer2Mapped = (WGPUBufferMapAsyncStatus status, Pointer userData) -> {
            System.out.println("=== Buffer 2 mapped with status: " + status);
            userData.putInt(0, 1);
        };

        int[] ready = new int[1];
        ready[0] = 0;

        Pointer udata = WgpuJava.createIntegerArrayPointer(ready);
        System.out.println(udata);
        wgpu.BufferMapAsync(buffer2, WGPUMapMode.Read, 0, 16, onBuffer2Mapped, udata);


        int iters = 0;
        // note you cannot test ready[0] because createIntegerArrayPointer made a copy
        while(udata.getInt(0) == 0){
            iters++;
            wgpu.DeviceTick(device);
        }
        System.out.println(" Iterations: "+iters);

        System.out.println(" received: " + String.valueOf(udata.getInt(0)));

        // Get a pointer to wherever the driver mapped the GPU memory to the RAM
        Pointer ram =  wgpu.BufferGetConstMappedRange(buffer2, 0, 16);
        for(int i = 0; i < 16; i++){
            byte num = ram.getByte(i);
            System.out.print(num);
            System.out.print(' ');
        }
        System.out.println();

// Then do not forget to unmap the memory
        wgpu.BufferUnmap(buffer2);

        wgpu.BufferRelease(buffer1);
        wgpu.BufferRelease(buffer2);

    }

    private String readShaderSource() {
        String src = null;
        try {
            src = Files.readString(Paths.get("shader.wgsl"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return src;
    }

    private void initBuffers() {

        FileInput input = new FileInput("webgpu.txt");
        System.out.println("input file size : "+input.size());
        ArrayList<Integer> indexValues = new ArrayList<>();
        ArrayList<Float> vertFloats = new ArrayList<>();
        int mode = 0;
        for(int lineNr = 0; lineNr < input.size(); lineNr++) {
            String line = input.get(lineNr).strip();
            if (line.contentEquals("[points]")) {
                mode = 1;
                continue;
            }
            if (line.contentEquals("[indices]")) {
                mode = 2;
                continue;
            }
            if(line.startsWith("#"))
                continue;
            if(line.length() == 0)
                continue;
            if(mode == 1){
                String [] words = line.split("[ \t]+");
                if(words.length != 5)
                    System.out.println("Expected 5 floats per vertex : "+line);
                for(int i = 0; i < 5; i++)
                    vertFloats.add(Float.parseFloat(words[i]));
            } else if (mode == 2){
                String [] words = line.split("[ \t]+");
                if(words.length != 3)
                    System.out.println("Expected 3 indices per line: "+line);
                for(int i = 0; i < 3; i++)
                    indexValues.add(Integer.parseInt(words[i]));
            } else {
                System.out.println("Unexpected input: "+line);
            }
        }

        vertexCount = vertFloats.size()/5;
        float[] vertexData = new float[ vertFloats.size() ];
        for(int i = 0; i < vertFloats.size(); i++){
            vertexData[i] = vertFloats.get(i);
        }

        indexCount = indexValues.size();
        int [] indexData = new int[ indexCount ];
        for(int i = 0; i < indexCount; i++){
            indexData[i] = indexValues.get(i);
        }

        // Create vertex buffer
        WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
        bufferDesc.setLabel("Vertex buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.Vertex );
        bufferDesc.setSize(vertexData.length*Float.BYTES);
        bufferDesc.setMappedAtCreation(0L);
        vertexBuffer = wgpu.DeviceCreateBuffer(device, bufferDesc);

        Pointer data = WgpuJava.createFloatArrayPointer(vertexData);

        // Upload geometry data to the buffer
        wgpu.QueueWriteBuffer(queue, vertexBuffer, 0, data, (int)bufferDesc.getSize());

        // Create index buffer
        bufferDesc.setLabel("Index buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.Index );
        bufferDesc.setSize(indexData.length*Integer.BYTES);
        // in case we use a sort index:
        //bufferDesc.size = (bufferDesc.size + 3) & ~3; // round up to the next multiple of 4
        bufferDesc.setMappedAtCreation(0L);
        indexBuffer = wgpu.DeviceCreateBuffer(device, bufferDesc);

        Pointer idata = WgpuJava.createIntegerArrayPointer(indexData);


        // Upload data to the buffer
        wgpu.QueueWriteBuffer(queue, indexBuffer, 0, idata, (int)bufferDesc.getSize());

        // Create uniform buffer
        //WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
        bufferDesc.setLabel("Uniform buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.Uniform );
        bufferDesc.setSize(4*Float.BYTES);
        bufferDesc.setMappedAtCreation(0L);
        uniformBuffer = wgpu.DeviceCreateBuffer(device, bufferDesc);

    }

    private void initBindGroups() {
        // Create a binding
        WGPUBindGroupEntry binding = WGPUBindGroupEntry.createDirect();
        binding.setNextInChain();
        binding.setBinding(0);  // binding index
        binding.setBuffer(uniformBuffer);
        binding.setOffset(0);
        binding.setSize(4 * Float.BYTES);

        // A bind group contains one or multiple bindings
        WGPUBindGroupDescriptor bindGroupDesc = WGPUBindGroupDescriptor.createDirect();
        bindGroupDesc.setNextInChain();
        bindGroupDesc.setLayout(bindGroupLayout);
        // There must be as many bindings as declared in the layout!
        bindGroupDesc.setEntryCount(1);
        bindGroupDesc.setEntries(binding);
        bindGroup = wgpu.DeviceCreateBindGroup(device, bindGroupDesc);
    }


    private void initializePipeline() {

        // Create Shader Module
        WGPUShaderModuleDescriptor shaderDesc = WGPUShaderModuleDescriptor.createDirect();
        shaderDesc.setLabel("My Shader");



        WGPUShaderModuleWGSLDescriptor shaderCodeDesc = WGPUShaderModuleWGSLDescriptor.createDirect();
        shaderCodeDesc.getChain().setNext();
        shaderCodeDesc.getChain().setSType(WGPUSType.ShaderModuleWGSLDescriptor);
        shaderCodeDesc.setCode(shaderSource);

        //System.out.println("shaderSource: "+shaderSource);

        shaderDesc.getNextInChain().set(shaderCodeDesc.getPointerTo());

        Pointer shaderModule = wgpu.DeviceCreateShaderModule(device, shaderDesc);


        //  create an array of WGPUVertexAttribute
        int attribCount = 2;

        WGPUVertexAttribute positionAttrib =  WGPUVertexAttribute.createDirect();

        positionAttrib.setFormat(WGPUVertexFormat.Float32x2);
        positionAttrib.setOffset(0);
        positionAttrib.setShaderLocation(0);

        WGPUVertexAttribute colorAttrib = WGPUVertexAttribute.createDirect();   // freed where?

        colorAttrib.setFormat(WGPUVertexFormat.Float32x3);
        colorAttrib.setOffset(2*Float.BYTES);
        colorAttrib.setShaderLocation(1);


        WGPUVertexBufferLayout vertexBufferLayout = WGPUVertexBufferLayout.createDirect();
        vertexBufferLayout.setAttributeCount(attribCount);

        vertexBufferLayout.setAttributes(positionAttrib, colorAttrib);
        vertexBufferLayout.setArrayStride(5*Float.BYTES);
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
        colorTarget.setFormat(surfaceFormat);
        colorTarget.setBlend(blendState);
        colorTarget.setWriteMask(WGPUColorWriteMask.All);

        fragmentState.setTargetCount(1);
        fragmentState.setTargets(colorTarget);

        pipelineDesc.setFragment(fragmentState);

        pipelineDesc.setDepthStencil();

        pipelineDesc.getMultisample().setCount(1);
        pipelineDesc.getMultisample().setMask( 0xFFFFFFFF);
        pipelineDesc.getMultisample().setAlphaToCoverageEnabled(0);

        // Define binding layout
        WGPUBindGroupLayoutEntry bindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(bindingLayout);
        bindingLayout.setBinding(0);
        bindingLayout.setVisibility(WGPUShaderStage.Vertex);
        bindingLayout.getBuffer().setType(WGPUBufferBindingType.Uniform);
        bindingLayout.getBuffer().setMinBindingSize(4 * Float.BYTES);

        // Create a bind group layout
        WGPUBindGroupLayoutDescriptor bindGroupLayoutDesc = WGPUBindGroupLayoutDescriptor.createDirect();
        bindGroupLayoutDesc.setNextInChain();
        bindGroupLayoutDesc.setLabel("My BG Layout");
        bindGroupLayoutDesc.setEntryCount(1);
        bindGroupLayoutDesc.setEntries(bindingLayout);
        bindGroupLayout = wgpu.DeviceCreateBindGroupLayout(device, bindGroupLayoutDesc);


        long[] layouts = new long[1];
        layouts[0] = bindGroupLayout.address();
        Pointer layoutPtr = WgpuJava.createLongArrayPointer(layouts);

        // Create the pipeline layout
        WGPUPipelineLayoutDescriptor layoutDesc = WGPUPipelineLayoutDescriptor.createDirect();
        layoutDesc.setNextInChain();
        layoutDesc.setLabel("Pipeline Layout");
        layoutDesc.setBindGroupLayoutCount(1);
        layoutDesc.setBindGroupLayouts(layoutPtr);
        layout = wgpu.DeviceCreatePipelineLayout(device, layoutDesc);

        pipelineDesc.setLayout(layout);
        pipeline = wgpu.DeviceCreateRenderPipeline(device, pipelineDesc);
        wgpu.ShaderModuleRelease(shaderModule);


    }


    public void render(){
        // loop
        Pointer targetView = getNextSurfaceTextureView();
        if (targetView.address() == 0) {
            System.out.println("*** Invalid target view");
            return;
        }

        float currentTime = (float) glfwGetTime();
        uniformData.putFloat(0,currentTime);
        wgpu.QueueWriteBuffer(queue, uniformBuffer, 0, uniformData, Float.BYTES);


        WGPUCommandEncoderDescriptor encoderDescriptor = WGPUCommandEncoderDescriptor.createDirect();
        encoderDescriptor.setNextInChain();
        encoderDescriptor.setLabel("My Encoder");

        Pointer encoder = wgpu.DeviceCreateCommandEncoder(device, encoderDescriptor);

        WGPURenderPassColorAttachment renderPassColorAttachment = WGPURenderPassColorAttachment.createDirect();
        renderPassColorAttachment.setNextInChain();
        renderPassColorAttachment.setView(targetView);
        renderPassColorAttachment.setResolveTarget(WgpuJava.createNullPointer());
        renderPassColorAttachment.setLoadOp(WGPULoadOp.Clear);
        renderPassColorAttachment.setStoreOp(WGPUStoreOp.Store);

        renderPassColorAttachment.getClearValue().setR(0.25);
        renderPassColorAttachment.getClearValue().setG(0.25);
        renderPassColorAttachment.getClearValue().setB(0.25);
        renderPassColorAttachment.getClearValue().setA(1.0);

        renderPassColorAttachment.setDepthSlice(wgpu.WGPU_DEPTH_SLICE_UNDEFINED);

        WGPURenderPassDescriptor renderPassDescriptor = WGPURenderPassDescriptor.createDirect();
        renderPassDescriptor.setNextInChain();

        renderPassDescriptor.setColorAttachmentCount(1);
        renderPassDescriptor.setColorAttachments( renderPassColorAttachment );
        renderPassDescriptor.setOcclusionQuerySet(WgpuJava.createNullPointer());
        renderPassDescriptor.setDepthStencilAttachment();
        renderPassDescriptor.setTimestampWrites();

        Pointer renderPass = wgpu.CommandEncoderBeginRenderPass(encoder, renderPassDescriptor);
// [...] Use Render Pass

        wgpu.RenderPassEncoderSetPipeline(renderPass, pipeline);

        // Set vertex buffer while encoding the render pass
        wgpu.RenderPassEncoderSetVertexBuffer(renderPass, 0, vertexBuffer, 0, wgpu.BufferGetSize(vertexBuffer));
        wgpu.RenderPassEncoderSetIndexBuffer(renderPass, indexBuffer, WGPUIndexFormat.Uint32, 0, wgpu.BufferGetSize(indexBuffer));

        wgpu.RenderPassEncoderSetBindGroup(renderPass, 0, bindGroup, 0, null);


        wgpu.RenderPassEncoderDrawIndexed(renderPass, indexCount, 1, 0, 0, 0);

        //wgpu.RenderPassEncoderDraw(renderPass, vertexCount, 1, 0, 0);

        wgpu.RenderPassEncoderEnd(renderPass);
        wgpu.RenderPassEncoderRelease(renderPass);

        WGPUCommandBufferDescriptor bufferDescriptor =  WGPUCommandBufferDescriptor.createDirect();
        bufferDescriptor.setNextInChain();
        bufferDescriptor.setLabel("Command Buffer");
        Pointer commandBuffer = wgpu.CommandEncoderFinish(encoder, bufferDescriptor);
        wgpu.CommandEncoderRelease(encoder);


        long[] buffers = new long[1];
        buffers[0] = commandBuffer.address();
        Pointer bufferPtr = WgpuJava.createLongArrayPointer(buffers);
        //System.out.println("Pointer: "+bufferPtr.toString());
        //System.out.println("Submitting command...");
        wgpu.QueueSubmit(queue, 1, bufferPtr);

        wgpu.CommandBufferRelease(commandBuffer);
        //System.out.println("Command submitted...");


        // At the end of the frame
        wgpu.TextureViewRelease(targetView);
        wgpu.SurfacePresent(surface);

        for(int i = 0; i < 10; i++)
            wgpu.DeviceTick(device);
    }

    public void exit(){
        // cleanup
        wgpu.PipelineLayoutRelease(layout);
        wgpu.BindGroupLayoutRelease(bindGroupLayout);
        wgpu.BindGroupRelease(bindGroup);
        wgpu.BufferRelease(indexBuffer);
        wgpu.BufferRelease(vertexBuffer);
        wgpu.BufferRelease(uniformBuffer);
        wgpu.RenderPipelineRelease(pipeline);
        wgpu.SurfaceUnconfigure(surface);
        wgpu.SurfaceRelease(surface);
        wgpu.QueueRelease(queue);
        wgpu.DeviceRelease(device);
        wgpu.InstanceRelease(instance);
    }

    private Pointer getNextSurfaceTextureView() {
        // [...] Get the next surface texture


        WGPUSurfaceTexture surfaceTexture = WGPUSurfaceTexture.createDirect();
        wgpu.SurfaceGetCurrentTexture(surface, surfaceTexture);
        //System.out.println("get current texture: "+surfaceTexture.status.get());
        if(surfaceTexture.getStatus() != WGPUSurfaceGetCurrentTextureStatus.Success){
            System.out.println("*** No current texture");
            return WgpuJava.createNullPointer();
        }
        // [...] Create surface texture view
        WGPUTextureViewDescriptor viewDescriptor = WGPUTextureViewDescriptor.createDirect();
        viewDescriptor.setNextInChain();
        viewDescriptor.setLabel("Surface texture view");
        Pointer tex = surfaceTexture.getTexture();
        WGPUTextureFormat format = wgpu.TextureGetFormat(tex);
        //System.out.println("Set format "+format);
        viewDescriptor.setFormat(format);
        viewDescriptor.setDimension(WGPUTextureViewDimension._2D);
        viewDescriptor.setBaseMipLevel(0);
        viewDescriptor.setMipLevelCount(1);
        viewDescriptor.setBaseArrayLayer(0);
        viewDescriptor.setArrayLayerCount(1);
        viewDescriptor.setAspect(WGPUTextureAspect.All);
        Pointer targetView = wgpu.TextureCreateView(surfaceTexture.getTexture(), viewDescriptor);

        return targetView;
    }


   final static long WGPU_LIMIT_U32_UNDEFINED = 4294967295L;
   final static long WGPU_LIMIT_U64_UNDEFINED = Long.MAX_VALUE;//.   18446744073709551615L;
   // should be 18446744073709551615L but Java longs are signed so it is half that, will it work?
    // todo


    void setDefault(WGPULimits limits) {
        limits.setMaxTextureDimension1D(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxTextureDimension2D(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxTextureDimension3D(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxTextureArrayLayers(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxBindGroups(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxBindGroupsPlusVertexBuffers(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxBindingsPerBindGroup(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxDynamicUniformBuffersPerPipelineLayout(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxDynamicStorageBuffersPerPipelineLayout(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxSampledTexturesPerShaderStage(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxSamplersPerShaderStage(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxStorageBuffersPerShaderStage(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxStorageTexturesPerShaderStage(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxUniformBuffersPerShaderStage(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxUniformBufferBindingSize(WGPU_LIMIT_U64_UNDEFINED);
        limits.setMaxStorageBufferBindingSize(WGPU_LIMIT_U64_UNDEFINED);
        limits.setMinUniformBufferOffsetAlignment(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMinStorageBufferOffsetAlignment(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxVertexBuffers(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxBufferSize(WGPU_LIMIT_U64_UNDEFINED);
        limits.setMaxVertexAttributes(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxVertexBufferArrayStride(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxInterStageShaderComponents(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxInterStageShaderVariables(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxColorAttachments(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxColorAttachmentBytesPerSample(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxComputeWorkgroupStorageSize(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxComputeInvocationsPerWorkgroup(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxComputeWorkgroupSizeX(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxComputeWorkgroupSizeY(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxComputeWorkgroupSizeZ(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxComputeWorkgroupsPerDimension(WGPU_LIMIT_U32_UNDEFINED);
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

}
