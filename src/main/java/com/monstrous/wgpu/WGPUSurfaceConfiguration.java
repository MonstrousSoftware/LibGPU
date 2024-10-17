package com.monstrous.wgpu;

import com.monstrous.utils.WgpuJavaStruct;
import jnr.ffi.Struct;

public class WGPUSurfaceConfiguration extends WgpuJavaStruct {
    public final Struct.Pointer nextInChain = new Struct.Pointer();
    public final Struct.Pointer device = new Struct.Pointer();
    WGPUTextureFormat format;
    WGPUTextureUsageFlags usage;
    size_t viewFormatCount;
    WGPUTextureFormat const * viewFormats;
    WGPUCompositeAlphaMode alphaMode;
    uint32_t width;
    uint32_t height;
    WGPUPresentMode presentMode;

}
