package com.monstrous.graphics;

public class Color {
    static final Color WHITE        = new Color(1,1,1,1);
    static final Color BLACK        = new Color(0,0,0,1);
    static final Color RED          = new Color(1,0,0,1);
    static final Color GREEN        = new Color(0,1,0,1);
    static final Color BLUE         = new Color(0,0,1,1);

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

    /** Packs the color components into a 32-bit integer with the format ABGR.
     * @return the packed color as a 32-bit int. */
    public int toIntBits () {
        return ((int)(255 * a) << 24) | ((int)(255 * b) << 16) | ((int)(255 * g) << 8) | ((int)(255 * r));
    }
}
