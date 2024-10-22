package com.monstrous.wgpu;

//typedef struct WGPUDeviceLostCallbackInfo {
//    WGPUChainedStruct const * nextInChain;
//    WGPUDeviceLostCallback callback;
//    WGPU_NULLABLE void* userdata1;
//    WGPU_NULLABLE void* userdata2;
//} WGPUDeviceLostCallbackInfo WGPU_STRUCTURE_ATTRIBUTE;

import com.monstrous.utils.WgpuJavaStruct;
import jnr.ffi.Struct;

public class WGPUDeviceLostCallbackInfo extends WgpuJavaStruct {
    public final Struct.Pointer nextInChain = new Struct.Pointer();

    private final Struct.Pointer callback = new Struct.Pointer();       // todo callback
    private final Struct.Pointer userdata1 = new Struct.Pointer();
    private final Struct.Pointer userdata2 = new Struct.Pointer();
}
