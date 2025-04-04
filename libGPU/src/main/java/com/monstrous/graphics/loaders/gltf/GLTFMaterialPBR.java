package com.monstrous.graphics.loaders.gltf;

import com.monstrous.graphics.Color;

public class GLTFMaterialPBR {
    public Color baseColorFactor;
    public int baseColorTexture;
    public float metallicFactor;
    public float roughnessFactor;
    public int metallicRoughnessTexture;

    public GLTFMaterialPBR() {
        baseColorFactor = new Color(1,1,1,1);
        metallicFactor = -1;
        roughnessFactor = -1;
        baseColorTexture = -1;
        metallicRoughnessTexture = -1;
    }
}
