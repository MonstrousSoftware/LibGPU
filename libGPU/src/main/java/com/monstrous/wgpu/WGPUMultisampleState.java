package com.monstrous.wgpu;

//typedef struct WGPUMultisampleState {
//    WGPUChainedStruct const * nextInChain;
//    uint32_t count;
//    uint32_t mask;
//    WGPUBool alphaToCoverageEnabled;
//} WGPUMultisampleState WGPU_STRUCTURE_ATTRIBUTE;

import com.monstrous.utils.WgpuJavaStruct;
import jnr.ffi.Struct;

public class WGPUMultisampleState extends WgpuJavaStruct {
    public final Pointer nextInChain = new Pointer();
    public final Struct.Unsigned32 count = new Unsigned32();
    public final Struct.Unsigned32 mask = new Unsigned32();
    public final Struct.WBOOL alphaToCoverageEnabled = new WBOOL();
}
