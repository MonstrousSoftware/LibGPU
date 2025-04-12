package com.monstrous.graphics.webgpu;

import com.monstrous.LibGPU;
import com.monstrous.utils.Disposable;
import com.monstrous.utils.JavaWebGPU;
import jnr.ffi.Pointer;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;

public class Queue implements Disposable {
    private final Pointer queue;

    public Queue(Device device) {
        queue = LibGPU.webGPU.wgpuDeviceGetQueue(device.getHandle());
    }

    public Pointer getHandle(){
        return queue;
    }

    public void submit(CommandBuffer commandBuffer) {
        try (MemoryStack stack = stackPush()) {
            // create native array of command buffer pointers
            ByteBuffer pBuffers = stack.malloc(Long.BYTES);
            pBuffers.putLong(0, commandBuffer.getHandle().address());

            LibGPU.webGPU.wgpuQueueSubmit(queue, 1, JavaWebGPU.createByteBufferPointer(pBuffers));
        }
    }

    public void writeBuffer(Buffer buffer, int bufferOffset, Pointer data, int dataSize) {
        LibGPU.webGPU.wgpuQueueWriteBuffer(queue, buffer.getHandle(),bufferOffset, data, dataSize);
    }




    @Override
    public void dispose() {
        LibGPU.webGPU.wgpuQueueRelease(queue);
    }
}
