package com.monstrous.graphics.webgpu;

import com.monstrous.LibGPU;
import com.monstrous.graphics.Texture;
import com.monstrous.utils.Disposable;
import com.monstrous.webgpu.WGPUTextureAspect;
import com.monstrous.webgpu.WGPUTextureFormat;
import com.monstrous.webgpu.WGPUTextureViewDescriptor;
import com.monstrous.webgpu.WGPUTextureViewDimension;
import jnr.ffi.Pointer;

public class TextureView implements Disposable {
    private final Pointer textureView;
    public WGPUTextureAspect aspect;
    public WGPUTextureViewDimension dimension;
    public WGPUTextureFormat format;
    public int baseMipLevel;
    public int mipLevelCount;
    public int baseArrayLayer;
    public int arrayLayerCount;


    public TextureView(Texture texture) {
        this(texture, 1, 1);
    }

    public TextureView(Texture texture, int mipLevelCount, int arrayLayerCount) {
        this(texture, WGPUTextureAspect.All, WGPUTextureViewDimension._2D, WGPUTextureFormat.RGBA8Unorm, 0, mipLevelCount, 0, arrayLayerCount);
    }

    public TextureView(Texture texture, WGPUTextureAspect aspect, WGPUTextureViewDimension dimension, WGPUTextureFormat format,
                       int baseMipLevel, int mipLevelCount, int baseArrayLayer, int arrayLayerCount) {
        this.aspect = aspect;
        this.dimension = dimension;
        this.format = format;
        this.baseMipLevel = baseMipLevel;
        this.mipLevelCount = mipLevelCount;
        this.baseArrayLayer = baseArrayLayer;
        this.arrayLayerCount = arrayLayerCount;

        // Create the view of the  texture manipulated by the rasterizer
        WGPUTextureViewDescriptor textureViewDesc = WGPUTextureViewDescriptor.createDirect();   // todo reuse
        textureViewDesc.setAspect(WGPUTextureAspect.All);
        textureViewDesc.setBaseArrayLayer(baseArrayLayer);
        textureViewDesc.setArrayLayerCount(arrayLayerCount);
        textureViewDesc.setBaseMipLevel(baseMipLevel);
        textureViewDesc.setMipLevelCount(mipLevelCount);
        textureViewDesc.setDimension(dimension);
        textureViewDesc.setFormat(texture.getFormat());
        textureView = LibGPU.webGPU.wgpuTextureCreateView(texture.getHandle(), textureViewDesc);
    }

    public Pointer getHandle(){
        return textureView;
    }

    @Override
    public void dispose() {
        LibGPU.webGPU.wgpuTextureViewRelease(textureView);
    }


}
