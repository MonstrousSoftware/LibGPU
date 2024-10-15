package org.example;

import jnr.ffi.Struct;

public class WGPURequestAdapterOptions extends WgpuJavaStruct {
    public final Pointer nextInChain = new Pointer();

    public final Pointer compatibleSurface = new Pointer();
    public final Struct.Enum<MainJNR.WGPUPowerPreference> powerPreference = new Struct.Enum<>(MainJNR.WGPUPowerPreference.class);
    public final Struct.Enum<MainJNR.WGPUBackendType> backendType = new Struct.Enum<>(MainJNR.WGPUBackendType.class);
    public final WBOOL forceFallbackAdapter = new WBOOL();
}
