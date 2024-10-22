package com.monstrous.wgpu;

import com.monstrous.utils.WgpuJavaStruct;

public class WGPUBlendState extends WgpuJavaStruct {
    public final WGPUBlendComponent color = inner(new WGPUBlendComponent());
    public final WGPUBlendComponent alpha = inner(new WGPUBlendComponent());

}
