package com.monstrous.graphics.webgpu;

import com.monstrous.LibGPU;
import com.monstrous.utils.Disposable;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.webgpu.WGPUPipelineLayoutDescriptor;
import jnr.ffi.Pointer;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;

public class PipelineLayout implements Disposable {
    private final Pointer handle;

    public PipelineLayout( String label, BindGroupLayout... bindGroupLayouts ) {
        int count = bindGroupLayouts.length;
        try (MemoryStack stack = stackPush()) {
            ByteBuffer pLayouts = stack.malloc(count * Long.BYTES);
            for (int i = 0; i < count; i++)
                pLayouts.putLong(i*Long.BYTES, bindGroupLayouts[i].getHandle().address());

            WGPUPipelineLayoutDescriptor pipelineLayoutDesc = WGPUPipelineLayoutDescriptor.createDirect();
            pipelineLayoutDesc.setNextInChain();
            pipelineLayoutDesc.setLabel(label);
            pipelineLayoutDesc.setBindGroupLayoutCount(count);
            pipelineLayoutDesc.setBindGroupLayouts(JavaWebGPU.createByteBufferPointer(pLayouts));  // expects an array of layouts in native memory
            handle = LibGPU.webGPU.wgpuDeviceCreatePipelineLayout(LibGPU.device.getHandle(), pipelineLayoutDesc);
        } // free malloced memory
    }

    public Pointer getHandle() {
        return handle;
    }

    @Override
    public void dispose() {
        LibGPU.webGPU.wgpuPipelineLayoutRelease(handle);
    }
}
