package com.monstrous.graphics;

import com.monstrous.wgpu.WGPUVertexFormat;

public class VertexAttribute {
    public String name;
    public WGPUVertexFormat format;
    public int shaderLocation;

    public VertexAttribute(String name, WGPUVertexFormat format, int shaderLocation) {
        this.name = name;
        this.format = format;
        this.shaderLocation = shaderLocation;
    }

    public int getSize(){
        switch(format){

            case Uint8x2:
            case Sint8x2:
            case Unorm8x2:
            case Snorm8x2:
                return 2;

            case Uint8x4:
            case Unorm8x4:
            case Sint8x4:
            case Snorm8x4:
            case Uint16x2:
            case Sint16x2:
            case Unorm16x2:
            case Snorm16x2:
            case Uint32:
            case Sint32:
            case Float16x2:
            case Float32:
            case Unorm1010102:
                return 4;

            case Uint16x4:
            case Sint16x4:
            case Unorm16x4:
            case Snorm16x4:
            case Float16x4:
            case Float32x2:
            case Uint32x2:
            case Sint32x2:
                return 8;

            case Float32x3:
            case Uint32x3:
            case Sint32x3:
                return 12;

            case Float32x4:
            case Uint32x4:
            case Sint32x4:
                 return 16;

            case Undefined:
            default:
                throw new RuntimeException("Unknown vertex format: "+format);

        }
    }
}
