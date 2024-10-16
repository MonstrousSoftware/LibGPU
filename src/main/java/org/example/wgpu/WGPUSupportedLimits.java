package org.example.wgpu;

//        typedef struct WGPUSupportedLimits {
//            WGPUChainedStructOut * nextInChain;
//            WGPULimits limits;                                    // not a pointer but an inner struct!!!
//        } WGPUSupportedLimits WGPU_STRUCTURE_ATTRIBUTE;

import org.example.utils.WgpuJavaStruct;

public final class WGPUSupportedLimits extends WgpuJavaStruct {
    public final Pointer nextInChain = new Pointer();
    public final WGPULimits limits = inner(new WGPULimits());

}