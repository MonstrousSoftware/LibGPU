package org.example.wgpu;

//typedef struct WGPUSupportedFeatures {
//    WGPUChainedStructOut * nextInChain;
//    size_t featureCount;
//    WGPUFeatureName const * features;
//} WGPUSupportedFeatures WGPU_STRUCTURE_ATTRIBUTE;

import jnr.ffi.Struct;
import org.example.WgpuJavaStruct;

public class WGPUSupportedFeatures extends WgpuJavaStruct {
    public final Pointer nextInChain = new Pointer();
    public final Struct.size_t featureCount = new Struct.size_t();
    public final Struct.Pointer features = new Struct.Pointer();        // todo map to array?
}
