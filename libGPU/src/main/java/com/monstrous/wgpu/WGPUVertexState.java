package com.monstrous.wgpu;

import com.monstrous.utils.RustCString;
import com.monstrous.utils.WgpuJavaStruct;
import jnr.ffi.Struct;

//typedef struct WGPUVertexState {
//    WGPUChainedStruct const * nextInChain;
//    WGPUShaderModule module;
//    WGPU_NULLABLE char const * entryPoint;
//    size_t constantCount;
//    WGPUConstantEntry const * constants;
//    size_t bufferCount;
//    WGPUVertexBufferLayout const * buffers;
//} WGPUVertexState WGPU_STRUCTURE_ATTRIBUTE;

public class WGPUVertexState  extends WgpuJavaStruct {
    public final Pointer nextInChain = new Pointer();
    public final Pointer module = new Pointer();
    public final Pointer entryPoint = new Pointer();
    public final Struct.size_t constantCount = new Struct.size_t();
    public final Pointer constants = new Pointer();
    public final Struct.size_t bufferCount = new Struct.size_t();
    public final Pointer buffers = new Pointer();

    public java.lang.String getEntryPoint(){
        return RustCString.fromPointer(entryPoint.get());
    }
    public void setEntryPoint(java.lang.String x){ this.entryPoint.set(RustCString.toPointer(x)); }

}
