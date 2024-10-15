package org.example;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import org.example.wgpu.*;


public class MainJNR {
    private static Runtime runtime;

    public static void main(String[] args) {
        MainJNR main = new MainJNR();
        main.runTest();
    }

    public void runTest() {
        WGPU wgpu = LibraryLoader.create(WGPU.class).load("nativec"); // load the library into the libc variable
        runtime = Runtime.getRuntime(wgpu);
        WgpuJava.setRuntime(runtime);

        // debug test
        System.out.println("Hello world!");
        int sum = wgpu.add(1200, 34);
        System.out.println("sum = "+sum);


        WGPURequestAdapterOptions options = new WGPURequestAdapterOptions();
        options.backendType.set(WGPUBackendType.Undefined);
        options.forceFallbackAdapter.set(true);
        options.powerPreference.set(WGPUPowerPreference.HighPerformance);

        Pointer instance = wgpu.CreateInstance();
        Pointer adapter = wgpu.RequestAdapterSync(instance, options);

        WGPUSupportedLimits supportedLimits = new WGPUSupportedLimits();
        supportedLimits.useDirectMemory();

        wgpu.AdapterGetLimits(adapter, supportedLimits);

        System.out.println("maxTextureDimension1D " + supportedLimits.limits.maxTextureDimension1D);
        System.out.println("maxTextureDimension2D " + supportedLimits.limits.maxTextureDimension2D);
        System.out.println("maxTextureDimension3D " + supportedLimits.limits.maxTextureDimension3D);
        System.out.println("maxTextureArrayLayers " + supportedLimits.limits.maxTextureArrayLayers);


        WGPUDeviceDescriptor deviceDescriptor = new WGPUDeviceDescriptor();
        deviceDescriptor.nextInChain.set(WgpuJava.createNullPointer());
        deviceDescriptor.setLabel("My Device");

        Pointer device = wgpu.RequestDeviceSync(adapter, deviceDescriptor);
        System.out.println("Got device = "+device.toString());
        wgpu.AdapterRelease(adapter);       // we can release our adapter as soon as we have a device


        wgpu.DeviceGetLimits(device, supportedLimits);

        System.out.println("maxTextureDimension1D " + supportedLimits.limits.maxTextureDimension1D);
        System.out.println("maxTextureDimension2D " + supportedLimits.limits.maxTextureDimension2D);
        System.out.println("maxTextureDimension3D " + supportedLimits.limits.maxTextureDimension3D);
        System.out.println("maxTextureArrayLayers " + supportedLimits.limits.maxTextureArrayLayers);

        Pointer queue = wgpu.DeviceGetQueue(device);
        System.out.println("Got queue = "+queue.toString());


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

        // cleanup
        wgpu.QueueRelease(queue);
        wgpu.DeviceRelease(device);
        wgpu.InstanceRelease(instance);

    }

}
