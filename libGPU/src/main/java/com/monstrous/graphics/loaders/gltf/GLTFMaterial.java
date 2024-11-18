package com.monstrous.graphics.loaders.gltf;

public class GLTFMaterial {
    public String name;
    public GLTFMaterialPBR pbrMetallicRoughness;
    public String normalTexturePath;
    public String occlusionTexturePath;
    public String emissiveTexturePath;
    public String alphaMode;
    public float alphaCutoff;
    public boolean doubleSide;


    public GLTFMaterial() {
        alphaMode = "OPAQUE";
        alphaCutoff = 0.5f;
        doubleSide = false;
    }
}
