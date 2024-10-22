package com.monstrous.wgpu;


//typedef struct WGPUFragmentState {
//    WGPUChainedStruct const * nextInChain;
//    WGPUShaderModule module;
//    WGPU_NULLABLE char const * entryPoint;
//    size_t constantCount;
//    WGPUConstantEntry const * constants;
//    size_t targetCount;
//    WGPUColorTargetState const * targets;
//} WGPUFragmentState WGPU_STRUCTURE_ATTRIBUTE;

import com.monstrous.utils.CString;
import com.monstrous.utils.WgpuJavaStruct;
import jnr.ffi.Struct;

public class WGPUFragmentState  extends WgpuJavaStruct {
    public final Pointer nextInChain = new Pointer();
    public final Pointer module = new Pointer();
    public final Pointer entryPoint = new Pointer();
    public final Struct.size_t constantCount = new Struct.size_t();
    public final Pointer constants = new Pointer();
    public final Struct.size_t targetCount = new Struct.size_t();
    public final Pointer targets = new Pointer();

    public java.lang.String getEntryPoint(){
        return CString.fromPointer(entryPoint.get());
    }
    public void setEntryPoint(java.lang.String x){ this.entryPoint.set(CString.toPointer(x)); }
}
