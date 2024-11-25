package com.monstrous.graphics;

import com.monstrous.math.Matrix4;

import java.util.ArrayList;

public class Renderable {
    public MeshPart meshPart;
    public Material material;
    public Matrix4 modelTransform;
    public ArrayList<Matrix4> instanceTransforms;

    public Renderable(){
    }

    public Renderable(MeshPart meshPart, Material material, Matrix4 modelTransform) {
        this.meshPart = meshPart;
        this.material = material;
        this.modelTransform = new Matrix4(modelTransform);  // need a copy because it may be changed
    }

    public void set(MeshPart meshPart, Material material, Matrix4 modelTransform) {
        this.meshPart = meshPart;
        this.material = material;
        this.modelTransform = new Matrix4(modelTransform);  // need a copy because it may be changed
    }
}
