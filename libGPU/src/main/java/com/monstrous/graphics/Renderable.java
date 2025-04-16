package com.monstrous.graphics;

import com.monstrous.graphics.g3d.MeshPart;
import com.monstrous.graphics.g3d.Model;
import com.monstrous.graphics.g3d.ModelInstance;
import com.monstrous.math.Matrix4;

public class Renderable {
    public MeshPart meshPart;
    public Material material;
    public Matrix4 modelTransform;
    public ModelInstance modelInstance; // for skinning TMP?

    public Renderable(){
    }

    public Renderable(MeshPart meshPart, Material material, Matrix4 modelTransform, ModelInstance modelInstance) {
        this.meshPart = meshPart;
        this.material = material;
        this.modelTransform = new Matrix4(modelTransform);  // need a copy because it may be changed
        this.modelInstance = modelInstance;
    }

    public void set(MeshPart meshPart, Material material, Matrix4 modelTransform, ModelInstance modelInstance) {
        this.meshPart = meshPart;
        this.material = material;
        this.modelTransform = new Matrix4(modelTransform);  // need a copy because it may be changed
        this.modelInstance = modelInstance;
    }
}
