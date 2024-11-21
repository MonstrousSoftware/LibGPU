package com.monstrous.graphics;

import com.monstrous.math.Matrix4;

import java.util.ArrayList;

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

    public void getRenderables( ArrayList<Renderable> renderables, RenderablePool pool ){
        for(Node rootNode : model.rootNodes)
            rootNode.getRenderables(renderables, modelTransform, pool);
    }
}
