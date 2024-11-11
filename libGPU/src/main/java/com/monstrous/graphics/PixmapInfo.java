package com.monstrous.graphics;

import com.monstrous.utils.WgpuJavaStruct;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;


public class PixmapInfo extends WgpuJavaStruct {

    public final Struct.Unsigned32 width = new Struct.Unsigned32();
    public final Struct.Unsigned32 height = new Struct.Unsigned32();
    public final Struct.Unsigned32 format = new Struct.Unsigned32();
    public final Struct.Unsigned32 blend = new Struct.Unsigned32();
    public final Struct.Unsigned32 scale = new Struct.Unsigned32();
    public final Struct.Pointer pixels = new Struct.Pointer();

    private PixmapInfo(){}

    @Deprecated
    public PixmapInfo(Runtime runtime){
        super(runtime);
    }

    public static PixmapInfo createAt(jnr.ffi.Pointer address){
        var struct = new PixmapInfo();
        struct.useMemory(address);
        return struct;
    }
}