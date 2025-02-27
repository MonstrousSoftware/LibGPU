package com.monstrous.graphics;

import com.monstrous.webgpu.WGPUVertexFormat;

public class VertexAttribute {

    public static class Usage {
        static public final long POSITION = 1;
        static public final long COLOR = 2;
        static public final long TEXTURE_COORDINATE = 4;
        static public final long NORMAL= 8;
        static public final long TANGENT = 16;
        static public final long BITANGENT = 32;
    }


    public String label;
    public WGPUVertexFormat format;
    public int shaderLocation;
    public long usage;

    public VertexAttribute(long usage, String name, WGPUVertexFormat format, int shaderLocation) {
        this.usage = usage;
        this.label = name;
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
