/*******************************************************************************
 * Copyright 2025 Monstrous Software.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.monstrous.graphics;

import com.monstrous.FileHandle;
import com.monstrous.Files;
import com.monstrous.LibGPU;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.webgpu.*;
import jnr.ffi.Pointer;


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
    private String label;


    public Texture() {
        this(256, 256);
    }

    public Texture(int width, int height){
        this(width, height, true, false, WGPUTextureFormat.RGBA8Unorm, 1);
    }

    public Texture(int width, int height, boolean mipMapping, boolean renderAttachment, WGPUTextureFormat format, int numSamples ) {
        this.width = width;
        this.height = height;
        mipLevelCount = 1;
        if (mipMapping)
            mipLevelCount = Math.max(1, bitWidth(Math.max(width, height)));      // todo test for non-square, non POT etc.
        create( "texture", mipLevelCount, renderAttachment, format, 1, numSamples);
    }

    public Texture(int width, int height, int numLayers ){
        this.width = width;
        this.height = height;
        create( "3d texture map", 1, false,  WGPUTextureFormat.RGBA8Unorm, numLayers, 1);
    }



    public Texture(String fileName) {
        this(fileName, true);
    }

    public Texture(String fileName,boolean mipMapping) {
        this(Files.internal(fileName), mipMapping);
    }


    public Texture(FileHandle file, boolean mipMapping){
        this(file, mipMapping, false, WGPUTextureFormat.RGBA8Unorm);
    }

    public Texture(String fileName, boolean mipMapping, boolean renderAttachment, WGPUTextureFormat format) {
        this(Files.internal(fileName), mipMapping, renderAttachment, format);
    }


    public Texture(FileHandle file, boolean mipMapping, boolean renderAttachment, WGPUTextureFormat format) {
        byte[] fileData = file.readAllBytes();

        Pointer data = JavaWebGPU.createByteArrayPointer(fileData);
        image = JavaWebGPU.getUtils().gdx2d_load(data, fileData.length);        // use native function to parse image file

        PixmapInfo info = PixmapInfo.createAt(image);
        this.width = info.width.intValue();
        this.height = info.height.intValue();
        this.nativeFormat = info.format.intValue();
        Pointer pixelPtr = info.pixels.get();
//        if(nativeFormat == 12) { // HDR image
//            format = WGPUTextureFormat.RGBA16Float;
//            System.out.println("Reading HDR image: "+file);
//        }

        mipLevelCount = 1;
        if (mipMapping)
            mipLevelCount = Math.max(1, bitWidth(Math.max(width, height)));      // todo test for non-square, non POT etc.
        create( file.file.getName(),  mipLevelCount, renderAttachment, format, 1, 1);
//        if(nativeFormat == 12)  // HDR image
//            fillHDR(Color.RED); //loadHDR(pixelPtr);            // todo HACK
//        else
            load(pixelPtr, 0);

    }

    public Texture(byte[] byteArray, boolean mipMapping) {
        this(byteArray, "texture", mipMapping, false, WGPUTextureFormat.RGBA8Unorm);
    }

    public Texture(byte[] byteArray, String name, boolean mipMapping) {
        this(byteArray, name, mipMapping, false, WGPUTextureFormat.RGBA8Unorm);
    }

    public Texture(byte[] byteArray, String name, boolean mipMapping, boolean renderAttachment, WGPUTextureFormat format) {

        Pointer data = JavaWebGPU.createByteArrayPointer(byteArray);
        image = JavaWebGPU.getUtils().gdx2d_load(data, byteArray.length);        // use native function to parse image file

        PixmapInfo info = PixmapInfo.createAt(image);
        this.width = info.width.intValue();
        this.height = info.height.intValue();
        this.nativeFormat = info.format.intValue();
        Pointer pixelPtr = info.pixels.get();

        if(nativeFormat == 12) { // HDR image  experimental!!!
            format = WGPUTextureFormat.RGBA16Float;
            System.out.println("Reading HDR image: "+name);
        }
        mipLevelCount = 1;
        if (mipMapping)
            mipLevelCount = Math.max(1, bitWidth(Math.max(width, height)));      // todo test for non-square, non POT etc.
        create( name, mipLevelCount, renderAttachment, format, 1, 1);
        if(nativeFormat == 12)  // HDR image
            loadHDR(pixelPtr);
        else
            load(pixelPtr, 0);
    }

    // for a multi-layer texture, e.g. a cube map
    public Texture(String[] fileNames, boolean mipMapping, WGPUTextureFormat format) {

        int numLayers = fileNames.length;

        for(int layer = 0; layer < numLayers; layer++) {

            byte[] fileData;

                FileHandle handle = Files.internal(fileNames[layer]);
                fileData = handle.readAllBytes();
                int len = fileData.length;
                Pointer data = JavaWebGPU.createByteArrayPointer(fileData);

                image = JavaWebGPU.getUtils().gdx2d_load(data, len);        // use native function to parse image file
                PixmapInfo info = PixmapInfo.createAt(image);

                // use the first image to
                if(layer == 0) {
                    this.width = info.width.intValue();
                    this.height = info.height.intValue();
                    mipLevelCount = 1;
                    if (mipMapping)
                        mipLevelCount = Math.max(1, bitWidth(Math.max(width, height)));
                    create(fileNames[layer], mipLevelCount, false, format, numLayers, 1);
                } else {
                    if(info.width.intValue() != width || info.height.intValue() != height)
                        throw new RuntimeException("Texture: layers must have same size");
                }

                this.nativeFormat = info.format.intValue();
                Pointer pixelPtr = info.pixels.get();

                load(pixelPtr, layer);
        }
    }

    /** Make a cube map from 6 textures */
    public Texture(Texture[] sides) {
        if(sides.length != 6) throw new IllegalArgumentException("Texture cube map requires array of 6 textures.");
        int numLayers = 6;

        this.width = sides[0].width;
        this.height = sides[0].height;
        create("cubemap", sides[0].mipLevelCount, false, sides[0].format, numLayers, 1);

        for(int layer = 0; layer < numLayers; layer++) {

            // ?
        }
    }


    /** Create a 3D Texture with LOD levels from image files
     *
      * @param fileNames    Array of files names for different layers. LOD level and extension will be appended.
     * @param extension     File name extension, e.g. ".png"
     * @param lodLevels     Number of LOD levels
     * @param format
     */
    public Texture(String[] fileNames, String extension, int lodLevels, WGPUTextureFormat format) {

        int numLayers = fileNames.length;

        for(int layer = 0; layer < numLayers; layer++) {

            for(int level = 0; level < lodLevels; level++) {
                byte[] fileData;

                String fileName = fileNames[layer] + level+ extension;

                FileHandle handle = Files.internal(fileName);
                fileData = handle.readAllBytes();
                int len = fileData.length;
                Pointer data = JavaWebGPU.createByteArrayPointer(fileData);

                image = JavaWebGPU.getUtils().gdx2d_load(data, len);        // use native function to parse image file
                PixmapInfo info = PixmapInfo.createAt(image);

                // use the first image to create the texture
                if (layer == 0 && level == 0) {
                    this.width = info.width.intValue();
                    this.height = info.height.intValue();
                    create(fileName, lodLevels, false, format, numLayers, 1);
                } else if (level == 0){
                    if (info.width.intValue() != width || info.height.intValue() != height)
                        throw new RuntimeException("Texture: layers must have same size");
                }

                this.nativeFormat = info.format.intValue();

                loadMipLevel(info, layer, level);
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

    public Pointer getSampler() { return sampler; }

    public WGPUTextureFormat getFormat(){
        return format;
    }

    public Pointer getHandle(){
        return texture;
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
    private void create( String label, int mipLevelCount, boolean renderAttachment, WGPUTextureFormat format, int numLayers, int numSamples) {
        if (LibGPU.device == null || LibGPU.queue == null)
            throw new RuntimeException("Texture creation requires device and queue to be available\n");

        this.label = label;

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
            textureDesc.setUsage(WGPUTextureUsage.TextureBinding | WGPUTextureUsage.CopyDst | WGPUTextureUsage.RenderAttachment | WGPUTextureUsage.CopySrc);    // todo COP|Y_SRC temp
        else
            textureDesc.setUsage(WGPUTextureUsage.TextureBinding | WGPUTextureUsage.CopyDst);
        textureDesc.setViewFormatCount(0);
        textureDesc.setViewFormats(JavaWebGPU.createNullPointer());
        texture = LibGPU.webGPU.wgpuDeviceCreateTexture(LibGPU.device, textureDesc);

        //System.out.println("dimensions: "+textureDesc.getSize().getDepthOrArrayLayers());


        // Create the view of the  texture manipulated by the rasterizer
        WGPUTextureViewDescriptor textureViewDesc = WGPUTextureViewDescriptor.createDirect();
        textureViewDesc.setAspect(WGPUTextureAspect.All);
        textureViewDesc.setBaseArrayLayer(0);
        textureViewDesc.setArrayLayerCount(numLayers);
        textureViewDesc.setBaseMipLevel(0);
        textureViewDesc.setMipLevelCount(mipLevelCount);
        textureViewDesc.setDimension(numLayers == 1 ? WGPUTextureViewDimension._2D : WGPUTextureViewDimension.Cube);    // assume it's a cube map if layers > 1
        textureViewDesc.setFormat(textureDesc.getFormat());
        textureView = LibGPU.webGPU.wgpuTextureCreateView(texture, textureViewDesc);

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
        sampler = LibGPU.webGPU.wgpuDeviceCreateSampler(LibGPU.device, samplerDesc);
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

        Pointer pixelPtr = JavaWebGPU.createByteArrayPointer(pixels);

        WGPUExtent3D ext = WGPUExtent3D.createDirect();
        ext.setWidth(width);
        ext.setHeight(height);
        ext.setDepthOrArrayLayers(1);

        destination.setMipLevel(0);

        // N.B. using textureDesc.getSize() for param won't work!
        LibGPU.webGPU.wgpuQueueWriteTexture(LibGPU.queue, destination, pixelPtr, width * height * 4, source, ext);
   }

    /** fill textures using bytes arranged as r, g, b, a, r, g, b, a, etc.
     * Size of buffer must be 4*width*height
     * */
    public void fill(byte[] pixels) {
        if(pixels.length != 4*width*height) throw new IllegalArgumentException("Texture.fill(): byte array is wrong size.");
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

        Pointer pixelPtr = JavaWebGPU.createByteArrayPointer(pixels);

        WGPUExtent3D ext = WGPUExtent3D.createDirect();
        ext.setWidth(width);
        ext.setHeight(height);
        ext.setDepthOrArrayLayers(1);

        destination.setMipLevel(0);

        // N.B. using textureDesc.getSize() for param won't work!
        LibGPU.webGPU.wgpuQueueWriteTexture(LibGPU.queue, destination, pixelPtr, width * height * 4, source, ext);
    }

    public void fillHDR(Color color) {
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
        source.setBytesPerRow(16 * width);
        source.setRowsPerImage(height);

        float[] pixels = new float[4 * width * height];
//        byte r = (byte) (color.r * 255);
//        byte g = (byte) (color.g * 255);
//        byte b = (byte) (color.b * 255);
//        byte a = (byte) (color.a * 255);

        int offset = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[offset++] = 0; //color.r;
                pixels[offset++] = 0; //color.g;
                pixels[offset++] = 0; //color.b;
                pixels[offset++] = 0; //color.a;
            }
        }

        Pointer pixelPtr = JavaWebGPU.createFloatArrayPointer(pixels);

        WGPUExtent3D ext = WGPUExtent3D.createDirect();
        ext.setWidth(width);
        ext.setHeight(height);
        ext.setDepthOrArrayLayers(1);

        destination.setMipLevel(0);

        // N.B. using textureDesc.getSize() for param won't work!
        LibGPU.webGPU.wgpuQueueWriteTexture(LibGPU.queue, destination, pixelPtr, (long) width * height * 4*4, source, ext);
    }

   // layer : which layer to load for a 3d texture, otherwise 0

    /** Load pixel data into texture.
     *
     * @param pixelPtr
     * @param layer which layer to load in case of a 3d texture, otherwise 0
     */
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

            if(mipLevel == 0){
                // fast copy for most common case: mip level 0
                pixelPtr.get(0, pixels, 0, 4 * mipLevelWidth * mipLevelHeight);
            }
            else {
                // todo with compute shader
                int offset = 0;
                for (int y = 0; y < mipLevelHeight; y++) {
                    for (int x = 0; x < mipLevelWidth; x++) {

                        // Get the corresponding 4 pixels from the previous level
                        int offset00 = 4 * ((2 * y + 0) * (2 * mipLevelWidth) + (2 * x + 0));
                        int offset01 = 4 * ((2 * y + 0) * (2 * mipLevelWidth) + (2 * x + 1));
                        int offset10 = 4 * ((2 * y + 1) * (2 * mipLevelWidth) + (2 * x + 0));
                        int offset11 = 4 * ((2 * y + 1) * (2 * mipLevelWidth) + (2 * x + 1));

                        // Average r, g and b components
                        // beware that java bytes are signed. So we convert to integer first
                        int r = toUnsignedInt(prevPixels[offset00]) + toUnsignedInt(prevPixels[offset01]) + toUnsignedInt(prevPixels[offset10]) + toUnsignedInt(prevPixels[offset11]);
                        int g = toUnsignedInt(prevPixels[offset00 + 1]) + toUnsignedInt(prevPixels[offset01 + 1]) + toUnsignedInt(prevPixels[offset10 + 1]) + toUnsignedInt(prevPixels[offset11 + 1]);
                        int b = toUnsignedInt(prevPixels[offset00 + 2]) + toUnsignedInt(prevPixels[offset01 + 2]) + toUnsignedInt(prevPixels[offset10 + 2]) + toUnsignedInt(prevPixels[offset11 + 2]);
                        int a = toUnsignedInt(prevPixels[offset00 + 3]) + toUnsignedInt(prevPixels[offset01 + 3]) + toUnsignedInt(prevPixels[offset10 + 3]) + toUnsignedInt(prevPixels[offset11 + 3]);
                        pixels[offset++] = (byte) (r >> 2);    // divide by 4
                        pixels[offset++] = (byte) (g >> 2);
                        pixels[offset++] = (byte) (b >> 2);
                        pixels[offset++] = (byte) (a >> 2);  // alpha
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

            if(mipLevel == 0){
                LibGPU.webGPU.wgpuQueueWriteTexture(LibGPU.queue, destination, pixelPtr, mipLevelWidth * mipLevelHeight * 4, source, ext);
            } else {

                // wrap byte array in native pointer
                Pointer pixelData = JavaWebGPU.createByteArrayPointer(pixels);
                // N.B. using textureDesc.getSize() for param won't work!
                LibGPU.webGPU.wgpuQueueWriteTexture(LibGPU.queue, destination, pixelData, mipLevelWidth * mipLevelHeight * 4, source, ext);
            }

            mipLevelWidth /= 2;
            mipLevelHeight /= 2;
            prevPixels = pixels;
        }
    }

    /** Load image data into a specific layer and mip level
     *
     * @param info
     * @param layer
     * @param mipLevel
     */
    private void loadMipLevel(PixmapInfo info, int layer, int mipLevel) {
        loadMipLevel(info.pixels.get(), info.width.intValue(), info.height.intValue(), layer, mipLevel);
    }

    private void loadMipLevel(Pointer data, int width, int height, int layer, int mipLevel) {

        // Arguments telling which part of the texture we upload to
        // (together with the last argument of writeTexture)
        WGPUImageCopyTexture destination = WGPUImageCopyTexture.createDirect();
        destination.setTexture(texture);
        destination.setMipLevel(mipLevel);
        destination.getOrigin().setX(0);
        destination.getOrigin().setY(0);
        destination.getOrigin().setZ(layer);
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

        LibGPU.webGPU.wgpuQueueWriteTexture(LibGPU.queue, destination, data, 4L * width * height, source, ext);
    }



    // load HDR image (RBGA16Float), no mip mapping, no layers
    private void loadHDR(Pointer pixelPtr) {

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
        source.setBytesPerRow(2*4*width);   // 2 bytes per component
        source.setRowsPerImage(height);


        WGPUExtent3D ext = WGPUExtent3D.createDirect();

        float[] pixels = new float[4 * width * height];

        int offset = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[offset] = pixelPtr.getFloat(offset);  offset++;
                pixels[offset] = pixelPtr.getFloat(offset);  offset++;
                pixels[offset] = pixelPtr.getFloat(offset);  offset++;
                pixels[offset] = pixelPtr.getFloat(offset);  offset++;
            }
        }

        destination.setMipLevel(0);
        destination.getOrigin().setZ(0);

        source.setBytesPerRow(2*4*width);
        source.setRowsPerImage(height);

        ext.setWidth(width);
        ext.setHeight(height);
        ext.setDepthOrArrayLayers(1);

        // wrap byte array in native pointer
        Pointer pixelData = JavaWebGPU.createFloatArrayPointer(pixels);
        // N.B. using textureDesc.getSize() for param won't work!
        LibGPU.webGPU.wgpuQueueWriteTexture(LibGPU.queue, destination, pixelData, width * height * 8, source, ext);

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
            JavaWebGPU.getUtils().gdx2d_free(image);
            image = null;
        }
        if(texture != null) {   // guard against double dispose
            System.out.println("Destroy texture " + label);
            LibGPU.webGPU.wgpuSamplerRelease(sampler);
            LibGPU.webGPU.wgpuTextureViewRelease(textureView);
            LibGPU.webGPU.wgpuTextureDestroy(texture);
            LibGPU.webGPU.wgpuTextureRelease(texture);
            texture = null;
        }
    }
}
