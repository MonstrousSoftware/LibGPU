package com.monstrous.graphics;

public abstract class Light {
    public Color color;

    public Light(){
        this.color = new Color(1,1,1,1);
    }

    public Light(Color color) {
        this.color = color;
    }
}
