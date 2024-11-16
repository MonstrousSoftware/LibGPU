package com.monstrous.graphics;

import com.monstrous.FileInput;
import com.monstrous.LibGPU;
import com.monstrous.wgpu.WGPUVertexBufferLayout;
import com.monstrous.wgpu.WGPUVertexFormat;
import com.monstrous.wgpuUtils.WgpuJava;
import com.monstrous.wgpu.WGPUBufferDescriptor;
import com.monstrous.wgpu.WGPUBufferUsage;
import jnr.ffi.Pointer;

import java.util.ArrayList;

public class Mesh {

    private Pointer vertexBuffer;
    private Pointer indexBuffer;
    private int vertexCount;
    private int indexCount;


    public Mesh(String name) {
        load(name);
    }

    private void load(String fileName){
        int dimensions = 3;
        FileInput input = new FileInput(fileName);
        int vertSize = 8+dimensions; // in floats
        ArrayList<Integer> indexValues = new ArrayList<>();
        ArrayList<Float> vertFloats = new ArrayList<>();
        int mode = 0;
        for(int lineNr = 0; lineNr < input.size(); lineNr++) {
            String line = input.get(lineNr).strip();
            if (line.contentEquals("[points]")) {
                mode = 1;
                continue;
            }
            if (line.contentEquals("[indices]")) {
                mode = 2;
                continue;
            }
            if(line.startsWith("#"))
                continue;
            if(line.isEmpty())
                continue;
            if(mode == 1){
                String [] words = line.split("[ \t]+");
                if(words.length != vertSize)
                    System.out.println("Expected "+vertSize+" floats per vertex : "+line);
                for(int i = 0; i < vertSize; i++)
                    vertFloats.add(Float.parseFloat(words[i]));
            } else if (mode == 2){
                String [] words = line.split("[ \t]+");
                if(words.length != 3)
                    System.out.println("Expected 3 indices per line: "+line);
                for(int i = 0; i < 3; i++)
                    indexValues.add(Integer.parseInt(words[i]));
            } else {
                System.out.println("Unexpected input: "+line);
            }
        }

        vertexCount = vertFloats.size()/vertSize;
        float[] vertexData = new float[ vertFloats.size() ];
        for(int i = 0; i < vertFloats.size(); i++){
            vertexData[i] = vertFloats.get(i);
        }

        indexCount = indexValues.size();
        int [] indexData = new int[ indexCount ];
        for(int i = 0; i < indexCount; i++){
            indexData[i] = indexValues.get(i);
        }

        // Create vertex buffer
        WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
        bufferDesc.setLabel("Vertex buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.Vertex );
        bufferDesc.setSize((long) vertexData.length *Float.BYTES);
        bufferDesc.setMappedAtCreation(0L);
        vertexBuffer = LibGPU.wgpu.DeviceCreateBuffer(LibGPU.device, bufferDesc);

        Pointer data = WgpuJava.createFloatArrayPointer(vertexData);

        // Upload geometry data to the buffer
        LibGPU.wgpu.QueueWriteBuffer(LibGPU.queue, vertexBuffer, 0, data, (int)bufferDesc.getSize());

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
