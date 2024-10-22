package com.monstrous.wgpu;

import com.monstrous.utils.WgpuJavaStruct;
import jnr.ffi.Struct;

public class WGPUAdapterProperties extends WgpuJavaStruct {
    public final Pointer nextInChain = new Pointer();

    public final Struct.Unsigned32 vendorID = new Unsigned32();
    public final Struct.Pointer vendorName = new Struct.Pointer();
    public final Struct.Pointer architecture = new Struct.Pointer();
    public final Struct.Unsigned32  deviceID = new Unsigned32();
    public final Struct.Pointer name = new Struct.Pointer();
    public final Struct.Pointer driverDescription = new Struct.Pointer();
    public final Struct.Enum<WGPUAdapterType> adapterType = new Struct.Enum<>(WGPUAdapterType.class);
    public final Struct.Enum<WGPUBackendType> backendType = new Struct.Enum<>(WGPUBackendType.class);
}
