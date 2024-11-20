package com.monstrous.graphics;

import com.monstrous.math.Matrix4;
import com.monstrous.math.Quaternion;
import com.monstrous.math.Vector3;

import java.util.ArrayList;

public class Node {
    public Node parent;
    public ArrayList<Node> children;

    public String name;
    public Matrix4 localTransform;
    public Matrix4 globalTransform;
    public Vector3 translation;
    public Vector3 scale;
    public Quaternion rotation;

    public ArrayList<NodePart> nodeParts;

    public Node() {
        parent = null;
        children = new ArrayList<>(2);

        localTransform = new Matrix4();
        globalTransform = new Matrix4();
        translation = new Vector3(0,0,0);
        scale = new Vector3(1,1,1);
        rotation = new Quaternion(0,0,0,1);
        nodeParts = null;
    }

    public void addChild(Node child){
        child.parent = this;
        children.add(child);
    }

    public void updateMatrices(boolean recurse){
        //Quaternion rot = new Quaternion();
        localTransform.set(translation, rotation, scale);

        if(parent != null)
            globalTransform.set(parent.globalTransform).mul(localTransform);
        else
            globalTransform.set(localTransform);
        if(recurse){
            for(Node child : children)
                child.updateMatrices(true);
        }
    }

    public void getRenderables( ArrayList<Renderable> renderables, Matrix4 modelTransform ){
        if(nodeParts != null) {

            for(NodePart nodePart : nodeParts) {
                Renderable renderable = new Renderable(nodePart.meshPart, nodePart.material, modelTransform);       // todo pooling
                // combine globalTransform from node with modelTransform from model instance
                renderable.modelTransform.mul(globalTransform);
                renderables.add(renderable);
            }
        }
        for(Node child : children)
            child.getRenderables(renderables, modelTransform);
    }


}
