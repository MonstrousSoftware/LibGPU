package com.monstrous.wgpu;

import com.monstrous.utils.WgpuJavaStruct;
import jnr.ffi.Struct;

//typedef struct WGPUPrimitiveState {
//    WGPUChainedStruct const * nextInChain;
//    WGPUPrimitiveTopology topology;
//    WGPUIndexFormat stripIndexFormat;
//    WGPUFrontFace frontFace;
//    WGPUCullMode cullMode;
//} WGPUPrimitiveState WGPU_STRUCTURE_ATTRIBUTE;


public class WGPUPrimitiveState  extends WgpuJavaStruct {
    public final Pointer nextInChain = new Pointer();
    public final Struct.Enum32<WGPUPrimitiveTopology> topology = new Enum32<>(WGPUPrimitiveTopology.class);
    public final Struct.Enum32<WGPUIndexFormat> stripIndexFormat = new Enum32<>(WGPUIndexFormat.class);
    public final Struct.Enum32<WGPUFrontFace> frontFace = new Enum32<>(WGPUFrontFace.class);
    public final Struct.Enum32<WGPUCullMode> cullMode = new Enum32<>(WGPUCullMode.class);
}
