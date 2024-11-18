package com.monstrous.graphics;

import com.monstrous.LibGPU;
import com.monstrous.wgpuUtils.WgpuJava;
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
    private Pointer texture;
    private Pointer textureView;
    private Pointer sampler;

    public Texture() {
        this(256, 256);
    }

    public Texture(int width, int height){
        this(width, height, true);
    }

    public Texture(int width, int height, boolean mipMapping) {
        this.width = width;
        this.height = height;
        load(null, mipMapping);
    }

    public Texture(String fileName) {
        this(fileName, true);
    }

    public Texture(String fileName, boolean mipMapping) {
        this();
        byte[] fileData;

        try {
            fileData = Files.readAllBytes(Paths.get(fileName));
            int len = fileData.length;
            Pointer data = WgpuJava.createByteArrayPointer(fileData);

            image = LibGPU.wgpu.gdx2d_load(data, len);        // use native function to parse image file
            //System.out.println("loaded: "+image);

            PixmapInfo info = PixmapInfo.createAt(image);
            this.width = info.width.intValue();
            this.height = info.height.intValue();
            this.format = info.format.intValue();
            Pointer pixelPtr = info.pixels.get();
            load(pixelPtr, mipMapping);


        } catch (IOException e) {
            throw new RuntimeException("Texture file not found: "+fileName);
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

    public WGPUBindGroupEntry getSamplerBinding(int index) {
        WGPUBindGroupEntry binding = WGPUBindGroupEntry.createDirect();
        binding.setNextInChain();
        binding.setBinding(index);  // binding index
        binding.setSampler(sampler);
        return binding;
    }

    private int bitWidth(int value) {
        if (value == 0)
            return 0;
        else {
            int w = 0;
            while ((value >>= 1) > 0)
                ++w;
            return w;
        }
    }

    private void load(Pointer pixelPtr, boolean mipMapping) {
        if(LibGPU.device == null || LibGPU.queue == null )
            throw new RuntimeException("Texture creation requires device and queue to be available\n");

        int mipLevelCount = 1;
        if(mipMapping)
            mipLevelCount = bitWidth(Math.max(width, height));      // todo test for non-square, non POT etc.

        // Create the texture
        WGPUTextureDescriptor textureDesc = WGPUTextureDescriptor.createDirect();
        textureDesc.setNextInChain();
        textureDesc.setDimension(WGPUTextureDimension._2D);
        textureDesc.setFormat(WGPUTextureFormat.RGBA8Unorm);
        textureDesc.setMipLevelCount(mipLevelCount);
        textureDesc.setSampleCount(1);
        textureDesc.getSize().setWidth(width);
        textureDesc.getSize().setHeight(height);
        textureDesc.getSize().setDepthOrArrayLayers(1);
        textureDesc.setUsage(WGPUTextureUsage.TextureBinding | WGPUTextureUsage.CopyDst);
        textureDesc.setViewFormatCount(0);
        textureDesc.setViewFormats(WgpuJava.createNullPointer());
        texture = LibGPU.wgpu.DeviceCreateTexture(LibGPU.device, textureDesc);

        // Create the view of the  texture manipulated by the rasterizer
        WGPUTextureViewDescriptor textureViewDesc = WGPUTextureViewDescriptor.createDirect();
        textureViewDesc.setAspect(WGPUTextureAspect.All);
        textureViewDesc.setBaseArrayLayer(0);
        textureViewDesc.setArrayLayerCount(1);
        textureViewDesc.setBaseMipLevel(0);
        textureViewDesc.setMipLevelCount(mipLevelCount);
        textureViewDesc.setDimension( WGPUTextureViewDimension._2D);
        textureViewDesc.setFormat( textureDesc.getFormat() );
        textureView = LibGPU.wgpu.TextureCreateView(texture, textureViewDesc);

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

        // Generate mipmap levels

        int mipLevelWidth = width;
        int mipLevelHeight = height;

        WGPUExtent3D ext = WGPUExtent3D.createDirect();

        byte[] prevPixels = null;
        for(int mipLevel = 0; mipLevel < mipLevelCount; mipLevel++) {

                byte[] pixels = new byte[4 * mipLevelWidth * mipLevelHeight];

                int offset = 0;
                for (int y = 0; y < mipLevelHeight; y++) {
                    for (int x = 0; x < mipLevelWidth; x++) {
                        if(mipLevel == 0) {
                            if(pixelPtr == null) {
                                // generate test pattern
                                pixels[offset++] = (byte) ((x / 16) % 2 == (y / 16) % 2 ? 255 : 0);
                                pixels[offset++] = (byte) (((x - y) / 16) % 2 == 0 ? 255 : 0);
                                pixels[offset++] = (byte) (((x + y) / 16) % 2 == 0 ? 255 : 0);
                                pixels[offset++] = (byte) 255;
                            }
                            else {
                                pixels[offset] = pixelPtr.getByte(offset);  offset++;
                                pixels[offset] = pixelPtr.getByte(offset);  offset++;
                                pixels[offset] = pixelPtr.getByte(offset);  offset++;
                                pixels[offset] = pixelPtr.getByte(offset);  offset++;
                            }

                        } else {
                            // Get the corresponding 4 pixels from the previous level
                            int offset00 =  4 * ((2*y+0) * (2*mipLevelWidth) + (2*x+0));
                            int offset01 =  4 * ((2*y+0) * (2*mipLevelWidth) + (2*x+1));
                            int offset10 =  4 * ((2*y+1) * (2*mipLevelWidth) + (2*x+0));
                            int offset11 =  4 * ((2*y+1) * (2*mipLevelWidth) + (2*x+1));

                            // Average r, g and b components
                            pixels[offset++] = (byte)((prevPixels[offset00]+prevPixels[offset01]+prevPixels[offset10]+prevPixels[offset11])/4);     // r
                            pixels[offset++] = (byte)((prevPixels[offset00+1]+prevPixels[offset01+1]+prevPixels[offset10+1]+prevPixels[offset11+1])/4); // g
                            pixels[offset++] = (byte)((prevPixels[offset00+2]+prevPixels[offset01+2]+prevPixels[offset10+2]+prevPixels[offset11+2])/4); // b
                            pixels[offset++] = (byte) 255;
                        }

                    }
                }
                pixelPtr = WgpuJava.createByteArrayPointer(pixels);


            destination.setMipLevel(mipLevel);

            source.setBytesPerRow(4*mipLevelWidth);
            source.setRowsPerImage(mipLevelHeight);

            ext.setWidth(mipLevelWidth);
            ext.setHeight(mipLevelHeight);
            ext.setDepthOrArrayLayers(1);

            // N.B. using textureDesc.getSize() for last param won't work!
            LibGPU.wgpu.QueueWriteTexture(LibGPU.queue, destination, pixelPtr, mipLevelWidth * mipLevelHeight * 4, source, ext);

            mipLevelWidth /= 2;
            mipLevelHeight /= 2;
            prevPixels = pixels;
        }

        // Create a sampler
        WGPUSamplerDescriptor samplerDesc = WGPUSamplerDescriptor.createDirect();
        samplerDesc.setAddressModeU( WGPUAddressMode.ClampToEdge);
        samplerDesc.setAddressModeV( WGPUAddressMode.ClampToEdge);
        samplerDesc.setAddressModeW( WGPUAddressMode.ClampToEdge);
        samplerDesc.setMagFilter( WGPUFilterMode.Linear);
        samplerDesc.setMinFilter( WGPUFilterMode.Linear);
        samplerDesc.setMipmapFilter( WGPUMipmapFilterMode.Linear);

        samplerDesc.setLodMinClamp(1);
        samplerDesc.setLodMaxClamp(mipLevelCount);
        samplerDesc.setCompare( WGPUCompareFunction.Undefined);
        samplerDesc.setMaxAnisotropy( 1);
        sampler = LibGPU.wgpu.DeviceCreateSampler(LibGPU.device, samplerDesc);
    }

    public void dispose(){
        if(image != null) {
            //System.out.println("free: "+image);
            LibGPU.wgpu.gdx2d_free(image);
        }
        LibGPU.wgpu.TextureViewRelease(textureView);
        LibGPU.wgpu.TextureDestroy(texture);
        LibGPU.wgpu.TextureRelease(texture);
    }
}
