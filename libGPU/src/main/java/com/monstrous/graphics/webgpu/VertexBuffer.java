package com.monstrous.graphics.webgpu;

import com.monstrous.LibGPU;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.webgpu.WGPUBufferUsage;
import jnr.ffi.Pointer;

public class VertexBuffer extends Buffer {

    /** size in bytes */
    public VertexBuffer(long bufferSize) {
        this(WGPUBufferUsage.CopyDst | WGPUBufferUsage.Vertex, bufferSize);
    }

    /** size in bytes */
    public VertexBuffer(long usage, long bufferSize) {
        super("vertex buffer", usage, bufferSize);
    }

    public void setVertices(float[] vertexData) {
        // Create vertex buffer
        int size = vertexData.length *Float.BYTES;
        if(size > getSize()) throw new IllegalArgumentException("VertexBuffer.setVertices: data set too large.");
        Pointer dataBuf = JavaWebGPU.createFloatArrayPointer(vertexData);
        // Upload geometry data to the buffer
        LibGPU.webGPU.wgpuQueueWriteBuffer(LibGPU.queue, getHandle(),0,dataBuf, size);
    }

}
