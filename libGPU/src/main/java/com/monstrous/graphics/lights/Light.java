package com.monstrous.graphics.lights;

import com.monstrous.graphics.Color;

public abstract class Light {

    public Color color = new Color(Color.WHITE);
    public float intensity = 1f;

    public void setColor(Color color) {
        this.color.set(color);
    }

    public Color getColor() {
        return color;
    }
    public float getIntensity() {
        return intensity;
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

}
