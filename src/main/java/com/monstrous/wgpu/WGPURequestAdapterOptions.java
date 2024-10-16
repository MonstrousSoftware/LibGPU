package com.monstrous.wgpu;

import com.monstrous.utils.WgpuJavaStruct;
import jnr.ffi.Struct;

public class WGPURequestAdapterOptions extends WgpuJavaStruct {
    public final Struct.Pointer nextInChain = new Struct.Pointer();

    public final Struct.Pointer compatibleSurface = new Struct.Pointer();
    public final Struct.Enum<WGPUPowerPreference> powerPreference = new Struct.Enum<>(WGPUPowerPreference.class);
    public final Struct.Enum<WGPUBackendType> backendType = new Struct.Enum<>(WGPUBackendType.class);
    public final WBOOL forceFallbackAdapter = new WBOOL();
}
