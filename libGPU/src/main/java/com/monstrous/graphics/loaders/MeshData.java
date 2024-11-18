package com.monstrous.graphics.loaders;

import java.util.ArrayList;

// interim storage for mesh data loaded from file

public class MeshData {
    public int vertSize; // in floats
    public ArrayList<Float> vertFloats = new ArrayList<>();
    public ArrayList<Integer> indexValues = new ArrayList<>();
    public String objectName;
    public MaterialData materialData;
}
