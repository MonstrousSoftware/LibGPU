package com.monstrous.wgpu;

import com.monstrous.utils.WgpuJavaStruct;
import jnr.ffi.Struct;

//WGPUBlendOperation operation;
//WGPUBlendFactor srcFactor;
//WGPUBlendFactor dstFactor;

public class WGPUBlendComponent extends WgpuJavaStruct {
    public final Struct.Enum32<WGPUBlendOperation> operation = new Enum32<>(WGPUBlendOperation.class);
    public final Struct.Enum32<WGPUBlendFactor> srcFactor = new Enum32<>(WGPUBlendFactor.class);
    public final Struct.Enum32<WGPUBlendFactor> dstFactor = new Enum32<>(WGPUBlendFactor.class);
}
