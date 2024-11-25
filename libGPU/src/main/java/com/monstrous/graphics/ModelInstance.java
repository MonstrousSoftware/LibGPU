package com.monstrous.graphics;

import com.monstrous.math.Matrix4;

import java.util.ArrayList;

public class ModelInstance {
    public Model model;
    public Matrix4 modelTransform;
    public ArrayList<Matrix4> instanceTransforms;

    public ModelInstance(Model model){
        this(model, new Matrix4());
    }


    public ModelInstance(Model model, float x, float y, float z) {
        this(model,new Matrix4().translate(x,y,z) );
    }

    public ModelInstance(Model model, Matrix4 transform) {
        if(model == null)
            throw new RuntimeException("ModelInstance: model is null");
        this.model = model;
        this.modelTransform = transform;
        this.instanceTransforms = null;
    }

    // to create a ModelInstance with instancing
    public ModelInstance(Model model, ArrayList<Matrix4> instanceTransforms) {
        if(model == null)
            throw new RuntimeException("ModelInstance: model is null");
        this.model = model;
        this.modelTransform = new Matrix4();
        this.instanceTransforms = instanceTransforms;
    }

    public void getRenderables( ArrayList<Renderable> renderables, RenderablePool pool ){
        for(Node rootNode : model.rootNodes)
            rootNode.getRenderables(renderables, modelTransform, instanceTransforms, pool);
    }
}
