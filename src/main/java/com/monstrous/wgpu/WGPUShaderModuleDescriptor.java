package com.monstrous.wgpu;

import com.monstrous.utils.CString;
import com.monstrous.utils.WgpuJavaStruct;

public class WGPUShaderModuleDescriptor extends WgpuJavaStruct {
    public final Pointer nextInChain = new Pointer();
    public final Pointer label = new Pointer();

    public java.lang.String getLabel(){
        return CString.fromPointer(label.get());
    }
    public void setLabel(java.lang.String x){
        this.label.set(CString.toPointer(x));
    }
}
