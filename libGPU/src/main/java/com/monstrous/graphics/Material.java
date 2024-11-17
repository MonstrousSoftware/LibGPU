package com.monstrous.graphics;

import com.monstrous.graphics.loaders.MaterialData;
import com.monstrous.utils.Disposable;

public class Material implements Disposable {
    public Texture texture;

    public Material(MaterialData materialData) {
        if(materialData.diffuseMap == null)
            throw new RuntimeException("Require diffuseMap in MTL "+materialData.name);
        this.texture = new Texture(materialData.diffuseMap);
    }

    public Material(Texture texture) {
        this.texture = texture;
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
