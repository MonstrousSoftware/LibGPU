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
    private int nativeFormat;
    private int mipLevelCount;
    private Pointer image;
    private Pointer texture;
    private Pointer textureView;
    private Pointer sampler;
    private WGPUTextureFormat format;

    public Texture() {
        this(256, 256);
    }

    public Texture(int width, int height){
        this(width, height, true, false, WGPUTextureFormat.RGBA8Unorm, 1);
    }

    public Texture(int width, int height, boolean mipMapping, boolean renderAttachment, WGPUTextureFormat format, int numSamples ) {
        this.width = width;
        this.height = height;
        create( "texture", mipMapping, renderAttachment, format, 1, numSamples);
    }


    public Texture(String fileName) {
        this(fileName, true);
    }

    public Texture(String fileName,boolean mipMapping) {
        this(fileName, mipMapping, false, WGPUTextureFormat.RGBA8Unorm);
    }

    public Texture(String fileName, boolean mipMapping, boolean renderAttachment, WGPUTextureFormat format) {

        byte[] fileData;

        try {
            fileData = Files.readAllBytes(Paths.get(fileName));
            int len = fileData.length;
            Pointer data = WgpuJava.createByteArrayPointer(fileData);

            image = LibGPU.webGPU.gdx2d_load(data, len);        // use native function to parse image file
            //System.out.println("loaded: "+image);

            PixmapInfo info = PixmapInfo.createAt(image);
            this.width = info.width.intValue();
            this.height = info.height.intValue();
            this.nativeFormat = info.format.intValue();
            Pointer pixelPtr = info.pixels.get();
            create( fileName, mipMapping, renderAttachment, format, 1, 1);
            load(pixelPtr, 0);


        } catch (IOException e) {
            throw new RuntimeException("Texture file not found: "+fileName);
        }
    }

    // for a multi-layer texture, e.g. a cube map
    public Texture(String[] fileNames, boolean mipMapping, WGPUTextureFormat format) {

        int numLayers = fileNames.length;

        for(int layer = 0; layer < numLayers; layer++) {

            byte[] fileData;
            try {
                fileData = Files.readAllBytes(Paths.get(fileNames[layer]));
                int len = fileData.length;
                Pointer data = WgpuJava.createByteArrayPointer(fileData);

                image = LibGPU.webGPU.gdx2d_load(data, len);        // use native function to parse image file
                PixmapInfo info = PixmapInfo.createAt(image);

                // use the first image to
                if(layer == 0) {
                    this.width = info.width.intValue();
                    this.height = info.height.intValue();
                    create(fileNames[layer], mipMapping, false, format, numLayers, 1);
                } else {
                    if(info.width.intValue() != width || info.height.intValue() != height)
                        throw new RuntimeException("Texture: layers must have same size");
                }

                this.nativeFormat = info.format.intValue();
                Pointer pixelPtr = info.pixels.get();

                load(pixelPtr, layer);


            } catch (IOException e) {
                throw new RuntimeException("Texture file not found: " + fileNames[layer]);
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    // native format from smb_image
    public int getNativeFormat() {
        return nativeFormat;
    }

    public Pointer getTextureView(){
        return textureView;
    }

    public WGPUTextureFormat getFormat(){
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

    // renderAttachment - will this texture be used for render output
    // numLayers - normally 1, e.g. 6 for a cube map
    // numSamples - for anti-aliasing
    //
    private void create( String label, boolean mipMapping, boolean renderAttachment, WGPUTextureFormat format, int numLayers, int numSamples) {
        if (LibGPU.device == null || LibGPU.queue == null)
            throw new RuntimeException("Texture creation requires device and queue to be available\n");

        mipLevelCount = 1;
        if (mipMapping)
            mipLevelCount = Math.max(1, bitWidth(Math.max(width, height)));      // todo test for non-square, non POT etc.

        // Create the texture
        WGPUTextureDescriptor textureDesc = WGPUTextureDescriptor.createDirect();
        textureDesc.setNextInChain();
        textureDesc.setLabel(label);
        textureDesc.setDimension( WGPUTextureDimension._2D);
        this.format = format; //
        textureDesc.setFormat(format);
        textureDesc.setMipLevelCount(mipLevelCount);
        textureDesc.setSampleCount(numSamples);
        textureDesc.getSize().setWidth(width);
        textureDesc.getSize().setHeight(height);
        textureDesc.getSize().setDepthOrArrayLayers(numLayers);
        if (renderAttachment)
            textureDesc.setUsage(WGPUTextureUsage.TextureBinding | WGPUTextureUsage.CopyDst | WGPUTextureUsage.RenderAttachment);
        else
            textureDesc.setUsage(WGPUTextureUsage.TextureBinding | WGPUTextureUsage.CopyDst);
        textureDesc.setViewFormatCount(0);
        textureDesc.setViewFormats(WgpuJava.createNullPointer());
        texture = LibGPU.webGPU.DeviceCreateTexture(LibGPU.device, textureDesc);

        System.out.println("dimensions: "+textureDesc.getSize().getDepthOrArrayLayers());


        // Create the view of the  texture manipulated by the rasterizer
        WGPUTextureViewDescriptor textureViewDesc = WGPUTextureViewDescriptor.createDirect();
        textureViewDesc.setAspect(WGPUTextureAspect.All);
        textureViewDesc.setBaseArrayLayer(0);
        textureViewDesc.setArrayLayerCount(numLayers);
        textureViewDesc.setBaseMipLevel(0);
        textureViewDesc.setMipLevelCount(mipLevelCount);
        textureViewDesc.setDimension(numLayers == 1 ? WGPUTextureViewDimension._2D : WGPUTextureViewDimension.Cube);    // assume it's a cube map if layers > 1
        textureViewDesc.setFormat(textureDesc.getFormat());
        textureView = LibGPU.webGPU.TextureCreateView(texture, textureViewDesc);

        // Create a sampler
        WGPUSamplerDescriptor samplerDesc = WGPUSamplerDescriptor.createDirect();
        samplerDesc.setAddressModeU(WGPUAddressMode.Repeat);
        samplerDesc.setAddressModeV(WGPUAddressMode.Repeat);
        samplerDesc.setAddressModeW(WGPUAddressMode.Repeat);
        samplerDesc.setMagFilter(WGPUFilterMode.Linear);
        samplerDesc.setMinFilter(WGPUFilterMode.Linear);
        samplerDesc.setMipmapFilter(WGPUMipmapFilterMode.Linear);

        samplerDesc.setLodMinClamp(0);
        samplerDesc.setLodMaxClamp(mipLevelCount);
        samplerDesc.setCompare(WGPUCompareFunction.Undefined);
        samplerDesc.setMaxAnisotropy(1);
        sampler = LibGPU.webGPU.DeviceCreateSampler(LibGPU.device, samplerDesc);
    }

    public void fill(Color color) {
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
        source.setBytesPerRow(4 * width);
        source.setRowsPerImage(height);

        byte[] pixels = new byte[4 * width * height];
        byte r = (byte) (color.r * 255);
        byte g = (byte) (color.g * 255);
        byte b = (byte) (color.b * 255);
        byte a = (byte) (color.a * 255);

        int offset = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[offset++] = r;
                pixels[offset++] = g;
                pixels[offset++] = b;
                pixels[offset++] = a;
            }
        }

        Pointer pixelPtr = WgpuJava.createByteArrayPointer(pixels);

        WGPUExtent3D ext = WGPUExtent3D.createDirect();
        ext.setWidth(width);
        ext.setHeight(height);
        ext.setDepthOrArrayLayers(1);

        destination.setMipLevel(0);

        // N.B. using textureDesc.getSize() for param won't work!
        LibGPU.webGPU.QueueWriteTexture(LibGPU.queue, destination, pixelPtr, width * height * 4, source, ext);
   }

   // layer : which layer to load for a 3d texture, otherwise 0
   private void load(Pointer pixelPtr, int layer) {

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
        // candidate for compute shader

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
//                                pixels[offset++] = (byte) x;
//                                pixels[offset++] = (byte) x;
//                                pixels[offset++] = (byte) x;
                            pixels[offset++] = (byte) ((x / 16) % 2 == (y / 16) % 2 ? 255 : 0);
                            pixels[offset++] = (byte) (((x - y) / 16) % 2 == 0 ? 255 : 0);
                            pixels[offset++] = (byte) (((x + y) / 16) % 2 == 0 ? 255 : 0);
                            pixels[offset++] = (byte) 255;
                        }
                        else {
                            pixels[offset] = convert(pixelPtr.getByte(offset));  offset++;
                            pixels[offset] = convert(pixelPtr.getByte(offset));  offset++;
                            pixels[offset] = convert(pixelPtr.getByte(offset));  offset++;
                            pixels[offset] = pixelPtr.getByte(offset);  offset++;
                        }

                    } else {
                        // Get the corresponding 4 pixels from the previous level
                        int offset00 =  4 * ((2*y+0) * (2*mipLevelWidth) + (2*x+0));
                        int offset01 =  4 * ((2*y+0) * (2*mipLevelWidth) + (2*x+1));
                        int offset10 =  4 * ((2*y+1) * (2*mipLevelWidth) + (2*x+0));
                        int offset11 =  4 * ((2*y+1) * (2*mipLevelWidth) + (2*x+1));

                        // Average r, g and b components
                        // beware that java bytes are signed. So we convert to integer first
                        int r = toUnsignedInt(prevPixels[offset00])   + toUnsignedInt(prevPixels[offset01])   + toUnsignedInt(prevPixels[offset10])   + toUnsignedInt(prevPixels[offset11]);
                        int g = toUnsignedInt(prevPixels[offset00+1]) + toUnsignedInt(prevPixels[offset01+1]) + toUnsignedInt(prevPixels[offset10+1]) + toUnsignedInt(prevPixels[offset11+1]);
                        int b = toUnsignedInt(prevPixels[offset00+2]) + toUnsignedInt(prevPixels[offset01+2]) + toUnsignedInt(prevPixels[offset10+2]) + toUnsignedInt(prevPixels[offset11+2]);
                        int a = toUnsignedInt(prevPixels[offset00+3]) + toUnsignedInt(prevPixels[offset01+3]) + toUnsignedInt(prevPixels[offset10+3]) + toUnsignedInt(prevPixels[offset11+3]);
                        pixels[offset++] = (byte)(r>>2);    // divide by 4
                        pixels[offset++] = (byte)(g>>2);
                        pixels[offset++] = (byte)(b>>2);
                        pixels[offset++] = (byte)(a>>2);  // alpha
                    }

                }
            }

            destination.setMipLevel(mipLevel);
            destination.getOrigin().setZ(layer);

            source.setBytesPerRow(4*mipLevelWidth);
            source.setRowsPerImage(mipLevelHeight);

            ext.setWidth(mipLevelWidth);
            ext.setHeight(mipLevelHeight);
            ext.setDepthOrArrayLayers(1);

            // wrap byte array in native pointer
            Pointer pixelData = WgpuJava.createByteArrayPointer(pixels);
            // N.B. using textureDesc.getSize() for param won't work!
            LibGPU.webGPU.QueueWriteTexture(LibGPU.queue, destination, pixelData, mipLevelWidth * mipLevelHeight * 4, source, ext);

            mipLevelWidth /= 2;
            mipLevelHeight /= 2;
            prevPixels = pixels;
        }


    }

    private static int toUnsignedInt(byte x) {
        return ((int) x) & 0xff;
    }

    private byte convert(byte input){
        return input;
        //return (byte) (255f*Math.pow(input/255f, 0.44f));
    }

    public void dispose(){
        if(image != null) {
            //System.out.println("free: "+image);
            LibGPU.webGPU.gdx2d_free(image);
        }
        LibGPU.webGPU.TextureViewRelease(textureView);
        LibGPU.webGPU.TextureDestroy(texture);
        LibGPU.webGPU.TextureRelease(texture);
    }
}
