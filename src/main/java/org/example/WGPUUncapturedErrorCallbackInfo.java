package org.example;

import jnr.ffi.Struct;

public class WGPUUncapturedErrorCallbackInfo extends WgpuJavaStruct {
    public final Struct.Pointer nextInChain = new Struct.Pointer();

    private final Struct.Pointer callback = new Struct.Pointer();       // todo callback
    private final Struct.Pointer userdata1 = new Struct.Pointer();
    private final Struct.Pointer userdata2 = new Struct.Pointer();
}