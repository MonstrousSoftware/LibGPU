package com.monstrous;

import com.monstrous.wgpu.WebGPU;
import com.monstrous.wgpu.WGPUSupportedLimits;
import com.monstrous.wgpu.WGPUTextureFormat;
import jnr.ffi.Pointer;

public class LibGPU {
    public static Application app;
    public static Input input;
    public static Graphics graphics;
    public static WebGPU webGPU;

    // put the following under wgpu?
    public static Pointer instance;
    public static Pointer surface;
    public static WGPUTextureFormat surfaceFormat = WGPUTextureFormat.Undefined;
    public static WGPUSupportedLimits supportedLimits;
    public static Pointer device;
    public static Pointer queue;
}
