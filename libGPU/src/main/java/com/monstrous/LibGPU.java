package com.monstrous;

import com.monstrous.webgpu.WGPUSupportedLimits;
import com.monstrous.webgpu.WGPUTextureFormat;
import com.monstrous.webgpu.WebGPU_JNI;
import jnr.ffi.Pointer;

public class LibGPU {
    public static Application app;
    public static Input input;
    public static Graphics graphics;
    public static WebGPU_JNI webGPU;

    // put the following under wgpu?
    public static Pointer instance;
    public static Pointer surface;
    public static WGPUTextureFormat surfaceFormat = WGPUTextureFormat.Undefined;
    public static WGPUSupportedLimits supportedLimits;
    public static Pointer device;
    public static Pointer queue;
}
