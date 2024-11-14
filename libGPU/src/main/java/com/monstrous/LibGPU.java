package com.monstrous;

import com.monstrous.wgpu.WGPU;
import jnr.ffi.Pointer;

public class LibGPU {
    public static Application application;
    public static WGPU wgpu;
    public static Pointer instance;
    public static Pointer surface;
    public static Pointer device;
    public static Pointer queue;
}
