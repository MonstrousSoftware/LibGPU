package com.monstrous.graphics;

public class Color {
    public float r;
    public float g;
    public float b;
    public float a;

    public Color(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public Color(Color color){
        this(color.r, color.g, color.b, color.a);
    }

    public void set(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }
}
