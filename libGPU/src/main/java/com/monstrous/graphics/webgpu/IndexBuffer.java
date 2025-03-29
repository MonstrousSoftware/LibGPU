package com.monstrous.graphics.webgpu;

import com.monstrous.LibGPU;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.webgpu.WGPUBufferUsage;
import com.monstrous.webgpu.WGPUIndexFormat;
import jnr.ffi.Pointer;

import java.util.ArrayList;

public class IndexBuffer extends Buffer {

    private int indexSizeInBytes;   // 2 or 4
    private int indexCount;

    public IndexBuffer(long usage, int bufferSize, int indexSizeInBytes) {
        super("index buffer", usage, align(bufferSize));
        this.indexSizeInBytes = indexSizeInBytes;
    }

    public IndexBuffer(ArrayList<Integer> indexValues, int indexSizeInBytes) {
        this(WGPUBufferUsage.CopyDst | WGPUBufferUsage.Index, align(indexValues.size()*indexSizeInBytes),indexSizeInBytes);
        setIndices(indexValues);
    }

    public IndexBuffer(short[] indexValues, int indexCount) {
        this(WGPUBufferUsage.CopyDst | WGPUBufferUsage.Index, align(indexCount*2), 2);
        setIndices(indexValues, indexCount);
    }

    private static int align(int indexBufferSize ){
        return (indexBufferSize + 3) & ~3; // round up to the next multiple of 4
    }

    public int getIndexCount(){
        return indexCount;
    }

    public WGPUIndexFormat getFormat(){
        return determineFormat(indexSizeInBytes);
    }

    public static WGPUIndexFormat determineFormat(int indexSizeInBytes ){
        if(indexSizeInBytes == 2)
            return WGPUIndexFormat.Uint16;
        else if(indexSizeInBytes == 4)
            return WGPUIndexFormat.Uint32;
        else
            throw new RuntimeException("setIndices: support only 16 bit or 32 bit indices.");
    }

    public void setIndices(short[] indices, int indexCount){
        this.indexSizeInBytes = 2;
        this.indexCount = indexCount;
        int indexBufferSize = align(indexCount * indexSizeInBytes);

        Pointer idata = JavaWebGPU.createDirectPointer(indexBufferSize);
        idata.put(0, indices, 0, indexCount);
        setIndices(idata, indexBufferSize);
    }

    public void setIndices(int[] indices, int indexCount){
        this.indexSizeInBytes = 4;
        this.indexCount = indexCount;
        int indexBufferSize = align(indexCount * indexSizeInBytes);

        Pointer idata = JavaWebGPU.createDirectPointer(indexBufferSize);
        idata.put(0, indices, 0, indexCount);
        setIndices(idata, indexBufferSize);
    }

    public void setIndices(ArrayList<Integer> indexValues) {
        if(indexValues == null) {
            indexCount = 0;
            return;
        }
        indexCount = indexValues.size();
        int indexBufferSize = align(indexCount * indexSizeInBytes);

        Pointer idata = JavaWebGPU.createDirectPointer(indexBufferSize);
        if (indexSizeInBytes == 2) {
            for (int i = 0; i < indexCount; i++) {
                idata.putShort((long) i * indexSizeInBytes, (short) (int) indexValues.get(i));
            }
        } else if (indexSizeInBytes == 4) {
            for (int i = 0; i < indexCount; i++) {
                idata.putInt((long) i * indexSizeInBytes, indexValues.get(i));
            }
        }
        setIndices(idata, indexBufferSize);
    }

    /** fill index buffer with raw data. */
    private void setIndices(Pointer idata, int indexBufferSize) {
        if(indexBufferSize > getSize()) throw new IllegalArgumentException("IndexBuffer.setIndices: data too large.");

        // Upload data to the buffer
        LibGPU.webGPU.wgpuQueueWriteBuffer(LibGPU.queue, getHandle(), 0, idata, indexBufferSize);
    }
}
