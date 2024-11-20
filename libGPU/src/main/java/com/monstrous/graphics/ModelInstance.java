package com.monstrous.graphics;

import com.monstrous.math.Matrix4;

public class ModelInstance {
    public Model model;
    public Matrix4 modelTransform;

    public ModelInstance(Model model){
        this(model, new Matrix4());
    }


    public ModelInstance(Model model, float x, float y, float z) {
        this.model = model;
        this.modelTransform = new Matrix4().translate(x,y,z);
    }

    public ModelInstance(Model model, Matrix4 transform) {
        this.model = model;
        this.modelTransform = transform;
    }
}
