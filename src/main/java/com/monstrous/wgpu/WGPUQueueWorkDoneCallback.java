package com.monstrous.wgpu;

//typedef void (*WGPUQueueWorkDoneCallback)(WGPUQueueWorkDoneStatus status, void * userdata) WGPU_FUNCTION_ATTRIBUTE;

import jnr.ffi.Pointer;
import jnr.ffi.annotations.Delegate;

public interface WGPUQueueWorkDoneCallback {

    @Delegate
    void invoke(WGPUQueueWorkDoneStatus status, Pointer userdata);
}
