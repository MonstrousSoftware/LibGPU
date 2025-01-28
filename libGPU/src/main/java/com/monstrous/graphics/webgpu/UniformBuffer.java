package com.monstrous.graphics.webgpu;

import com.monstrous.wgpu.WGPUBufferUsage;

public class UniformBuffer {


    private int size;
    private WGPUBufferUsage usage;
    private boolean dynamicOffset;


    public UniformBuffer(int size, WGPUBufferUsage usage, boolean dynamicOffset) {
        this.size = size;
        this.usage = usage;
        this.dynamicOffset = dynamicOffset;
    }

    public void beginFill(){

    }

    public void append( float f ){

    }

    public void endFill(){

    }
}
