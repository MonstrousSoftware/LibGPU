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
import com.monstrous.utils.Disposable;
import com.monstrous.webgpu.WGPUBufferDescriptor;
import jnr.ffi.Pointer;


/**
 * Encapsulation of WebGPU Buffer
 *
 * label: for debug/error messages, no functional value
 * bufferSize: in bytes, to be aligned if necessary
 * usage: one or more flags in combination, e.g. WGPUBufferUsage.CopyDst | WGPUBufferUsage.Uniform
 */
public class Buffer implements Disposable {

    private Pointer handle;
    private long bufferSize;

    public Buffer(String label, long usage, long bufferSize){
        this.bufferSize = bufferSize;

        // Create uniform buffer
        WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
        bufferDesc.setLabel( label );
        bufferDesc.setUsage( usage );
        bufferDesc.setSize( bufferSize );
        bufferDesc.setMappedAtCreation(0L);
        this.handle = LibGPU.webGPU.wgpuDeviceCreateBuffer(LibGPU.device.getHandle(), bufferDesc);
    }

    public Pointer getHandle(){
        return handle;
    }

    public long getSize(){
        return bufferSize;
    }

    @Override
    public void dispose() {
        LibGPU.webGPU.wgpuBufferDestroy(handle);
        LibGPU.webGPU.wgpuBufferRelease(handle);
        handle = null;
    }
}
