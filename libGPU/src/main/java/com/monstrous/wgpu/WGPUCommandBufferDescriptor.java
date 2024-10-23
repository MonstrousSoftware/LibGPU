package com.monstrous.wgpu;


import com.monstrous.utils.WgpuJavaStruct;
import com.monstrous.utils.RustCString;

public class WGPUCommandBufferDescriptor extends WgpuJavaStruct {
    public final Pointer nextInChain = new Pointer();

    public final Pointer label = new Pointer();

    public java.lang.String getLabel(){
        return RustCString.fromPointer(label.get());
    }

    public void setLabel(java.lang.String x){
        this.label.set(RustCString.toPointer(x));
    }
}
