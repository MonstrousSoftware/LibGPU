package com.monstrous.wgpu;


//typedef struct WGPUSurfaceTexture {
//        WGPUTexture texture;
//        WGPUBool suboptimal;
//        WGPUSurfaceGetCurrentTextureStatus status;
//        } WGPUSurfaceTexture WGPU_STRUCTURE_ATTRIBUTE;

import com.monstrous.utils.WgpuJavaStruct;
import jnr.ffi.Struct;

public class WGPUSurfaceTexture extends WgpuJavaStruct {
    public final Struct.Pointer texture = new Struct.Pointer();
    public final Struct.WBOOL suboptimal = new Struct.WBOOL();
    public final  Struct.Enum32<WGPUSurfaceGetCurrentTextureStatus> status = new Struct.Enum<>(WGPUSurfaceGetCurrentTextureStatus.class);
}
