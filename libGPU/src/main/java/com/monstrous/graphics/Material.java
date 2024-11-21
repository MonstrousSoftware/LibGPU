package com.monstrous.graphics;

import com.monstrous.graphics.loaders.MaterialData;
import com.monstrous.utils.Disposable;

public class Material implements Disposable {
    public Color baseColor;
    public Texture diffuseTexture;
    public Texture normalTexture;

    public Material(MaterialData materialData) {
        baseColor = new Color(materialData.diffuse);
        String fileName;
        if(materialData.diffuseMapFilePath == null)
            fileName = "textures\\white.png";
        else
            fileName = materialData.diffuseMapFilePath;
        this.diffuseTexture = new Texture(fileName, false);            // todo until mipmapping is fixed

        if( materialData.normalMapFilePath != null) {
            fileName = materialData.normalMapFilePath;
            this.normalTexture = new Texture(fileName, false);            // todo until mipmapping is fixed
        }
    }

    public Material(Texture texture) {
        baseColor = new Color(1,1,1,1);
        this.diffuseTexture = texture;
    }

    // for sorting materials, put emphasis on having or not a normal map, because this implies a pipeline switch, not just a material switch
    //
    public int sortCode(){
        return (normalTexture != null ? 10000 : 0) + (hashCode() % 10000);
    }

    public Material(Color baseColor) {
        this.baseColor = new Color(baseColor);
    }

    @Override
    public void dispose() {
        diffuseTexture.dispose();
        if(normalTexture != null)
            normalTexture.dispose();
    }


}
