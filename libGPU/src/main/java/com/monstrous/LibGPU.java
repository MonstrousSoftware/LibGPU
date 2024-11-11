package com.monstrous;

import com.monstrous.wgpu.WGPU;
import jnr.ffi.Pointer;

public class LibGPU {
    public static WGPU wgpu;
    public static Pointer device;
    public static Pointer queue;
}
