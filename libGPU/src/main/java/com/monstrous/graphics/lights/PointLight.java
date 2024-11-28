package com.monstrous.graphics.lights;

import com.monstrous.graphics.Color;
import com.monstrous.math.Vector3;

public class PointLight extends Light {

    public Vector3 position = new Vector3();


    public PointLight(Color color, Vector3 position) {
        this.color.set(color);
        this.position.set(position);
    }
    public PointLight(Color color, Vector3 position, float intensity) {
        this.color.set(color);
        this.position.set(position);
        setIntensity(intensity);
    }

    public Vector3 getPosition() {
        return position;
    }

    public void setPosition(Vector3 position) {
        this.position = position;
    }




}
