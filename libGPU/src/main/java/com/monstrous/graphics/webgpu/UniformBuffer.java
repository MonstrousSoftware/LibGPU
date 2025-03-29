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

package com.monstrous.graphics.webgpu;

import com.monstrous.LibGPU;
import com.monstrous.graphics.Color;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;
import com.monstrous.utils.Disposable;
import com.monstrous.utils.JavaWebGPU;
import jnr.ffi.Pointer;

// todo auto padding between elements

public class UniformBuffer extends Buffer {

    private final int contentSize;
    private final int uniformStride;
    private final int maxSlices;
    private final Pointer floatData;
    private int offset;


    public UniformBuffer(int contentSize, long usage){
        this(contentSize, usage, 1);
    }

    /** Construct a Uniform Buffer. To use dynamic offsets, set maxSlices to the number of segments needed. */
    public UniformBuffer(int contentSize, long usage, int maxSlices){
        super("uniform buffer", usage, calculateBufferSize(contentSize, maxSlices));
        this.contentSize = contentSize;
        this.maxSlices = maxSlices;

        this.uniformStride = calculateStride(contentSize, maxSlices);

        // working buffer in native memory to use as input to WriteBuffer
        floatData = JavaWebGPU.createDirectPointer(contentSize);       // native memory buffer for one instance to aid write buffer
    }



    private static long calculateBufferSize(int contentSize, int maxSlices){
        // round up buffer size to 16 byte alignment
        long bufferSize = ceilToNextMultiple(contentSize, 16);

        // if we use dynamics offsets, there is a minimum stride to apply between "slices"
        if(maxSlices > 1) { // do we use dynamic offsets?
            int uniformStride = calculateStride(contentSize, maxSlices);
            bufferSize += uniformStride * (maxSlices - 1);
        }
        return bufferSize;
    }

    private static int calculateStride(int contentSize, int maxSlices){
        int stride = 0;
        if(maxSlices > 1) { // do we use dynamic offsets?
            int uniformAlignment = (int) LibGPU.supportedLimits.getLimits().getMinUniformBufferOffsetAlignment();
            stride = ceilToNextMultiple(contentSize, uniformAlignment);
        }
        return stride;
    }

    private static int ceilToNextMultiple(int value, int step){
        int d = value / step + (value % step == 0 ? 0 : 1);
        return step * d;
    }

    /** When using dynamic offsets, they need to be a multiple of this value. */
    public int getUniformStride(){
        return uniformStride;
    }

    public void beginFill(){
        offset = 0;
    }

    public void pad(int bytes){
        offset += bytes;
    }

    public int getOffset(){
        return offset;
    }

    public void setOffset(int offset){
        this.offset = offset;
    }

    public void append( int value ){
        floatData.putInt(offset, value);
        offset += Integer.BYTES;
    }

    public void append( float f ){
        floatData.putFloat(offset, f);
        offset += Float.BYTES;
        //offset += 4*Float.BYTES;           // with padding!
    }

    public void append( Matrix4 mat ){
        floatData.put(offset, mat.val, 0, 16);
        offset += 16*Float.BYTES;
    }

    public void append( Vector3 vec ){
        floatData.putFloat(offset+0*Float.BYTES, vec.x);
        floatData.putFloat(offset+1*Float.BYTES, vec.y);
        floatData.putFloat(offset+2*Float.BYTES, vec.z);
        offset += 4*Float.BYTES;           // with padding!
    }

    public void append( Color color ){
        floatData.putFloat(offset+0*Float.BYTES, color.r);
        floatData.putFloat(offset+1*Float.BYTES, color.g);
        floatData.putFloat(offset+2*Float.BYTES, color.b);
        floatData.putFloat(offset+3*Float.BYTES, color.a);
        offset += 4*Float.BYTES;
    }

    public void append( float r, float g, float b, float a ){
        floatData.putFloat(offset+0*Float.BYTES, r);
        floatData.putFloat(offset+1*Float.BYTES, g);
        floatData.putFloat(offset+2*Float.BYTES, b);
        floatData.putFloat(offset+3*Float.BYTES, a);
        offset += 4*Float.BYTES;
    }

    /** Write buffer data to the GPU */
    public void endFill(){
        endFill(0);
    }

    /** Fill the given slice of the uniform buffer. Writes data to the GPU. destOffset should be a multiple of uniformStride. */
    public void endFill(int destOffset){
        int dataSize = offset;
        if(dataSize > contentSize) throw new RuntimeException("Overflow in UniformBuffer: content ("+dataSize+") > size ("+contentSize+").");
        if(destOffset > getSize()-dataSize) throw new IllegalArgumentException("UniformBuffer: offset too large.");
        LibGPU.webGPU.wgpuQueueWriteBuffer(LibGPU.queue, getHandle(), destOffset, floatData, dataSize);
    }

}
