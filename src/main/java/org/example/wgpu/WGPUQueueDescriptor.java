package org.example.wgpu;

//typedef struct WGPUQueueDescriptor {
//    WGPUChainedStruct const * nextInChain;
//    /**
//     * If the null value is passed, this defaults to the empty string.
//     */
//    WGPUStringView label;
//} WGPUQueueDescriptor WGPU_STRUCTURE_ATTRIBUTE;

import jnr.ffi.Struct;
import org.example.utils.RustCString;
import org.example.utils.WgpuJavaStruct;

public class WGPUQueueDescriptor extends WgpuJavaStruct {
    public final Pointer nextInChain = new Pointer();

    private final Struct.Pointer label = new Struct.Pointer();

    public java.lang.String getLabel(){
        return RustCString.fromPointer(label.get());
    }

    public void setLabel(java.lang.String x){
        this.label.set(RustCString.toPointer(x));
    }
}
