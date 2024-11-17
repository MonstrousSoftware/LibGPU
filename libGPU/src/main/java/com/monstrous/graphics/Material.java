package com.monstrous.graphics;

import com.monstrous.graphics.loaders.MaterialData;
import com.monstrous.utils.Disposable;

public class Material implements Disposable {
    public Texture diffuseTexture;
    public Texture normalTexture;

    public Material(MaterialData materialData) {
        String fileName;
        if(materialData == null || materialData.diffuseMapFilePath == null) {
            fileName = "textures\\rgb.png";
        }
        else
            fileName = materialData.diffuseMapFilePath;
        this.diffuseTexture = new Texture(fileName, false);            // todo until mipmapping is fixed

        if(materialData != null && materialData.normalMapFilePath != null) {
            fileName = materialData.normalMapFilePath;
            this.normalTexture = new Texture(fileName, false);            // todo until mipmapping is fixed
        }
    }

    public Material(Texture texture) {
        this.diffuseTexture = texture;
    }

    @Override
    public void dispose() {
        diffuseTexture.dispose();
        if(normalTexture != null)
            normalTexture.dispose();
    }
}
