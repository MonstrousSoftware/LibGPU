package com.monstrous.wgpu;

import com.monstrous.utils.CString;
import com.monstrous.utils.WgpuJavaStruct;

//typedef struct WGPUCommandEncoderDescriptor {
//    WGPUChainedStruct const * nextInChain;
//    /**
//     * If the null value is passed, this defaults to the empty string.
//     */
//    WGPUStringView label;
//} WGPUCommandEncoderDescriptor WGPU_STRUCTURE_ATTRIBUTE;

public class WGPUCommandEncoderDescriptor extends WgpuJavaStruct {
    public final Pointer nextInChain = new Pointer();

    public final Pointer label = new Pointer();

    public java.lang.String getLabel(){
        return CString.fromPointer(label.get());
    }
    public void setLabel(java.lang.String x){
        this.label.set(CString.toPointer(x));
    }
}
