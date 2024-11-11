package com.monstrous.graphics;

import com.monstrous.LibGPU;
import com.monstrous.utils.WgpuJava;
import com.monstrous.wgpu.*;
import jnr.ffi.Pointer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Texture {
    private int width;
    private int height;
    private int format;
    private Pointer image;
    private Pointer pixelPtr;
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
        this();
        byte[] fileData;

        try {
            fileData = Files.readAllBytes(Paths.get(fileName));
            int len = fileData.length;
            Pointer data = WgpuJava.createByteArrayPointer(fileData);

            image = LibGPU.wgpu.gdx2d_load(data, len);        // use native function to parse image file

            PixmapInfo info = PixmapInfo.createAt(image);
            this.width = info.width.intValue();
            this.height = info.height.intValue();
            this.format = info.format.intValue();
            pixelPtr = info.pixels.get();
            createTexture();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    // native format from smb_image
    public int getFormat() {
        return format;
    }

    public WGPUBindGroupEntry getBinding(int index) {
        WGPUBindGroupEntry texBinding = WGPUBindGroupEntry.createDirect();
        texBinding.setNextInChain();
        texBinding.setBinding(index);  // binding index
        texBinding.setTextureView(textureView);
        return texBinding;
    }


    private void createTexture() {
        if(LibGPU.device == null || LibGPU.queue == null )
            throw new RuntimeException("Texture creation requires device and queue to be available\n");

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

        if(pixelPtr == null) {
            byte[] pixels = new byte[4 * width * height];
            int offset = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixels[offset++] = (byte) ((x / 16) % 2 == (y / 16) % 2 ? 255 : 0);
                    pixels[offset++] = (byte) (((x - y) / 16) % 2 == 0 ? 255 : 0);
                    pixels[offset++] = (byte) (((x + y) / 16) % 2 == 0 ? 255 : 0);
                    pixels[offset++] = (byte) 255;
                }
            }
            pixelPtr = WgpuJava.createByteArrayPointer(pixels);
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


        WGPUExtent3D ext = WGPUExtent3D.createDirect();
        ext.setWidth(width);
        ext.setHeight(height);
        ext.setDepthOrArrayLayers(1);

        // N.B. using textureDesc.getSize() for last param won't work!
        LibGPU.wgpu.QueueWriteTexture(LibGPU.queue, destination, pixelPtr, width*height*4, source, ext);
    }

    public void dispose(){
        if(image != null)
            LibGPU.wgpu.gdx2d_free(image);
        LibGPU.wgpu.TextureViewRelease(textureView);
        LibGPU.wgpu.TextureDestroy(texture);
        LibGPU.wgpu.TextureRelease(texture);
    }
}
