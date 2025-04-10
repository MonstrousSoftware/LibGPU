package com.monstrous.graphics.webgpu;

import com.monstrous.LibGPU;
import com.monstrous.utils.Disposable;
import com.monstrous.webgpu.WGPUCommandBufferDescriptor;
import jnr.ffi.Pointer;

public class CommandBuffer implements Disposable {
    private Pointer commandBuffer;

    public CommandBuffer(CommandEncoder encoder ) {
        // finish the encoder to give use command buffer
        WGPUCommandBufferDescriptor bufferDescriptor = WGPUCommandBufferDescriptor.createDirect();
        bufferDescriptor.setNextInChain();
        commandBuffer = LibGPU.webGPU.wgpuCommandEncoderFinish(encoder.getHandle(), bufferDescriptor);
    }

    public Pointer getHandle(){
        return commandBuffer;
    }

    @Override
    public void dispose() {
        LibGPU.webGPU.wgpuCommandBufferRelease(commandBuffer);
    }
}
