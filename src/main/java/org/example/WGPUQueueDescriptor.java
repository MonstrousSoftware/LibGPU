package org.example;

//typedef struct WGPUQueueDescriptor {
//    WGPUChainedStruct const * nextInChain;
//    /**
//     * If the null value is passed, this defaults to the empty string.
//     */
//    WGPUStringView label;
//} WGPUQueueDescriptor WGPU_STRUCTURE_ATTRIBUTE;

import jnr.ffi.Struct;

public class WGPUQueueDescriptor extends WgpuJavaStruct {
    public final Pointer nextInChain = new Pointer();

    private final Struct.Pointer label = new Struct.Pointer();
}
