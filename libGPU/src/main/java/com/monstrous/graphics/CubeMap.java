package com.monstrous.graphics;

import com.monstrous.webgpu.WGPUTextureFormat;
import com.monstrous.webgpu.WGPUTextureUsage;

public class CubeMap extends TextureArray {

    public CubeMap(int width, int height ){
        this(width, height, false);
    }

    public CubeMap(int width, int height, boolean mipMapping ){
        this.width = width;
        this.height = height;
        this.numLayers = 6;
        mipLevelCount = mipMapping ? Math.max(1, bitWidth(Math.max(width, height))) : 1;
        int textureUsage = WGPUTextureUsage.TextureBinding | WGPUTextureUsage.CopyDst;
        create( "cube map", mipLevelCount, textureUsage,  WGPUTextureFormat.RGBA8Unorm, numLayers, 1, null);
    }

    public CubeMap(String[] fileNames, boolean mipMapping) {
        super(fileNames, mipMapping);
    }

    public CubeMap(String[] fileNames, String extension, int lodLevels){
        super(fileNames, extension, lodLevels);
    }
}
