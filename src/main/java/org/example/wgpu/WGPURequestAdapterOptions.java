package org.example.wgpu;

import jnr.ffi.Struct;
import org.example.WgpuJavaStruct;

public class WGPURequestAdapterOptions extends WgpuJavaStruct {
    public final Pointer nextInChain = new Pointer();

    public final Pointer compatibleSurface = new Pointer();
    public final Struct.Enum<WGPUPowerPreference> powerPreference = new Struct.Enum<>(WGPUPowerPreference.class);
    public final Struct.Enum<WGPUBackendType> backendType = new Struct.Enum<>(WGPUBackendType.class);
    public final WBOOL forceFallbackAdapter = new WBOOL();
}
