package com.monstrous.graphics;

public class Color {
    public static final Color WHITE        = new Color(1.0f,1.0f,1.0f,1f);
    public static final Color BLACK        = new Color(0f,0f,0f,1);
    public static final Color RED          = new Color(1f,0f,0f,1);
    public static final Color GREEN        = new Color(0f,1f,0f,1);
    public static final Color BLUE         = new Color(0f,0f,1f,1);
    public static final Color YELLOW        = new Color(1f,1f,0f,1);
    public static final Color GRAY         = new Color(0.5f,0.5f,0.5f,1);
    public static final Color GREEN_YELLOW   = new Color(173, 255, 47);
    public static final Color TEAL           = new Color(0, 128, 128);
    public static final Color ORANGE         = new Color(255, 165, 0);

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

    public Color(int r, int g, int b) {
        this.r = r/255f;
        this.g = g/255f;
        this.b = b/255f;
        this.a = 1.0f;
    }

    public Color(Color color){
        this(color.r, color.g, color.b, color.a);
    }

    /** use one integer value to set a color, typically provided as a hexadecimal RGB value e.g. 0xFFA0A0 */
    public Color(int hex){
        this((hex >> 16)&0xFF, (hex >> 8)&0xFF, hex&0xFF);
    }

    public void set(Color color) {
        set(color.r, color.g, color.b, color.a);
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

    /** Encodes the ABGR int color as a float. The alpha is compressed to use only even numbers between 0-254 to avoid using bits
     * in the NaN range (see {@link Float#intBitsToFloat(int)} javadocs). Rendering which uses colors encoded as floats should
     * expand the 0-254 back to 0-255, else colors cannot be fully opaque. */
    public float toFloatBits() {
        int packed = ((int)(255 * a) << 24) | ((int)(255 * b) << 16) | ((int)(255 * g) << 8) | ((int)(255 * r));
        return Float.intBitsToFloat(packed & 0xfeffffff);
    }
}
