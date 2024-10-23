package com.monstrous.wgpu;

//typedef struct WGPUTextureViewDescriptor {
//        WGPUChainedStruct const * nextInChain;
//        WGPU_NULLABLE char const * label;
//        WGPUTextureFormat format;
//        WGPUTextureViewDimension dimension;
//        uint32_t baseMipLevel;
//        uint32_t mipLevelCount;
//        uint32_t baseArrayLayer;
//        uint32_t arrayLayerCount;
//        WGPUTextureAspect aspect;
//        } WGPUTextureViewDescriptor WGPU_STRUCTURE_ATTRIBUTE;


import com.monstrous.utils.RustCString;
import com.monstrous.utils.WgpuJavaStruct;
import jnr.ffi.Struct;

public class WGPUTextureViewDescriptor extends WgpuJavaStruct {
    public final Struct.Pointer nextInChain = new Struct.Pointer();

    public final Struct.Pointer label = new Struct.Pointer();
    public final Struct.Enum32<WGPUTextureFormat> format = new Struct.Enum32<>(WGPUTextureFormat.class);
    public final Struct.Enum32<WGPUTextureViewDimension> dimension  = new Struct.Enum32<>(WGPUTextureViewDimension.class);
    public final Struct.Unsigned32 baseMipLevel = new Unsigned32();
    public final Struct.Unsigned32 mipLevelCount = new Unsigned32();
    public final Struct.Unsigned32 baseArrayLayer = new Unsigned32();
    public final Struct.Unsigned32 arrayLayerCount = new Unsigned32();
    public final Struct.Enum32<WGPUTextureAspect> aspect = new Struct.Enum32<>(WGPUTextureAspect.class);


    public java.lang.String getLabel(){
        return RustCString.fromPointer(label.get());
    }

    public void setLabel(java.lang.String x){
        this.label.set(RustCString.toPointer(x));
    }

}
