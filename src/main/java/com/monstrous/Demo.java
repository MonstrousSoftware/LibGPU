package com.monstrous;

import com.monstrous.utils.CString;
import com.monstrous.utils.WgpuJava;
import com.monstrous.wgpu.*;
import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;


public class Demo {
    private static Runtime runtime;
    private WGPU wgpu;
    private Pointer surface;
    private Pointer instance;
    private Pointer device;
    private Pointer queue;


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


        WGPUSurfaceConfiguration config = new WGPUSurfaceConfiguration();
        config.nextInChain.set(WgpuJava.createNullPointer());

        config.width.set(640);
        config.height.set(480);

        WGPUTextureFormat surfaceFormat = wgpu.SurfaceGetPreferredFormat(surface, adapter);
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

        // loop
        Pointer targetView = getNextSurfaceTextureView();
        if (targetView.address() == 0)
            System.out.println("*** Invalid target view");  // break


        // At the end of the frame
        wgpu.TextureViewRelease(targetView);

        WGPUCommandEncoderDescriptor encoderDescriptor = new WGPUCommandEncoderDescriptor();
        encoderDescriptor.nextInChain.set(WgpuJava.createNullPointer());
        encoderDescriptor.setLabel("My Encoder");

        Pointer encoder = wgpu.DeviceCreateCommandEncoder(device, encoderDescriptor);

        wgpu.CommandEncoderInsertDebugMarker(encoder, "foobar");


        WGPUCommandBufferDescriptor bufferDescriptor = new WGPUCommandBufferDescriptor();
        bufferDescriptor.nextInChain.set(WgpuJava.createNullPointer());
        bufferDescriptor.setLabel("My Buffer");
        Pointer commandBuffer = wgpu.CommandEncoderFinish(encoder, bufferDescriptor);
        wgpu.CommandEncoderRelease(encoder);

        long[] buffers = new long[1];
        buffers[0] = commandBuffer.address();

        Pointer bufferPtr = WgpuJava.createLongArrayPointer(buffers);
        System.out.println("Pointer: "+bufferPtr.toString());
        System.out.println("Submitting command...");
        wgpu.QueueSubmit(queue, 1, bufferPtr);

        wgpu.CommandBufferRelease(commandBuffer);
        System.out.println("Command submitted...");

        // there is no tick or poll defined in webgpu.h
    }

    public void render(){

    }

    public void exit(){
        // cleanup
        wgpu.SurfaceUnconfigure(surface);
        wgpu.SurfaceRelease(surface);
        wgpu.QueueRelease(queue);
        wgpu.DeviceRelease(device);
        wgpu.InstanceRelease(instance);
    }

    private Pointer getNextSurfaceTextureView() {
        // [...] Get the next surface texture


        WGPUSurfaceTexture surfaceTexture = new WGPUSurfaceTexture();
        wgpu.SurfaceGetCurrentTexture(surface, surfaceTexture);
        if(surfaceTexture.status.get() != WGPUSurfaceGetCurrentTextureStatus.Success){
            return WgpuJava.createNullPointer();
        }
        // [...] Create surface texture view
        WGPUTextureViewDescriptor viewDescriptor = new WGPUTextureViewDescriptor();
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

}
