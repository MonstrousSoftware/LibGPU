package com.monstrous;

import com.monstrous.utils.WgpuJava;
import com.monstrous.wgpu.*;
import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;


public class Demo {
    private final String shaderSource = "@vertex\n" +
            "fn vs_main(@builtin(vertex_index) in_vertex_index: u32) -> @builtin(position) vec4f {\n" +
            "    var p = vec2f(0.0, 0.0);\n" +
            "    if (in_vertex_index == 0u) {\n" +
            "        p = vec2f(-0.5, -0.5);\n" +
            "    } else if (in_vertex_index == 1u) {\n" +
            "        p = vec2f(0.5, -0.5);\n" +
            "    } else {\n" +
            "        p = vec2f(0.0, 0.5);\n" +
            "    }\n" +
            "    return vec4f(p, 0.0, 1.0);\n" +
            "}\n" +
            "\n" +
            "@fragment\n" +
            "fn fs_main() -> @location(0) vec4f {\n" +
            "    return vec4f(0.0, 0.4, 1.0, 1.0);\n" +
            "}";


    private static Runtime runtime;
    private WGPU wgpu;
    private Pointer surface;
    private Pointer instance;
    private Pointer device;
    private Pointer queue;
    private Pointer pipeline;
    private WGPUTextureFormat surfaceFormat = WGPUTextureFormat.Undefined;

    public void init(long windowHandle) {
        wgpu = LibraryLoader.create(WGPU.class).load("nativec"); // load the library into the libc variable
        runtime = Runtime.getRuntime(wgpu);
        WgpuJava.setRuntime(runtime);

        // debug test
        System.out.println("Hello world!");
        int sum = wgpu.add(1200, 34);
        System.out.println("sum = "+sum);

        instance = wgpu.CreateInstance();

        System.out.println("window = "+Long.toString(windowHandle, 16));
        surface = wgpu.glfwGetWGPUSurface(instance,  windowHandle);
        System.out.println("surface = "+surface);

        WGPURequestAdapterOptions options = WGPURequestAdapterOptions.createDirect();
        options.setNextInChain();
        options.setCompatibleSurface(surface);

        // Get Adapter
        Pointer adapter = wgpu.RequestAdapterSync(instance, options);

        WGPUSupportedLimits supportedLimits = WGPUSupportedLimits.createDirect();


        wgpu.AdapterGetLimits(adapter, supportedLimits);

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

        // Get Device
        WGPUDeviceDescriptor deviceDescriptor = WGPUDeviceDescriptor.createDirect();
        deviceDescriptor.setNextInChain();
        deviceDescriptor.setLabel("My Device");

        device = wgpu.RequestDeviceSync(adapter, deviceDescriptor);
        wgpu.AdapterRelease(adapter);       // we can release our adapter as soon as we have a device

        // use a lambda expression to define a callback function
        WGPUErrorCallback deviceCallback = (WGPUErrorType type, String message, Pointer userdata) -> {
            System.out.println("*** Device error: "+ type + " : "+message);
        };
        wgpu.DeviceSetUncapturedErrorCallback(device, deviceCallback, null);

        wgpu.DeviceGetLimits(device, supportedLimits);

        System.out.println("maxTextureDimension1D " + supportedLimits.getLimits().getMaxTextureDimension1D());
        System.out.println("maxTextureDimension2D " + supportedLimits.getLimits().getMaxTextureDimension2D());
        System.out.println("maxTextureDimension3D " + supportedLimits.getLimits().getMaxTextureDimension3D());
        System.out.println("maxTextureArrayLayers " + supportedLimits.getLimits().getMaxTextureArrayLayers());

        queue = wgpu.DeviceGetQueue(device);

        // use a lambda expression to define a callback function
        WGPUQueueWorkDoneCallback queueCallback = (WGPUQueueWorkDoneStatus status, Pointer userdata) -> {
            System.out.println("=== Queue work finished with status: "+ status);
        };
        wgpu.QueueOnSubmittedWorkDone(queue, queueCallback, null);


        // configure the surface
        WGPUSurfaceConfiguration config = WGPUSurfaceConfiguration.createDirect();
        config.setNextInChain();

        config.setWidth(640);
        config.setHeight(480);

        surfaceFormat = wgpu.SurfaceGetPreferredFormat(surface, adapter);
        System.out.println("Using format: "+surfaceFormat);
        config.setFormat(surfaceFormat);
        // And we do not need any particular view format:
        config.setViewFormatCount(0);
        config.setViewFormats();
        config.setUsage(WGPUTextureUsage.RenderAttachment);
        config.setDevice(device);
        config.setPresentMode(WGPUPresentMode.Fifo);
        config.setAlphaMode(WGPUCompositeAlphaMode.Auto);

        wgpu.SurfaceConfigure(surface, config);

        initializePipeline();

    }

    public void render(){
        // loop
        Pointer targetView = getNextSurfaceTextureView();
        if (targetView.address() == 0) {
            System.out.println("*** Invalid target view");
            return;
        }

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

        renderPassColorAttachment.getClearValue().setR(0.9);
        renderPassColorAttachment.getClearValue().setG(0.1);
        renderPassColorAttachment.getClearValue().setB(0.2);
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
        wgpu.RenderPassEncoderDraw(renderPass, 3, 1, 0, 0);

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
    }

    public void exit(){
        // cleanup
        wgpu.RenderPipelineRelease(pipeline);
        wgpu.SurfaceUnconfigure(surface);
        wgpu.SurfaceRelease(surface);
        wgpu.QueueRelease(queue);
        wgpu.DeviceRelease(device);
        wgpu.InstanceRelease(instance);
    }

    private Pointer getNextSurfaceTextureView() {
        // [...] Get the next surface texture


        WGPUSurfaceTexture surfaceTexture = new WGPUSurfaceTexture();
        surfaceTexture.useDirectMemory();
        wgpu.SurfaceGetCurrentTexture(surface, surfaceTexture);
        //System.out.println("get current texture: "+surfaceTexture.status.get());
        if(surfaceTexture.status.get() != WGPUSurfaceGetCurrentTextureStatus.Success){
            System.out.println("*** No current texture");
            return WgpuJava.createNullPointer();
        }
        // [...] Create surface texture view
        WGPUTextureViewDescriptor viewDescriptor = new WGPUTextureViewDescriptor();
        viewDescriptor.useDirectMemory();
        viewDescriptor.nextInChain.set(WgpuJava.createNullPointer());
        viewDescriptor.setLabel("Surface texture view");
        Pointer tex = surfaceTexture.texture.get();
        WGPUTextureFormat format = wgpu.TextureGetFormat(tex);
        //System.out.println("Set format "+format);
        viewDescriptor.format.set(format);
        viewDescriptor.dimension.set(WGPUTextureViewDimension._2D);
        viewDescriptor.baseMipLevel.set(0);
        viewDescriptor.mipLevelCount.set(1);
        viewDescriptor.baseArrayLayer.set(0);
        viewDescriptor.arrayLayerCount.set(1);
        viewDescriptor.aspect.set(WGPUTextureAspect.All);
        Pointer targetView = wgpu.TextureCreateView(surfaceTexture.texture.get(), viewDescriptor);

        return targetView;
    }

    void initializePipeline() {

        // Create Shader Module
        WGPUShaderModuleDescriptor shaderDesc = WGPUShaderModuleDescriptor.createDirect();
        shaderDesc.setLabel("My Shader");



        WGPUShaderModuleWGSLDescriptor shaderCodeDesc = WGPUShaderModuleWGSLDescriptor.createDirect();
        shaderCodeDesc.getChain().setNext();
        shaderCodeDesc.getChain().setSType(WGPUSType.ShaderModuleWGSLDescriptor);
        shaderCodeDesc.setCode(shaderSource);

        shaderDesc.getNextInChain().set(shaderCodeDesc.getPointerTo());

        Pointer shaderModule = wgpu.DeviceCreateShaderModule(device, shaderDesc);


        WGPURenderPipelineDescriptor pipelineDesc = WGPURenderPipelineDescriptor.createDirect();
        pipelineDesc.setNextInChain();
        pipelineDesc.setLabel("pipeline");

        pipelineDesc.getVertex().setBufferCount(0);
        pipelineDesc.getVertex().setBuffers();

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
        WGPUBlendState blendState = new WGPUBlendState();
        blendState.useDirectMemory();
        blendState.color.srcFactor.set(WGPUBlendFactor.SrcAlpha);
        blendState.color.dstFactor.set(WGPUBlendFactor.OneMinusSrcAlpha);
        blendState.color.operation.set(WGPUBlendOperation.Add);
        blendState.alpha.srcFactor.set(WGPUBlendFactor.Zero);
        blendState.alpha.dstFactor.set(WGPUBlendFactor.One);
        blendState.alpha.operation.set(WGPUBlendOperation.Add);

        WGPUColorTargetState colorTarget = new WGPUColorTargetState();
        colorTarget.useDirectMemory();
        colorTarget.format.set(surfaceFormat);
        colorTarget.blend.set(blendState.getPointerTo());
        colorTarget.writeMask.set(WGPUColorWriteMask.All);

        fragmentState.setTargetCount(1);
        fragmentState.setTargets(colorTarget);

        pipelineDesc.setFragment(fragmentState);

        pipelineDesc.setDepthStencil();



        pipelineDesc.getMultisample().setCount(1);
        pipelineDesc.getMultisample().setMask( 0xFFFFFFFF);
        pipelineDesc.getMultisample().setAlphaToCoverageEnabled(0);

        pipelineDesc.setLayout(WgpuJava.createNullPointer());

        pipeline = wgpu.DeviceCreateRenderPipeline(device, pipelineDesc);

        wgpu.ShaderModuleRelease(shaderModule);
    }

}
