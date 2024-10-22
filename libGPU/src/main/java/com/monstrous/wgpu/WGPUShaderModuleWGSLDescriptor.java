package com.monstrous.wgpu;

import com.monstrous.utils.CString;
import com.monstrous.utils.WgpuJavaStruct;
import jnr.ffi.Struct;

//typedef struct WGPUShaderModuleWGSLDescriptor {
//    WGPUChainedStruct chain;
//    char const * code;
//} WGPUShaderModuleWGSLDescriptor WGPU_STRUCTURE_ATTRIBUTE;

public class WGPUShaderModuleWGSLDescriptor extends WgpuJavaStruct {
    public final Pointer next = new Pointer();
    public final Struct.Enum32<WGPUSType> sType = new Struct.Enum32<>(WGPUSType.class);
    public final Pointer code = new Pointer();

    public java.lang.String getCode(){
        return CString.fromPointer(code.get());
    }
    public void setCode(java.lang.String x){
        this.code.set(CString.toPointer(x));
    }
}