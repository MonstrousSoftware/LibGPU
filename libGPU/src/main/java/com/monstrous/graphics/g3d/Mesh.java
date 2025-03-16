/*******************************************************************************
 * Copyright 2025 Monstrous Software.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.monstrous.graphics.g3d;

import com.monstrous.LibGPU;
import com.monstrous.graphics.VertexAttribute;
import com.monstrous.graphics.VertexAttributes;
import com.monstrous.graphics.loaders.MeshData;
import com.monstrous.graphics.webgpu.Buffer;
import com.monstrous.math.Vector3;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.webgpu.WGPUBufferUsage;
import com.monstrous.webgpu.WGPUIndexFormat;
import jnr.ffi.Pointer;

import java.util.ArrayList;

public class Mesh {

    private Buffer vertexBuffer;
    private Buffer indexBuffer;
    private int vertexCount;
    private int indexCount;     // can be zero if the vertices are not indexed
    public VertexAttributes vertexAttributes;
    public WGPUIndexFormat indexFormat = WGPUIndexFormat.Uint16;
    public BoundingBox boundingBox;

    public Mesh(){
        boundingBox = new BoundingBox();
        indexCount = 0;
    }

    public Mesh(MeshData data) {
        this();
        setVertexAttributes(data.vertexAttributes);

        //vertexCount = data.vertFloats.size() * Float.BYTES / data.vertexAttributes.getVertexSizeInBytes();
        float[] vertexData = new float[data.vertFloats.size()];
        for (int i = 0; i < data.vertFloats.size(); i++) {
            vertexData[i] = data.vertFloats.get(i);
        }
        setVertices(vertexData);

        setIndices(data.indexValues, data.indexSizeInBytes);
    }

    private void calculateBoundingBox(float[] vertexData){
        int stride = vertexAttributes.getVertexSizeInBytes()/Float.BYTES;   // stride in floats
        int positionOffset = vertexAttributes.getOffset(VertexAttribute.Usage.POSITION);
        if(positionOffset < 0)
            throw new RuntimeException("Mesh has no POSITION information.");
        boundingBox.clear();
        Vector3 vertex = new Vector3();
        for(int i = 0; i < vertexCount; i++){
            int index = i*stride + positionOffset;
            vertex.x = vertexData[index];
            vertex.y = vertexData[index+1];
            vertex.z = vertexData[index+2];
            boundingBox.ext(vertex);
        }
    }

    public void setVertexAttributes(VertexAttributes vertexAttributes){
        this.vertexAttributes = vertexAttributes;
    }

    public void setVertices(float[] vertexData) {
        // Create vertex buffer
        int size = vertexData.length *Float.BYTES;
        vertexBuffer = new Buffer("Vertex buffer", WGPUBufferUsage.CopyDst | WGPUBufferUsage.Vertex, size);
        vertexCount = size / vertexAttributes.getVertexSizeInBytes();

        Pointer dataBuf = JavaWebGPU.createFloatArrayPointer(vertexData);
        // Upload geometry data to the buffer
        LibGPU.webGPU.wgpuQueueWriteBuffer(LibGPU.queue,vertexBuffer.getHandle(),0,dataBuf, size);

        calculateBoundingBox(vertexData);
    }


    public void setIndices(short[] indices){
        int indexSizeInBytes = 2;
        indexCount = indices.length;
        int indexBufferSize = indexCount * indexSizeInBytes;
        indexBufferSize = (indexBufferSize + 3) & ~3; // round up to the next multiple of 4

        Pointer idata = JavaWebGPU.createDirectPointer(indexBufferSize);
        idata.put(0, indices, 0, indexCount);
        setIndices(idata, indexBufferSize);
    }

    public void setIndices(ArrayList<Integer> indexValues){
        if(indexValues == null)
            indexCount = 0;
        else {
            int indexWidth = 2;
            if (indexValues.size() > Short.MAX_VALUE)
                indexWidth = 4;
            setIndices(indexValues, indexWidth);
        }
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

    public void setIndices(Pointer idata, int indexBufferSize) {
        indexBuffer = new Buffer("Index buffer", WGPUBufferUsage.CopyDst | WGPUBufferUsage.Index, indexBufferSize);

        // Upload data to the buffer
        LibGPU.webGPU.wgpuQueueWriteBuffer(LibGPU.queue, indexBuffer.getHandle(), 0, idata, indexBufferSize);
    }

    public void dispose(){
        if(indexBuffer != null)
            indexBuffer.dispose();
        vertexBuffer.dispose();

    }

    public Buffer getVertexBuffer(){
        return vertexBuffer;
    }

    public Buffer getIndexBuffer(){
        return indexBuffer;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public int getIndexCount() {
        return indexCount;
    }
}
