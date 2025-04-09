package com.monstrous.graphics;

import com.monstrous.FileHandle;
import com.monstrous.Files;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.webgpu.WGPUTextureFormat;
import com.monstrous.webgpu.WGPUTextureUsage;
import jnr.ffi.Pointer;

public class TextureArray extends Texture {

    protected int numLayers;

    public TextureArray(){}

    public TextureArray(int width, int height, int numLayers ){
        this(width, height, false, numLayers);
    }

    public TextureArray(int width, int height, boolean mipMapping, int numLayers ){
        this.width = width;
        this.height = height;
        this.numLayers = numLayers;
        mipLevelCount = mipMapping ? Math.max(1, bitWidth(Math.max(width, height))) : 1;
        int textureUsage = WGPUTextureUsage.TextureBinding | WGPUTextureUsage.CopyDst;
        create( "texture array", mipLevelCount, textureUsage,  WGPUTextureFormat.RGBA8Unorm, numLayers, 1, null);
    }

    /** create a texture array from a series of image files */
    public TextureArray(String[] fileNames, boolean mipMapping) {

        int numLayers = fileNames.length;
        mipLevelCount = mipMapping ? Math.max(1, bitWidth(Math.max(width, height))) : 1;
        format = WGPUTextureFormat.RGBA8Unorm;

        for(int layer = 0; layer < numLayers; layer++) {

            byte[] fileData;

            FileHandle handle = Files.internal(fileNames[layer]);
            fileData = handle.readAllBytes();
            int len = fileData.length;
            Pointer data = JavaWebGPU.createByteArrayPointer(fileData);

            Pointer image = JavaWebGPU.getUtils().gdx2d_load(data, len);        // use native function to parse image file
            PixmapInfo info = PixmapInfo.createAt(image);

            // use the first image to establish the dimensions and create the multi-layer texture
            if(layer == 0) {
                this.width = info.width.intValue();
                this.height = info.height.intValue();
                int textureUsage = WGPUTextureUsage.TextureBinding | WGPUTextureUsage.CopyDst;
                create(fileNames[layer], mipLevelCount, textureUsage, format, numLayers, 1, null);
            } else {
                if(info.width.intValue() != width || info.height.intValue() != height)
                    throw new RuntimeException("Texture: layers must have same size");
            }
            Pointer pixelPtr = info.pixels.get();
            load(pixelPtr, layer);
        }
    }

    /** Create a texture array with LOD levels from image files
     *
     * @param fileNames    Array of files names for different layers. LOD level and extension will be appended.
     * @param extension     File name extension, e.g. ".png"
     * @param lodLevels     Number of LOD levels
     */
    public TextureArray(String[] fileNames, String extension, int lodLevels) {

        int numLayers = fileNames.length;
        format = WGPUTextureFormat.RGBA8Unorm;

        for(int layer = 0; layer < numLayers; layer++) {

            for(int level = 0; level < lodLevels; level++) {
                byte[] fileData;

                String fileName = fileNames[layer] + level + extension;

                FileHandle handle = Files.internal(fileName);
                fileData = handle.readAllBytes();
                Pointer data = JavaWebGPU.createByteArrayPointer(fileData);

                Pointer image = JavaWebGPU.getUtils().gdx2d_load(data, fileData.length);        // use native function to parse image file
                PixmapInfo info = PixmapInfo.createAt(image);

                // use the first image to create the texture
                if (layer == 0 && level == 0) {
                    this.width = info.width.intValue();
                    this.height = info.height.intValue();
                    int textureUsage = WGPUTextureUsage.TextureBinding | WGPUTextureUsage.CopyDst;
                    create(fileName, lodLevels, textureUsage, format, numLayers, 1, null);
                } else if (level == 0){
                    if (info.width.intValue() != width || info.height.intValue() != height)
                        throw new RuntimeException("Texture: layers must have same size");
                }
                loadMipLevel(info, layer, level);
            }
        }
    }


    public int getNumLayers() {
        return numLayers;
    }

    public void setNumLayers(int numLayers) {
        this.numLayers = numLayers;
    }


}
