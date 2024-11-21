package com.monstrous.graphics.loaders;


import com.monstrous.graphics.Color;

// Storage of data from MTL file. Most is not supported.

public class MaterialData {
    public String name;
    public Color ambient;
    public Color diffuse;
    public Color specular;
    public Color emissive;
    public float specularExponent;
    public float transparency;
    public Color opticalDensity;
    public int illuminationModel;
    public String diffuseMapFilePath;
    public String normalMapFilePath;
    public String emissiveMapFilePath;
    public String occlusionMapFilePath;
}
