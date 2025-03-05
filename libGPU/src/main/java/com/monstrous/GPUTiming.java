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

package com.monstrous;

import com.monstrous.utils.Disposable;
import com.monstrous.webgpu.*;
import jnr.ffi.Pointer;

import static com.monstrous.LibGPU.webGPU;

public class GPUTiming implements Disposable {

    private final boolean timingEnabled;
    private Pointer timestampQuerySet;
    private Pointer timeStampResolveBuffer;
    private Pointer timeStampMapBuffer;
    private boolean timeStampMapOngoing = false;
    private WGPURenderPassTimestampWrites query = null;

    public GPUTiming(Pointer device, boolean enabled) {
        this.timingEnabled = enabled;
        if(!timingEnabled)
            return;

        // Create timestamp queries
        WGPUQuerySetDescriptor querySetDescriptor =  WGPUQuerySetDescriptor.createDirect();
        querySetDescriptor.setNextInChain();
        querySetDescriptor.setLabel("Timestamp Query Set");
        querySetDescriptor.setType(WGPUQueryType.Timestamp);
        querySetDescriptor.setCount(2); // start and end time

        timestampQuerySet = webGPU.wgpuDeviceCreateQuerySet(device, querySetDescriptor);

        // Create buffer
        WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
        bufferDesc.setLabel("timestamp resolve buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopySrc | WGPUBufferUsage.QueryResolve );
        bufferDesc.setSize(16);     // space for 2 uint64's
        bufferDesc.setMappedAtCreation(0L);
        timeStampResolveBuffer = webGPU.wgpuDeviceCreateBuffer(device, bufferDesc);

        bufferDesc.setLabel("timestamp map buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.MapRead );
        bufferDesc.setSize(16);
        timeStampMapBuffer = webGPU.wgpuDeviceCreateBuffer(device, bufferDesc);

        query = WGPURenderPassTimestampWrites.createDirect();
        query.setBeginningOfPassWriteIndex(0);
        query.setEndOfPassWriteIndex(1);
        query.setQuerySet(timestampQuerySet);
    }

    // call this before configuration of render pass
    public void configureRenderPassDescriptor(WGPURenderPassDescriptor renderPassDescriptor){
        if(timingEnabled) {
            renderPassDescriptor.setTimestampWrites(query);
        } else {
            renderPassDescriptor.setTimestampWrites();  // no timestamp writes
        }
    }

    public void resolveTimeStamps(Pointer encoder){
        if(!timingEnabled || timeStampMapOngoing)
            return;

        // Resolve the timestamp queries (write their result to the resolve buffer)
        webGPU.wgpuCommandEncoderResolveQuerySet(encoder, timestampQuerySet, 0, 2, timeStampResolveBuffer, 0);

        // Copy to the map buffer
        webGPU.wgpuCommandEncoderCopyBufferToBuffer(encoder, timeStampResolveBuffer, 0,  timeStampMapBuffer, 0,16);
    }



    WGPUBufferMapCallback onTimestampBufferMapped = (WGPUBufferMapAsyncStatus status, Pointer userData) -> {
        if(status != WGPUBufferMapAsyncStatus.Success)
            System.out.println("*** ERROR: Timestamp buffer mapped with status: " + status);
        else {
            Pointer ram =  webGPU.wgpuBufferGetConstMappedRange(timeStampMapBuffer, 0, 16);
            long start = ram.getLong(0);
            long end = ram.getLong(Long.BYTES);
            webGPU.wgpuBufferUnmap(timeStampMapBuffer);
            long ns = end - start;
            addTimeSample(ns);
        }
        timeStampMapOngoing = false;
    };

    public void fetchTimestamps(){
        if(!timingEnabled || timeStampMapOngoing)
            return;

        // use a lambda expression to define a callback function


        timeStampMapOngoing = true;
        webGPU.wgpuBufferMapAsync(timeStampMapBuffer, WGPUMapMode.Read, 0, 16, onTimestampBufferMapped, null);
    }

    @Override
    public void dispose() {
       if(!timingEnabled)
            return;

        webGPU.wgpuQuerySetRelease(timestampQuerySet);
        webGPU.wgpuQuerySetDestroy(timestampQuerySet);

        webGPU.wgpuBufferDestroy(timeStampMapBuffer);
        webGPU.wgpuBufferRelease(timeStampMapBuffer);
        webGPU.wgpuBufferDestroy(timeStampResolveBuffer);
        webGPU.wgpuBufferRelease(timeStampResolveBuffer);
    }

    private long cumulative = 0;
    private int numSamples = 0;

    private void addTimeSample(long us){
        numSamples++;
        cumulative += us;
    }

    // returns average time per frame spent by GPU (in microseconds).
    public float getAverageGPUtime(){
//        if(!timingEnabled)
//            throw new RuntimeException("To use getAverageGPUtime() enable GPU timing in the ApplicationConfiguration.");
        if(numSamples == 0)
            return 0;
        float avg = 0.001f * (float) cumulative / (float)numSamples;
        //avg = cumulative;
        resetGPUsamples();
        return avg;
    }

    public void logAverageGPUtime(){
        if(!timingEnabled)
            throw new RuntimeException("logAverageGPUtime(): ApplicationConfiguration.enableGPUtiming is false.");
        System.out.println("average: "+(float)cumulative / (float)numSamples + " numSamples: "+numSamples);
        resetGPUsamples();
    }

    public void resetGPUsamples(){
        numSamples = 0;
        cumulative = 0;
    }


}
