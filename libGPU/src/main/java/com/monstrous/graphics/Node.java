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
    public Matrix4 worldTransform;
    public Vector3 translation;
    public Vector3 scale;
    public Quaternion rotation;

    NodePart nodePart;

    public Node() {
        parent = null;
        children = new ArrayList<>(2);

        localTransform = new Matrix4();
        worldTransform = new Matrix4();
        translation = new Vector3(0,0,0);
        scale = new Vector3(1,1,1);
        rotation = new Quaternion(0,0,0,1);
        nodePart = null;
    }

    public void addChild(Node child){
        child.parent = this;
        children.add(child);
    }

    public void updateMatrices(boolean recurse){
        localTransform.setToTranslation(translation);
        localTransform.scale(scale);
        // todo rotation

        if(parent != null)
            worldTransform.set(parent.worldTransform).mul(localTransform);
        else
            worldTransform.set(localTransform);
        if(recurse){
            for(Node child : children)
                child.updateMatrices(true);
        }
    }


}
