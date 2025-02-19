package com.monstrous.graphics.loaders;


import com.monstrous.graphics.Color;

// Storage of data from MTL file or from material in GLTF file. Not all options are supported.

public class MaterialData {
    public String name;
    public Color ambient;
    public Color diffuse;
    public Color specular;
    public Color emissive;
    public float metallicFactor;
    public float roughnessFactor;
    public float specularExponent;
    public float transparency;
    public Color opticalDensity;
    public int illuminationModel;
//    public String diffuseMapFilePath;
//    public String normalMapFilePath;
//    public String metallicRoughnessMapFilePath;
//    public String emissiveMapFilePath;
//    public String occlusionMapFilePath;

    // image data, e.g. the content of a .png file
    public byte[] diffuseMapData;
    public byte[] normalMapData;
    public byte[] metallicRoughnessMapData;
    public byte[] emissiveMapData;
    public byte[] occlusionMapData;

    public MaterialData() {
        // default -1 means undefined
        metallicFactor = -1;
        roughnessFactor = -1;
    }
}
