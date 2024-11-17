package com.monstrous.graphics;

import com.monstrous.math.Matrix4;

public class ModelInstance {
    public Model model;
    public Matrix4 modelTransform;

    public ModelInstance(Model model, Matrix4 transform) {
        this.model = model;
        this.modelTransform = transform;
    }
}
