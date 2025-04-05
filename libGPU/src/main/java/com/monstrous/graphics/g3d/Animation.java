package com.monstrous.graphics.g3d;

import java.util.ArrayList;

public class Animation {
    public String name;
    public float duration;
    public ArrayList<NodeAnimation> nodeAnimations;

    public Animation() {
        nodeAnimations = new ArrayList<>();
    }

    public void addNodeAnimation(NodeAnimation nodeAnimation){
        nodeAnimations.add(nodeAnimation);
    }
}
