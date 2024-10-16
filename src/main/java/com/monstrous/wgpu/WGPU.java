package com.monstrous.wgpu;

import jnr.ffi.Pointer;

public interface WGPU { // A representation of the C interface in Java

    int add(int a, int b);

    void testStruct(WGPURequestAdapterOptions options);
    void testLimitsStruct(WGPUSupportedLimits supported);

    Pointer CreateInstance();
    void InstanceRelease(Pointer instance);

    Pointer RequestAdapterSync(Pointer instance, WGPURequestAdapterOptions options);
    void AdapterRelease(Pointer adapter);
    boolean    AdapterGetLimits(Pointer adapter, WGPUSupportedLimits limits);
    void AdapterGetProperties(Pointer adapter, WGPUAdapterProperties properties);

    Pointer RequestDeviceSync(Pointer adapter, WGPUDeviceDescriptor descriptor);
    void DeviceRelease(Pointer device);
    //WGPUStatus DeviceGetFeatures(Pointer device, WGPUSupportedFeatures features);
    boolean DeviceGetLimits(Pointer device, WGPUSupportedLimits limits);
    void DeviceSetUncapturedErrorCallback(Pointer device, WGPUErrorCallback callback, Pointer userdata);

    Pointer DeviceGetQueue(Pointer device);
    void QueueRelease(Pointer queue);
    void QueueSubmit(Pointer queue, int count, Pointer commandBuffer);       // array of command buffer
    void QueueOnSubmittedWorkDone(Pointer queue, WGPUQueueWorkDoneCallback callback, Pointer userdata);

    Pointer DeviceCreateCommandEncoder(Pointer device, WGPUCommandEncoderDescriptor encoderDesc);
    void CommandEncoderRelease(Pointer commandEncoder);
    void CommandEncoderInsertDebugMarker(Pointer encoder, String marker);

    Pointer CommandEncoderFinish(Pointer encoder, WGPUCommandBufferDescriptor cmdBufferDescriptor);
    void CommandBufferRelease(Pointer commandBuffer);

    Pointer glfwGetWGPUSurface(Pointer instance, Pointer GLFWwindow);

}