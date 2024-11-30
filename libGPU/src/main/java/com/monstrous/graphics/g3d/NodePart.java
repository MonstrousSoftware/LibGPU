package com.monstrous.graphics.g3d;

import com.monstrous.graphics.Material;

public class NodePart {

    public MeshPart meshPart;
    public Material material;

    public NodePart(MeshPart meshPart, Material material) {
        this.meshPart = meshPart;
        this.material = material;
    }
}
