package com.monstrous.graphics.webgpu;

import com.monstrous.LibGPU;
import com.monstrous.utils.Disposable;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.webgpu.WGPUComputePassDescriptor;
import jnr.ffi.Pointer;

public class ComputePass implements Disposable {
    private final Pointer computePass;

    public ComputePass(CommandEncoder commandEncoder) {
        // Create a compute pass
        WGPUComputePassDescriptor passDesc = WGPUComputePassDescriptor.createDirect();
        passDesc.setNextInChain();
        passDesc.setTimestampWrites();
        computePass = LibGPU.webGPU.wgpuCommandEncoderBeginComputePass(commandEncoder.getHandle(), passDesc);
    }

    public void setBindGroup(int groupId, BindGroup bindGroup) {
        LibGPU.webGPU.wgpuComputePassEncoderSetBindGroup(computePass, groupId, bindGroup.getHandle(), 0, JavaWebGPU.createNullPointer());
    }

    public void setPipeline(Pointer pipeline) {
        LibGPU.webGPU.wgpuComputePassEncoderSetPipeline(computePass, pipeline);
    }

    public void dispatchWorkGroups(int workgroupCountX, int workgroupCountY, int workgroupCountZ) {
        LibGPU.webGPU.wgpuComputePassEncoderDispatchWorkgroups(computePass,workgroupCountX,workgroupCountY,workgroupCountZ);
    }

    public void end(){
        LibGPU.webGPU.wgpuComputePassEncoderEnd(computePass);
    }


    public Pointer getHandle(){
        return computePass;
    }

    @Override
    public void dispose() {
        //webGPU.wgpuComputePassEncoderRelease(computePass);
    }
}
