package com.monstrous.graphics;

import com.monstrous.LibGPU;
import com.monstrous.graphics.loaders.MeshData;
import com.monstrous.wgpu.WGPUBufferDescriptor;
import com.monstrous.wgpu.WGPUBufferUsage;
import com.monstrous.wgpuUtils.WgpuJava;
import jnr.ffi.Pointer;

public class Mesh {

    private Pointer vertexBuffer;
    private Pointer indexBuffer;
    private int vertexCount;
    private int indexCount;     // can be zero if the vertices are not indexed
    public VertexAttributes vertexAttributes;


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

    public Mesh(MeshData data){

        vertexAttributes = data.vertexAttributes;
        // todo could calculate vertSize from attributes

        vertexCount = data.vertFloats.size()/data.vertSize;
        float[] vertexData = new float[ data.vertFloats.size() ];
        for(int i = 0; i < data.vertFloats.size(); i++){
            vertexData[i] = data.vertFloats.get(i);
        }

        indexCount = data.indexValues.size();
        int [] indexData = new int[ indexCount ];
        for(int i = 0; i < indexCount; i++){
            indexData[i] = data.indexValues.get(i);
        }

        // Create vertex buffer
        WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
        bufferDesc.setLabel("Vertex buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.Vertex );
        bufferDesc.setSize((long) vertexData.length *Float.BYTES);
        bufferDesc.setMappedAtCreation(0L);
        vertexBuffer = LibGPU.wgpu.DeviceCreateBuffer(LibGPU.device, bufferDesc);

        Pointer dataBuf = WgpuJava.createFloatArrayPointer(vertexData);

        // Upload geometry data to the buffer
        LibGPU.wgpu.QueueWriteBuffer(LibGPU.queue, vertexBuffer, 0, dataBuf, (int)bufferDesc.getSize());

        // Create index buffer
        bufferDesc.setLabel("Index buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.Index );
        int indexBufferSize = indexData.length*Integer.BYTES;
        indexBufferSize = (indexBufferSize + 3) & ~3; // round up to the next multiple of 4
        bufferDesc.setSize(indexBufferSize);
        // in case we use a sort index:

        bufferDesc.setMappedAtCreation(0L);
        indexBuffer = LibGPU.wgpu.DeviceCreateBuffer(LibGPU.device, bufferDesc);

        Pointer idata = WgpuJava.createIntegerArrayPointer(indexData);
        // Upload data to the buffer
        LibGPU.wgpu.QueueWriteBuffer(LibGPU.queue, indexBuffer, 0, idata, (int)bufferDesc.getSize());
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
