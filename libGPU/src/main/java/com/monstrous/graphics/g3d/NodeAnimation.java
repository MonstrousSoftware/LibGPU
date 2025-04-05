package com.monstrous.graphics.g3d;

import com.monstrous.math.Quaternion;
import com.monstrous.math.Vector3;

import java.util.ArrayList;

public class NodeAnimation {
    public Node node;
    public ArrayList<NodeKeyframe<Vector3>> translation = null;
    public ArrayList<NodeKeyframe<Quaternion>> rotation = null;
    public ArrayList<NodeKeyframe<Vector3>> scaling = null;

    public void addTranslation(NodeKeyframe<Vector3> keyFrame){
        if(translation == null)
            translation = new ArrayList<>();
        translation.add(keyFrame);
    }

    public void addRotation(NodeKeyframe<Quaternion> keyFrame){
        if(rotation == null)
            rotation = new ArrayList<>();
        rotation.add(keyFrame);
    }

    public void addScaling(NodeKeyframe<Vector3> keyFrame){
        if(scaling == null)
            scaling = new ArrayList<>();
        scaling.add(keyFrame);
    }
}
