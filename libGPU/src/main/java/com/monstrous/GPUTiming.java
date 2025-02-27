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

    public GPUTiming(Pointer device, boolean enabled) {
        this.timingEnabled = enabled;
        if(!timingEnabled)
            return;

        // Create timestamp queries
        WGPUQuerySetDescriptor querySetDescriptor =  WGPUQuerySetDescriptor.createDirect();
        querySetDescriptor.setNextInChain();
        querySetDescriptor.setLabel("Timestamp Query Set");
        querySetDescriptor.setType(WGPUQueryType.Timestamp);
        querySetDescriptor.setCount(2); // start and end

        timestampQuerySet = webGPU.wgpuDeviceCreateQuerySet(device, querySetDescriptor);

        // Create buffer
        WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
        bufferDesc.setLabel("timestamp resolve buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopySrc | WGPUBufferUsage.QueryResolve );
        bufferDesc.setSize(32);
        bufferDesc.setMappedAtCreation(0L);
        timeStampResolveBuffer = webGPU.wgpuDeviceCreateBuffer(device, bufferDesc);

        bufferDesc.setLabel("timestamp map buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.MapRead );
        bufferDesc.setSize(32);
        timeStampMapBuffer = webGPU.wgpuDeviceCreateBuffer(device, bufferDesc);
    }

    // call this before configuration of render pass
    public void configureRenderPassDescriptor(WGPURenderPassDescriptor renderPassDescriptor){
        if(timingEnabled) {
            WGPURenderPassTimestampWrites start = WGPURenderPassTimestampWrites.createDirect();
            start.setBeginningOfPassWriteIndex(0);
            start.setEndOfPassWriteIndex(1);
            start.setQuerySet(timestampQuerySet);

            renderPassDescriptor.setTimestampWrites(start);
        } else {
            renderPassDescriptor.setTimestampWrites();
        }
    }

    public void resolveTimeStamps(Pointer encoder){
        if(!timingEnabled || timeStampMapOngoing)
            return;

        // Resolve the timestamp queries (write their result to the resolve buffer)
        webGPU.wgpuCommandEncoderResolveQuerySet(encoder, timestampQuerySet, 0, 2, timeStampResolveBuffer, 0);

        // Copy to the map buffer
        webGPU.wgpuCommandEncoderCopyBufferToBuffer(encoder, timeStampResolveBuffer, 0,  timeStampMapBuffer, 0,32);
    }


    public void fetchTimestamps(){
        if(!timingEnabled || timeStampMapOngoing)
            return;

        // use a lambda expression to define a callback function
        WGPUBufferMapCallback onTimestampBufferMapped = (WGPUBufferMapAsyncStatus status, Pointer userData) -> {
            if(status != WGPUBufferMapAsyncStatus.Success)
                System.out.println("*** ERROR: Timestamp buffer mapped with status: " + status);
            else {
                Pointer ram =  webGPU.wgpuBufferGetConstMappedRange(timeStampMapBuffer, 0, 32);
                long start = ram.getLong(0);
                long end = ram.getLong(Long.BYTES);
                webGPU.wgpuBufferUnmap(timeStampMapBuffer);
                long ns = end - start;
                int microseconds = (int)(0.001 * ns);
                addTimeSample(microseconds);
//                System.out.println("us :"+microseconds);
            }
            timeStampMapOngoing = false;
        };

        timeStampMapOngoing = true;
        webGPU.wgpuBufferMapAsync(timeStampMapBuffer, WGPUMapMode.Read, 0, 32, onTimestampBufferMapped, null);
    }

    @Override
    public void dispose() {
       if(!timingEnabled)
            return;
        //  timestampQuerySet release
        webGPU.wgpuBufferDestroy(timeStampMapBuffer);
        webGPU.wgpuBufferRelease(timeStampMapBuffer);
        webGPU.wgpuBufferDestroy(timeStampResolveBuffer);
        webGPU.wgpuBufferRelease(timeStampResolveBuffer);
    }

    private long cumulative = 0;
    private int numSamples = 0;

    private void addTimeSample(int us){
        numSamples++;
        cumulative += us;
    }

    // returns average time per frame spent by GPU (in microseconds).
    public float getAverageGPUtime(){
//        if(!timingEnabled)
//            throw new RuntimeException("To use getAverageGPUtime() enable GPU timing in the ApplicationConfiguration.");
        if(numSamples == 0)
            return 0;
        float avg = (float) cumulative / (float)numSamples;
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
