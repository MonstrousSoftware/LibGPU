package com.monstrous.graphics.lights;

import com.monstrous.graphics.Color;
import com.monstrous.math.Vector3;

public class DirectionalLight extends Light {
    public Vector3 direction = new Vector3();

    public DirectionalLight(Color color, Vector3 direction) {
        this.color.set(color);
        this.direction.set(direction).nor();
    }
}
