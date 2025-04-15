package com.monstrous.graphics;

import com.monstrous.graphics.g3d.MeshPart;
import com.monstrous.graphics.g3d.Model;
import com.monstrous.math.Matrix4;

public class Renderable {
    public MeshPart meshPart;
    public Material material;
    public Matrix4 modelTransform;
    public Model model; // for skinning TMP?

    public Renderable(){
    }

    public Renderable(MeshPart meshPart, Material material, Matrix4 modelTransform, Model model) {
        this.meshPart = meshPart;
        this.material = material;
        this.modelTransform = new Matrix4(modelTransform);  // need a copy because it may be changed
        this.model = model;
    }

    public void set(MeshPart meshPart, Material material, Matrix4 modelTransform, Model model) {
        this.meshPart = meshPart;
        this.material = material;
        this.modelTransform = new Matrix4(modelTransform);  // need a copy because it may be changed
        this.model = model;
    }
}
