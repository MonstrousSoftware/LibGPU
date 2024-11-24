package com.monstrous.graphics;

import com.monstrous.LibGPU;
import com.monstrous.graphics.loaders.MeshData;
import com.monstrous.wgpu.WGPUBufferDescriptor;
import com.monstrous.wgpu.WGPUBufferUsage;
import com.monstrous.wgpu.WGPUIndexFormat;
import com.monstrous.wgpuUtils.WgpuJava;
import jnr.ffi.Pointer;

import java.util.ArrayList;

public class Mesh {

    private Pointer vertexBuffer;
    private Pointer indexBuffer;
    private int vertexCount;
    private int indexCount;     // can be zero if the vertices are not indexed
    public VertexAttributes vertexAttributes;
    public WGPUIndexFormat indexFormat = WGPUIndexFormat.Uint16;


//    public Mesh(String name) {
//        load(name);
//    }


//    private void loadTxt(String fileName) {
//        MeshData data = TxtLoader.load(fileName);
//        storeMesh(data);
//    }
//    private void load(String fileName) {
//        this(ObjLoader.load(fileName));
//
//        System.out.println("Loaded "+data.objectName);
//    }

    public Mesh(MeshData data) {
        vertexAttributes = data.vertexAttributes;

        vertexCount = data.vertFloats.size() * Float.BYTES / data.vertexAttributes.getVertexSizeInBytes();
        float[] vertexData = new float[data.vertFloats.size()];
        for (int i = 0; i < data.vertFloats.size(); i++) {
            vertexData[i] = data.vertFloats.get(i);
        }
        setVertices(vertexData);
        setIndices(data.indexValues, data.indexSizeInBytes);
    }

    public void setVertices(float[] vertexData) {
        // Create vertex buffer
        WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
            bufferDesc.setLabel("Vertex buffer");
            bufferDesc.setUsage(WGPUBufferUsage.CopyDst |WGPUBufferUsage.Vertex );
            bufferDesc.setSize((long)vertexData.length *Float.BYTES);
            System.out.println("VB "+(long)vertexData.length *Float.BYTES);
            bufferDesc.setMappedAtCreation(0L);
        vertexBuffer =LibGPU.wgpu.DeviceCreateBuffer(LibGPU.device,bufferDesc);

        Pointer dataBuf = WgpuJava.createFloatArrayPointer(vertexData);
        // Upload geometry data to the buffer
        LibGPU.wgpu.QueueWriteBuffer(LibGPU.queue,vertexBuffer,0,dataBuf,(int)bufferDesc.getSize());
    }

    public void setIndices(ArrayList<Integer> indexValues, int indexSizeInBytes) {

        if(indexSizeInBytes == 2)
            indexFormat = WGPUIndexFormat.Uint16;
        else if(indexSizeInBytes == 4)
            indexFormat = WGPUIndexFormat.Uint32;
        else
            throw new RuntimeException("setIndices: support only 16 bit or 32 bit indices.");

        indexCount = indexValues.size();
        int indexBufferSize = indexCount * indexSizeInBytes;
        indexBufferSize = (indexBufferSize + 3) & ~3; // round up to the next multiple of 4

        Pointer idata = WgpuJava.createDirectPointer(indexBufferSize);
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

    public void setIndices(Pointer idata, int indexBufferSize) {
        // Create index buffer
        WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();

        // Create index buffer
        bufferDesc.setLabel("Index buffer");
        bufferDesc.setUsage(WGPUBufferUsage.CopyDst | WGPUBufferUsage.Index);
        bufferDesc.setSize(indexBufferSize);
        bufferDesc.setMappedAtCreation(0L);
        indexBuffer = LibGPU.wgpu.DeviceCreateBuffer(LibGPU.device, bufferDesc);

        // Upload data to the buffer
        LibGPU.wgpu.QueueWriteBuffer(LibGPU.queue, indexBuffer, 0, idata, indexBufferSize);
    }

    public void dispose(){
        LibGPU.wgpu.BufferRelease(indexBuffer);
        LibGPU.wgpu.BufferRelease(vertexBuffer);
    }

    public Pointer getVertexBuffer(){
        return vertexBuffer;
    }

    public Pointer getIndexBuffer(){
        return indexBuffer;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public int getIndexCount() {
        return indexCount;
    }
}
