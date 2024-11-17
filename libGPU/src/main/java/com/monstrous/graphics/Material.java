package com.monstrous.graphics;

import com.monstrous.graphics.loaders.MaterialData;
import com.monstrous.utils.Disposable;

public class Material implements Disposable {
    public Texture texture;

    public Material(MaterialData materialData) {
        String fileName;
        if(materialData == null || materialData.diffuseMap == null) {
            fileName = "textures\\rgb.png";
        }
        else
            fileName = materialData.diffuseMap;
        this.texture = new Texture(fileName, false);            // todo until mipmapping is fixed
    }

    public Material(Texture texture) {
        this.texture = texture;
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
