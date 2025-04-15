package com.monstrous.graphics.loaders.gltf;

import java.util.ArrayList;

public class GLTFSkin {
    public String name;
    public int skeleton;
    public int inverseBindMatrices;
    public ArrayList<Integer> joints;

    public GLTFSkin() {
        joints = new ArrayList<>();
    }
}
