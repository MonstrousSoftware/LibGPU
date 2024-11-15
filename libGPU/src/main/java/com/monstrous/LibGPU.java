package com.monstrous;

import com.monstrous.wgpu.WGPU;
import com.monstrous.wgpu.WGPUSupportedLimits;
import com.monstrous.wgpu.WGPUTextureFormat;
import jnr.ffi.Pointer;

public class LibGPU {
    public static Application application;
    public static Input input;
    public static Graphics graphics;
    public static WGPU wgpu;

    // put the following under wgpu?
    public static Pointer instance;
    public static Pointer surface;
    public static WGPUTextureFormat surfaceFormat = WGPUTextureFormat.Undefined;
    public static WGPUSupportedLimits supportedLimits;
    public static Pointer device;
    public static Pointer queue;
}
