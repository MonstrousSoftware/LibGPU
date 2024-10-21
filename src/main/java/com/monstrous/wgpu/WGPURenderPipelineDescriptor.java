package com.monstrous.wgpu;

import com.monstrous.utils.CString;
import com.monstrous.utils.WgpuJavaStruct;

//typedef struct WGPURenderPipelineDescriptor {
//    WGPUChainedStruct const * nextInChain;
//    WGPU_NULLABLE char const * label;
//    WGPU_NULLABLE WGPUPipelineLayout layout;
//    WGPUVertexState vertex;
//    WGPUPrimitiveState primitive;
//    WGPU_NULLABLE WGPUDepthStencilState const * depthStencil;
//    WGPUMultisampleState multisample;
//    WGPU_NULLABLE WGPUFragmentState const * fragment;
//} WGPURenderPipelineDescriptor WGPU_STRUCTURE_ATTRIBUTE;

public class WGPURenderPipelineDescriptor extends WgpuJavaStruct {
    public final Pointer nextInChain = new Pointer();
    public final Pointer label = new Pointer();
    public final Pointer layout = new Pointer();
    public final WGPUVertexState vertex = inner(new WGPUVertexState());
    public final WGPUPrimitiveState primitive = inner(new WGPUPrimitiveState());
    public final Pointer depthStencil = new Pointer();
    public final WGPUMultisampleState multisample = inner(new WGPUMultisampleState());
    public final Pointer fragment = new Pointer();


    public java.lang.String getLabel(){
        return CString.fromPointer(label.get());
    }
    public void setLabel(java.lang.String x){
        this.label.set(CString.toPointer(x));
    }

}
