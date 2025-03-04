package com.monstrous.graphics.webgpu;

import com.monstrous.LibGPU;
import com.monstrous.utils.Disposable;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.webgpu.WGPUPipelineLayoutDescriptor;
import jnr.ffi.Pointer;

public class PipelineLayout implements Disposable {
    private final Pointer handle;

    public PipelineLayout( String label, BindGroupLayout... bindGroupLayouts ) {
        int count = bindGroupLayouts.length;
        long[] layouts = new long[count];
        for(int i = 0; i < count; i++)
            layouts[i] = bindGroupLayouts[i].getHandle().address();
        Pointer layoutPtr = JavaWebGPU.createLongArrayPointer(layouts);

        WGPUPipelineLayoutDescriptor pipelineLayoutDesc = WGPUPipelineLayoutDescriptor.createDirect();
        pipelineLayoutDesc.setNextInChain();
        pipelineLayoutDesc.setLabel(label);
        pipelineLayoutDesc.setBindGroupLayoutCount(count);
        pipelineLayoutDesc.setBindGroupLayouts(layoutPtr);  // expects an array of layouts in native memory
        handle = LibGPU.webGPU.wgpuDeviceCreatePipelineLayout(LibGPU.device, pipelineLayoutDesc);
    }

    public Pointer getHandle() {
        return handle;
    }

    @Override
    public void dispose() {
        LibGPU.webGPU.wgpuPipelineLayoutRelease(handle);
    }
}
