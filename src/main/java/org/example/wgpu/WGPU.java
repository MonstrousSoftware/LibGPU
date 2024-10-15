package org.example.wgpu;

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

    Pointer RequestDeviceSync(Pointer adapter, WGPUDeviceDescriptor descriptor);
    void DeviceRelease(Pointer device);

    //WGPUStatus DeviceGetFeatures(Pointer device, WGPUSupportedFeatures features);

    boolean DeviceGetLimits(Pointer device, WGPUSupportedLimits limits);
}