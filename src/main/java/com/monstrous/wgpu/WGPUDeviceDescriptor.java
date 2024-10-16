package com.monstrous.wgpu;

//typedef struct WGPUDeviceDescriptor {
//    WGPUChainedStruct const * nextInChain;
//    /**
//     * If the null value is passed, this defaults to the empty string.
//     */
//    WGPUStringView label;
//    size_t requiredFeatureCount;
//    WGPUFeatureName const * requiredFeatures;
//    WGPU_NULLABLE WGPURequiredLimits const * requiredLimits;
//    WGPUQueueDescriptor defaultQueue;
//    WGPUDeviceLostCallbackInfo deviceLostCallbackInfo;
//    WGPUUncapturedErrorCallbackInfo uncapturedErrorCallbackInfo;
//} WGPUDeviceDescriptor WGPU_STRUCTURE_ATTRIBUTE;

import com.monstrous.utils.RustCString;
import com.monstrous.utils.WgpuJavaStruct;
import jnr.ffi.Struct;

public class WGPUDeviceDescriptor extends WgpuJavaStruct {
    public final Pointer nextInChain = new Pointer();

    public final Struct.Pointer label = new Struct.Pointer();
    public final Struct.size_t requiredFeatureCount = new Struct.size_t();
    public final Struct.Pointer requiredFeatures = new Struct.Pointer();
    public final Struct.Pointer requiredLimits = new Struct.Pointer();
    public final WGPUQueueDescriptor defaultQueue = inner(new WGPUQueueDescriptor());
    public final WGPUDeviceLostCallbackInfo deviceLostCallbackInfo = inner(new WGPUDeviceLostCallbackInfo());
    public final WGPUUncapturedErrorCallbackInfo uncapturedErrorCallbackInfo = inner(new WGPUUncapturedErrorCallbackInfo());

    public java.lang.String getLabel(){
        return RustCString.fromPointer(label.get());
    }

    public void setLabel(java.lang.String x){
        this.label.set(RustCString.toPointer(x));
    }
}
