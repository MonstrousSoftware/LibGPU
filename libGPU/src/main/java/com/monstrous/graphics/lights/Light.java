package com.monstrous.graphics.lights;

import com.monstrous.graphics.Color;

public abstract class Light {

    public Color color = new Color(1,1,1,1);

    public void setColor(Color color) {
        this.color.set(color);
    }

    public Color getColor() {
        return color;
    }

}
