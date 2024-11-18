package com.monstrous.graphics.loaders.gltf;

import java.util.ArrayList;

public class GLTFMesh {
    public String name;
    public ArrayList<GLTFPrimitive> primitives;

    public GLTFMesh() {
        primitives = new ArrayList<>();
    }
}
