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
// todo test dynamic offsets

public class UniformBuffer implements Disposable {

    private int contentSize;
    private Pointer floatData;
    private int offset;
    private final int dynamicOffset;
    private Buffer buffer;

    public UniformBuffer(int contentSize, long usage){
        this(contentSize, usage, 1);
    }

    public UniformBuffer(int contentSize, long usage, int maxSlices){
        this.contentSize = contentSize;
        dynamicOffset = 0;

        // round up buffer size to 16 byte alignment
        long bufferSize = (long)ceilToNextMultiple(contentSize, 16);

        // if we use dynamics offsets, there is a minimum stride to apply between "slices"
        if(maxSlices > 1) { // do we use dynamic offsets?
            int uniformAlignment = (int) LibGPU.supportedLimits.getLimits().getMinUniformBufferOffsetAlignment();
            long uniformStride = ceilToNextMultiple(contentSize, uniformAlignment);
            bufferSize += uniformStride * (maxSlices - 1);
        }

        buffer = new Buffer("uniform buffer", usage, bufferSize);

//        // Create uniform buffer
//        WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
//        bufferDesc.setLabel("Uniform object buffer");
//        bufferDesc.setUsage( usage ); //WGPUBufferUsage.CopyDst | WGPUBufferUsage.Uniform );
//        bufferDesc.setSize( bufferSize );
//        bufferDesc.setMappedAtCreation(0L);
//        this.handle = LibGPU.webGPU.wgpuDeviceCreateBuffer(LibGPU.device, bufferDesc);

        // working buffer in native memory to use as input to WriteBuffer
        float[] floats = new float[contentSize/Float.BYTES];
        floatData = JavaWebGPU.createFloatArrayPointer(floats);       // native memory buffer for one instance to aid write buffer
    }

    private int ceilToNextMultiple(int value, int step){
        int d = value / step + (value % step == 0 ? 0 : 1);
        return step * d;
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


    public void endFill(){
        endFill(0);
    }


    public void endFill(int writeOffset){
        if(offset > contentSize)
            throw new RuntimeException("Overflow in UniformBuffer: offset ("+offset+") > size ("+contentSize+").");
        LibGPU.webGPU.wgpuQueueWriteBuffer(LibGPU.queue, buffer.getHandle(), dynamicOffset+writeOffset, floatData, offset);
    }

    public Pointer getHandle(){
        return buffer.getHandle();
    }

    public Buffer getBuffer(){
        return buffer;
    }

    @Override
    public void dispose() {
        LibGPU.webGPU.wgpuBufferRelease(buffer.getHandle());
        buffer = null;
    }
}
