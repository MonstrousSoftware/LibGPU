package com.monstrous.graphics.webgpu;

import com.monstrous.LibGPU;
import com.monstrous.utils.Disposable;
import com.monstrous.webgpu.WGPUCommandEncoderDescriptor;
import com.monstrous.webgpu.WGPUComputePassDescriptor;
import jnr.ffi.Pointer;

public class CommandEncoder implements Disposable {

    private final Pointer encoder;

    public CommandEncoder(Pointer device) {
        // create a command encoder
        WGPUCommandEncoderDescriptor encoderDesc = WGPUCommandEncoderDescriptor.createDirect();
        encoderDesc.setNextInChain();
        encoder = LibGPU.webGPU.wgpuDeviceCreateCommandEncoder(LibGPU.device, encoderDesc);
    }

    public Pointer getHandle(){
        return encoder;
    }

    public ComputePass beginComputePass(){
        return new ComputePass(this);
    }

    public CommandBuffer finish(){
        return new CommandBuffer(this);
    }

    public void copyBufferToBuffer(Buffer buffer1, int offset1, Buffer buffer2, int offset2, int byteCount){
        LibGPU.webGPU.wgpuCommandEncoderCopyBufferToBuffer(encoder, buffer1.getHandle(), offset1, buffer2.getHandle(), offset2, byteCount);
    }

    @Override
    public void dispose() {
           LibGPU.webGPU.wgpuCommandEncoderRelease(encoder);
    }
}
