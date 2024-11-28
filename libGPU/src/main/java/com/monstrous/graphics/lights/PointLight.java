package com.monstrous.graphics.lights;

import com.monstrous.graphics.Color;
import com.monstrous.math.Vector3;

public class PointLight extends Light {

    public Vector3 position = new Vector3();
    public float intensity;

    public PointLight(Color color, Vector3 position, float intensity) {
        this.color.set(color);
        this.position.set(position);
        this.intensity = intensity;
    }

    public Vector3 getPosition() {
        return position;
    }

    public void setPosition(Vector3 position) {
        this.position = position;
    }

    public float getIntensity() {
        return intensity;
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }


}
