package com.monstrous.wgpu;

import com.monstrous.utils.WgpuJavaStruct;
import jnr.ffi.Struct;

public class WGPUSurfaceConfiguration extends WgpuJavaStruct {
    public final Struct.Pointer nextInChain = new Struct.Pointer();
    public final Struct.Pointer device = new Struct.Pointer();
    public final Struct.Enum32<WGPUTextureFormat> format = new Enum32<>(WGPUTextureFormat.class);
    public final Struct.Unsigned32 usage = new Struct.Unsigned32();
    public final Struct.size_t viewFormatCount = new Struct.size_t();
    public final Struct.Pointer viewFormats = new Struct.Pointer();
    public final Struct.Enum32<WGPUCompositeAlphaMode> alphaMode = new Enum32<>(WGPUCompositeAlphaMode.class);
    public final Struct.Unsigned32 width = new Struct.Unsigned32();
    public final Struct.Unsigned32 height = new Struct.Unsigned32();
    public final Struct.Enum32<WGPUPresentMode> presentMode = new Enum32<>(WGPUPresentMode.class);

}
