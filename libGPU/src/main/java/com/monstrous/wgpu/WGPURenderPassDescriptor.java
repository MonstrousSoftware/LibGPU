package com.monstrous.wgpu;

import com.monstrous.utils.RustCString;
import com.monstrous.utils.WgpuJavaStruct;
import jnr.ffi.Struct;


//typedef struct WGPURenderPassDescriptor {
//        WGPUChainedStruct const * nextInChain;
//        WGPU_NULLABLE char const * label;
//        size_t colorAttachmentCount;
//        WGPURenderPassColorAttachment const * colorAttachments;
//        WGPU_NULLABLE WGPURenderPassDepthStencilAttachment const * depthStencilAttachment;
//        WGPU_NULLABLE WGPUQuerySet occlusionQuerySet;
//        WGPU_NULLABLE WGPURenderPassTimestampWrites const * timestampWrites;
//        } WGPURenderPassDescriptor WGPU_STRUCTURE_ATTRIBUTE;

public class WGPURenderPassDescriptor extends WgpuJavaStruct {
    public final Pointer nextInChain = new Pointer();

    public final Struct.Pointer label = new Struct.Pointer();
    public final Struct.size_t colorAttachmentCount = new Struct.size_t();
    public final Struct.Pointer colorAttachments = new Struct.Pointer();
    public final Struct.Pointer depthStencilAttachment = new Struct.Pointer();
    public final Struct.Pointer occlusionQuerySet = new Struct.Pointer();
    public final Struct.Pointer timestampWrites = new Struct.Pointer();


    public java.lang.String getLabel(){
        return RustCString.fromPointer(label.get());
    }

    public void setLabel(java.lang.String x){
        this.label.set(RustCString.toPointer(x));
    }
}

