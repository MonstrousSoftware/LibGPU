package com.monstrous.graphics;

import com.monstrous.LibGPU;
import com.monstrous.utils.WgpuJava;
import com.monstrous.wgpu.*;
import jnr.ffi.Pointer;

public class Texture {
    private int width;
    private int height;
    private Pointer texture;
    private Pointer textureView;

    public Texture() {
        this(256, 256);
    }

    public Texture(int width, int height) {
        this.width = width;
        this.height = height;
        createTexture();
    }

    public Texture(String fileName) {
        this(); // todo
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public WGPUBindGroupEntry getBinding(int index) {
        WGPUBindGroupEntry texBinding = WGPUBindGroupEntry.createDirect();
        texBinding.setNextInChain();
        texBinding.setBinding(index);  // binding index
        texBinding.setTextureView(textureView);
        return texBinding;
    }


    private void createTexture() {

        // Create the texture
        WGPUTextureDescriptor textureDesc = WGPUTextureDescriptor.createDirect();
        textureDesc.setNextInChain();
        textureDesc.setDimension(WGPUTextureDimension._2D);
        textureDesc.setFormat(WGPUTextureFormat.RGBA8Unorm);
        textureDesc.setMipLevelCount(1);
        textureDesc.setSampleCount(1);
        textureDesc.getSize().setWidth(width);
        textureDesc.getSize().setHeight(height);
        textureDesc.getSize().setDepthOrArrayLayers(1);
        textureDesc.setUsage(WGPUTextureUsage.TextureBinding | WGPUTextureUsage.CopyDst);
        textureDesc.setViewFormatCount(0);
        textureDesc.setViewFormats(WgpuJava.createNullPointer());
        texture = LibGPU.wgpu.DeviceCreateTexture(LibGPU.device, textureDesc);

        // Create the view of the depth texture manipulated by the rasterizer
        WGPUTextureViewDescriptor textureViewDesc = WGPUTextureViewDescriptor.createDirect();
        textureViewDesc.setAspect(WGPUTextureAspect.All);
        textureViewDesc.setBaseArrayLayer(0);
        textureViewDesc.setArrayLayerCount(1);
        textureViewDesc.setBaseMipLevel(0);
        textureViewDesc.setMipLevelCount(1);
        textureViewDesc.setDimension( WGPUTextureViewDimension._2D);
        textureViewDesc.setFormat( textureDesc.getFormat() );
        textureView = LibGPU.wgpu.TextureCreateView(texture, textureViewDesc);

        byte[] pixels = new byte[4 * width * height];
        int offset = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[offset++] = (byte) ((x/16) % 2 == (y/16) % 2 ? 255 : 0);
                pixels[offset++] = (byte) (((x-y)/16) % 2 == 0 ? 255 : 0);
                pixels[offset++] = (byte) (((x+y)/16) % 2 == 0 ? 255 : 0);
                pixels[offset++] = (byte) 255;
            }
        }

        // Arguments telling which part of the texture we upload to
        // (together with the last argument of writeTexture)
        WGPUImageCopyTexture destination = WGPUImageCopyTexture.createDirect();
        destination.setTexture(texture);
        destination.setMipLevel(0);
        destination.getOrigin().setX(0);
        destination.getOrigin().setY(0);
        destination.getOrigin().setZ(0);
        destination.setAspect(WGPUTextureAspect.All);   // not relevant

        // Arguments telling how the C++ side pixel memory is laid out
        WGPUTextureDataLayout source = WGPUTextureDataLayout.createDirect();
        source.setOffset(0);
        source.setBytesPerRow(4*width);
        source.setRowsPerImage(height);

        Pointer pixelPtr = WgpuJava.createByteArrayPointer(pixels);

        WGPUExtent3D ext = WGPUExtent3D.createDirect();
        ext.setWidth(width);
        ext.setHeight(height);
        ext.setDepthOrArrayLayers(1);

        // N.B. using textureDesc.getSize() as last param doesn't work!
        LibGPU.wgpu.QueueWriteTexture(LibGPU.queue, destination, pixelPtr, width*height*4, source, ext);
    }

    public void dispose(){
        LibGPU.wgpu.TextureViewRelease(textureView);
        LibGPU.wgpu.TextureDestroy(texture);
        LibGPU.wgpu.TextureRelease(texture);
    }
}
