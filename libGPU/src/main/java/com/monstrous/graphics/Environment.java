package com.monstrous.graphics;

import java.util.ArrayList;

public class Environment {
    public ArrayList<Light> lights;

    public Environment() {
        lights = new ArrayList<>();
    }

    public void add(Light light){
        lights.add(light);
    }
}
