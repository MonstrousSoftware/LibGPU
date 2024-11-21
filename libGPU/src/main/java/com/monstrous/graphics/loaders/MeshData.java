package com.monstrous.graphics.loaders;

import com.monstrous.graphics.VertexAttributes;

import java.util.ArrayList;

// interim storage for mesh data loaded from file

public class MeshData {

    public VertexAttributes vertexAttributes;
    public ArrayList<Float> vertFloats = new ArrayList<>();
    public ArrayList<Integer> indexValues = new ArrayList<>();
    public String objectName;
    public int indexSizeInBytes;   // in bytes per index, e.g. 2 for Uint16

}
