package com.monstrous.graphics.lights;

import java.util.ArrayList;

public class Environment {
    public ArrayList<Light> lights;
    public float ambientLightLevel;       // 0 .. 1

    public Environment() {
        lights = new ArrayList<>();
    }

    public void add(Light light){
        lights.add(light);
    }
}
