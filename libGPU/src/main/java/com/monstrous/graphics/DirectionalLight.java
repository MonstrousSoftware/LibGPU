package com.monstrous.graphics;

import com.monstrous.math.Vector3;

public class DirectionalLight extends Light {
    public Vector3 direction;

    public DirectionalLight(Color color, Vector3 direction) {
        this.color = color;
        this.direction = direction;
    }
}
