package com.monstrous.wgpu;


//typedef struct WGPUColorTargetState {
//    WGPUChainedStruct const * nextInChain;
//    WGPUTextureFormat format;
//    WGPU_NULLABLE WGPUBlendState const * blend;
//    WGPUColorWriteMaskFlags writeMask;
//} WGPUColorTargetState WGPU_STRUCTURE_ATTRIBUTE;

import com.monstrous.utils.WgpuJavaStruct;
import jnr.ffi.Struct;

public class WGPUColorTargetState extends WgpuJavaStruct {
    public final Pointer nextInChain = new Pointer();
    public final Struct.Enum32<WGPUTextureFormat> format = new Struct.Enum32<>(WGPUTextureFormat.class);
    public final Pointer blend = new Pointer();
    public final Struct.Unsigned32 writeMask = new Unsigned32();
}
