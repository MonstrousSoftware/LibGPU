package com.monstrous.graphics.webgpu;

import com.monstrous.LibGPU;
import com.monstrous.utils.Disposable;
import com.monstrous.utils.JavaWebGPU;
import jnr.ffi.Pointer;

public class Queue implements Disposable {
    private final Pointer queue;

    public Queue(Pointer device) {
        queue = LibGPU.webGPU.wgpuDeviceGetQueue(device);
    }

    public void submit(CommandBuffer commandBuffer) {
        // feed the command buffer to the queue
        long[] buffers = new long[1];
        buffers[0] = commandBuffer.getHandle().address();
        Pointer bufferPtr = JavaWebGPU.createLongArrayPointer(buffers);
        LibGPU.webGPU.wgpuQueueSubmit(LibGPU.queue, 1, bufferPtr);
    }

    public void writeBuffer(Buffer buffer, int bufferOffset, Pointer data, int dataSize) {
        LibGPU.webGPU.wgpuQueueWriteBuffer(queue, buffer.getHandle(),bufferOffset, data, dataSize);
    }

    @Override
    public void dispose() {
        LibGPU.webGPU.wgpuQueueRelease(queue);
    }
}
