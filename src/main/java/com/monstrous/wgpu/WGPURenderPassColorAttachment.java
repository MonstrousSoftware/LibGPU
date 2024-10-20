package com.monstrous.wgpu;


//typedef struct WGPURenderPassColorAttachment {
//        WGPUChainedStruct const * nextInChain;
//        WGPU_NULLABLE WGPUTextureView view;
//        uint32_t depthSlice;
//        WGPU_NULLABLE WGPUTextureView resolveTarget;
//        WGPULoadOp loadOp;
//        WGPUStoreOp storeOp;
//        WGPUColor clearValue;
//        } WGPURenderPassColorAttachment WGPU_STRUCTURE_ATTRIBUTE;

import com.monstrous.utils.WgpuJavaStruct;
import jnr.ffi.Struct;

public class WGPURenderPassColorAttachment extends WgpuJavaStruct {
    public final Pointer nextInChain = new Pointer();

    public final Struct.Pointer view = new Struct.Pointer();
    public final Struct.Unsigned32 depthSlice = new Struct.Unsigned32();
    public final Struct.Pointer resolveTarget = new Struct.Pointer();
    public final Struct.Enum32<WGPULoadOp> loadOP = new Struct.Enum32<>(WGPULoadOp.class);
    public final Struct.Enum32<WGPUStoreOp> storeOP = new Struct.Enum32<>(WGPUStoreOp.class);
    public final Struct.Pointer clearValue = new Struct.Pointer();
}
