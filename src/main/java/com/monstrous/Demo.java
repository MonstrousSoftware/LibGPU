package com.monstrous;

import com.monstrous.utils.CString;
import com.monstrous.utils.WgpuJava;
import com.monstrous.wgpu.*;
import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;

import static java.lang.Boolean.FALSE;


public class Demo {
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

        WGPURequestAdapterOptions options = new WGPURequestAdapterOptions();
        options.nextInChain.set(WgpuJava.createNullPointer());
        options.compatibleSurface.set(surface.address());

        // Get Adapter
        Pointer adapter = wgpu.RequestAdapterSync(instance, options);

        WGPUSupportedLimits supportedLimits = new WGPUSupportedLimits();
        supportedLimits.useDirectMemory();


        wgpu.AdapterGetLimits(adapter, supportedLimits);

        System.out.println("maxTextureDimension1D " + supportedLimits.limits.maxTextureDimension1D);
        System.out.println("maxTextureDimension2D " + supportedLimits.limits.maxTextureDimension2D);
        System.out.println("maxTextureDimension3D " + supportedLimits.limits.maxTextureDimension3D);
        System.out.println("maxTextureArrayLayers " + supportedLimits.limits.maxTextureArrayLayers);


        WGPUAdapterProperties adapterProperties = new WGPUAdapterProperties();
        adapterProperties.useDirectMemory();
        adapterProperties.nextInChain.set(WgpuJava.createNullPointer());

        wgpu.AdapterGetProperties(adapter, adapterProperties);

        System.out.println("VendorID: " + adapterProperties.vendorID);
        System.out.println("Vendor name: " + CString.fromPointer(adapterProperties.vendorName.get()));
        System.out.println("Device ID: " + adapterProperties.deviceID);
        System.out.println("Back end: " + adapterProperties.backendType);

        // Get Device
        WGPUDeviceDescriptor deviceDescriptor = new WGPUDeviceDescriptor();
        deviceDescriptor.nextInChain.set(WgpuJava.createNullPointer());
        deviceDescriptor.setLabel("My Device");

        device = wgpu.RequestDeviceSync(adapter, deviceDescriptor);
        System.out.println("Got device = "+device.toString());
        wgpu.AdapterRelease(adapter);       // we can release our adapter as soon as we have a device

        // use a lambda expression to define a callback function
        WGPUErrorCallback deviceCallback = (WGPUErrorType type, String message, Pointer userdata) -> {
            System.out.println("*** Device error: "+ type + " : "+message);
        };
        wgpu.DeviceSetUncapturedErrorCallback(device, deviceCallback, null);




        wgpu.DeviceGetLimits(device, supportedLimits);

        System.out.println("maxTextureDimension1D " + supportedLimits.limits.maxTextureDimension1D);
        System.out.println("maxTextureDimension2D " + supportedLimits.limits.maxTextureDimension2D);
        System.out.println("maxTextureDimension3D " + supportedLimits.limits.maxTextureDimension3D);
        System.out.println("maxTextureArrayLayers " + supportedLimits.limits.maxTextureArrayLayers);

        queue = wgpu.DeviceGetQueue(device);
        System.out.println("Got queue = "+queue.toString());

        // use a lambda expression to define a callback function
        WGPUQueueWorkDoneCallback queueCallback = (WGPUQueueWorkDoneStatus status, Pointer userdata) -> {
            System.out.println("=== Queue work finished with status: "+ status);
        };
        wgpu.QueueOnSubmittedWorkDone(queue, queueCallback, null);


        // configure the surface
        WGPUSurfaceConfiguration config = new WGPUSurfaceConfiguration();
        config.nextInChain.set(WgpuJava.createNullPointer());

        config.width.set(640);
        config.height.set(480);

        surfaceFormat = wgpu.SurfaceGetPreferredFormat(surface, adapter);
        System.out.println("Using format: "+surfaceFormat);
        config.format.set(surfaceFormat);
        // And we do not need any particular view format:
        config.viewFormatCount.set(0);
        config.viewFormats.set(WgpuJava.createNullPointer());
        config.usage.set(WGPUTextureUsage.RenderAttachment);
        config.device.set(device);
        config.presentMode.set(WGPUPresentMode.Fifo);
        config.alphaMode.set(WGPUCompositeAlphaMode.Auto);

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

        WGPUCommandEncoderDescriptor encoderDescriptor = new WGPUCommandEncoderDescriptor();
        encoderDescriptor.useDirectMemory();
        encoderDescriptor.nextInChain.set(WgpuJava.createNullPointer());
        encoderDescriptor.setLabel("My Encoder");

        Pointer encoder = wgpu.DeviceCreateCommandEncoder(device, encoderDescriptor);




        WGPURenderPassColorAttachment renderPassColorAttachment = new WGPURenderPassColorAttachment();
        renderPassColorAttachment.useDirectMemory();
        renderPassColorAttachment.nextInChain.set(WgpuJava.createNullPointer());
        renderPassColorAttachment.view.set(targetView);
        renderPassColorAttachment.resolveTarget.set(WgpuJava.createNullPointer());
        renderPassColorAttachment.loadOP.set(WGPULoadOp.Clear);
        renderPassColorAttachment.storeOP.set(WGPUStoreOp.Store);

        // todo find smarter way
        renderPassColorAttachment.clearValue.r.set(0.9);
        renderPassColorAttachment.clearValue.g.set(0.1);
        renderPassColorAttachment.clearValue.b.set(0.2);
        renderPassColorAttachment.clearValue.a.set(1.0);

        renderPassColorAttachment.depthSlice.set(wgpu.WGPU_DEPTH_SLICE_UNDEFINED);

        WGPURenderPassDescriptor renderPassDescriptor = new WGPURenderPassDescriptor();
        renderPassDescriptor.useDirectMemory();
        renderPassDescriptor.nextInChain.set(WgpuJava.createNullPointer());

        renderPassDescriptor.colorAttachmentCount.set(1);
        renderPassDescriptor.colorAttachments.set( renderPassColorAttachment.getPointerTo());
        renderPassDescriptor.occlusionQuerySet.set(WgpuJava.createNullPointer());
        renderPassDescriptor.depthStencilAttachment.set(WgpuJava.createNullPointer());
        renderPassDescriptor.timestampWrites.set(WgpuJava.createNullPointer());

        Pointer renderPass = wgpu.CommandEncoderBeginRenderPass(encoder, renderPassDescriptor);
// [...] Use Render Pass

        wgpu.RenderPassEncoderSetPipeline(renderPass, pipeline);
        wgpu.RenderPassEncoderDraw(renderPass, 3, 1, 0, 0);

        wgpu.RenderPassEncoderEnd(renderPass);
        wgpu.RenderPassEncoderRelease(renderPass);

        WGPUCommandBufferDescriptor bufferDescriptor = new WGPUCommandBufferDescriptor();
        bufferDescriptor.useDirectMemory();
        bufferDescriptor.nextInChain.set(WgpuJava.createNullPointer());
        bufferDescriptor.setLabel("Command Buffer");
        Pointer commandBuffer = wgpu.CommandEncoderFinish(encoder, bufferDescriptor);
        wgpu.CommandEncoderRelease(encoder);



        //wgpu.CommandEncoderInsertDebugMarker(encoder, "foobar");


        long[] buffers = new long[1];
        buffers[0] = commandBuffer.address();
        Pointer bufferPtr = WgpuJava.createLongArrayPointer(buffers);
        System.out.println("Pointer: "+bufferPtr.toString());
        System.out.println("Submitting command...");
        wgpu.QueueSubmit(queue, 1, bufferPtr);

        wgpu.CommandBufferRelease(commandBuffer);
        System.out.println("Command submitted...");

        // there is no tick or poll defined in webgpu.h

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
        System.out.println("get current texture: "+surfaceTexture.status.get());
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
        System.out.println("Set format "+format);
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
        WGPUShaderModuleDescriptor shaderDesc = new WGPUShaderModuleDescriptor();
        shaderDesc.useDirectMemory();
        shaderDesc.setLabel("My Shader");

        String shaderSource = "@vertex\n" +
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

        WGPUShaderModuleWGSLDescriptor shaderCodeDesc = new WGPUShaderModuleWGSLDescriptor();
        shaderCodeDesc.useDirectMemory();
        shaderCodeDesc.next.set(WgpuJava.createNullPointer());
        shaderCodeDesc.sType.set(WGPUSType.ShaderModuleWGSLDescriptor);
        shaderCodeDesc.setCode(shaderSource);

        shaderDesc.nextInChain.set(shaderCodeDesc.getPointerTo());

        Pointer shaderModule = wgpu.DeviceCreateShaderModule(device, shaderDesc);


        WGPURenderPipelineDescriptor pipelineDesc = new WGPURenderPipelineDescriptor();
        pipelineDesc.useDirectMemory();
        pipelineDesc.nextInChain.set(WgpuJava.createNullPointer());
        pipelineDesc.setLabel("pipeline");

        pipelineDesc.vertex.bufferCount.set(0);
        pipelineDesc.vertex.buffers.set(WgpuJava.createNullPointer());

        pipelineDesc.vertex.module.set(shaderModule);
        pipelineDesc.vertex.setEntryPoint("vs_main");
        pipelineDesc.vertex.constantCount.set(0);
        pipelineDesc.vertex.constants.set(WgpuJava.createNullPointer());

        pipelineDesc.primitive.topology.set(WGPUPrimitiveTopology.TriangleList);
        pipelineDesc.primitive.stripIndexFormat.set(WGPUIndexFormat.Undefined);
        pipelineDesc.primitive.frontFace.set(WGPUFrontFace.CCW);
        pipelineDesc.primitive.cullMode.set(WGPUCullMode.None);

        WGPUFragmentState fragmentState = new WGPUFragmentState();
        fragmentState.useDirectMemory();
        fragmentState.nextInChain.set(WgpuJava.createNullPointer());
        fragmentState.module.set(shaderModule);
        fragmentState.setEntryPoint("fs_main");
        fragmentState.constantCount.set(0);
        fragmentState.constants.set(WgpuJava.createNullPointer());

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

        fragmentState.targetCount.set(1);
        fragmentState.targets.set(colorTarget.getPointerTo());

        pipelineDesc.fragment.set(fragmentState.getPointerTo());

        pipelineDesc.depthStencil.set(WgpuJava.createNullPointer());




        pipelineDesc.multisample.count.set(1);
        pipelineDesc.multisample.mask.set( 0xFFFFFFFF);
        pipelineDesc.multisample.alphaToCoverageEnabled.set(FALSE);

        pipelineDesc.layout.set(WgpuJava.createNullPointer());

        pipeline = wgpu.DeviceCreateRenderPipeline(device, pipelineDesc);

        wgpu.ShaderModuleRelease(shaderModule);
    }

}
