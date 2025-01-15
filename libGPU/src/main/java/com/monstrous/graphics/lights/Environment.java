package com.monstrous.graphics.lights;

import com.monstrous.graphics.Camera;
import com.monstrous.graphics.Texture;

import java.util.ArrayList;

public class Environment {
    public ArrayList<Light> lights;
    public float ambientLightLevel;       // 0 .. 1

    public boolean depthPass = false;
    public boolean renderShadows = false;
    public Texture shadowMap;               // may be null
    public Camera shadowCamera;             // may be null

    public Environment() {
        lights = new ArrayList<>();
    }

    public void add(Light light){
        lights.add(light);
    }

    public void setShadowMap(Camera shadowCamera, Texture map ){
        this.shadowMap = map;
        this.shadowCamera = shadowCamera;
    }
}
