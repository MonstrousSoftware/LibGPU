package com.monstrous.graphics.loaders;

import java.util.ArrayList;

// interim storage for mesh data loaded from file

public class MeshData {
    // x y z nx ny nz r g b u v
    public int vertSize = 11; // in floats
    public ArrayList<Float> vertFloats = new ArrayList<>();
    public ArrayList<Integer> indexValues = new ArrayList<>();
    public String objectName;
    public MaterialData materialData;
}
