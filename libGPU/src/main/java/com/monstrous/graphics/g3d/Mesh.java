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
import com.monstrous.graphics.webgpu.IndexBuffer;
import com.monstrous.graphics.webgpu.VertexBuffer;
import com.monstrous.math.Vector3;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.webgpu.WGPUBufferUsage;
import com.monstrous.webgpu.WGPUIndexFormat;
import jnr.ffi.Pointer;

import java.util.ArrayList;

public class Mesh {

    private VertexBuffer vertexBuffer;
    private IndexBuffer indexBuffer;
    private int vertexCount;
    public VertexAttributes vertexAttributes;
    public BoundingBox boundingBox;

    public Mesh(){
        boundingBox = new BoundingBox();
    }

    public Mesh(MeshData data) {
        this();
        setVertexAttributes(data.vertexAttributes);

        vertexCount = data.vertFloats.size() * Float.BYTES / data.vertexAttributes.getVertexSizeInBytes();

        // convert ArrayList<Float> to float[]
        // todo use FloatBuffer in MeshData?
        float[] vertexData = new float[data.vertFloats.size()];
        for (int i = 0; i < data.vertFloats.size(); i++) {
            vertexData[i] = data.vertFloats.get(i);
        }
        vertexBuffer = new VertexBuffer(vertexData.length *Float.BYTES);
        vertexBuffer.setVertices(vertexData);
        calculateBoundingBox(vertexData);

        if(data.indexValues.size() > 0)
            indexBuffer = new IndexBuffer(data.indexValues, data.indexSizeInBytes);
    }



    public void setVertexAttributes(VertexAttributes vertexAttributes){
        this.vertexAttributes = vertexAttributes;
    }

    public void setVertices(float[] vertexData) {
        if(vertexBuffer == null)
            vertexBuffer = new VertexBuffer(vertexData.length *Float.BYTES);
        vertexBuffer.setVertices(vertexData);
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


    public void setIndices(short[] indices, int indexCount){
        if(indexBuffer == null)
            indexBuffer = new IndexBuffer(indices, indexCount);
        else
            indexBuffer.setIndices(indices, indexCount);
    }

    public void dispose(){
        if(indexBuffer != null)
            indexBuffer.dispose();
        vertexBuffer.dispose();
    }

    public VertexBuffer getVertexBuffer(){
        return vertexBuffer;
    }

    public IndexBuffer getIndexBuffer(){
        return indexBuffer;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public int getIndexCount() {
        return indexBuffer == null ? 0 : indexBuffer.getIndexCount();
    }
}
