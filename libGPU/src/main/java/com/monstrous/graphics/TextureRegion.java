package com.monstrous.graphics;

public class TextureRegion {
    private Texture texture;
    private float u,v;
    private float u2, v2;

    public TextureRegion(Texture texture, int x, int y, int width, int height) {
        this.texture = texture;
        setRegion(x,y,width, height);
    }

    public void setRegion(int x, int y, int width, int height) {
        float tw = texture.getWidth();
        float th = texture.getHeight();
        setRegion(x/tw, y/th, (x+width)/tw, (y+height)/th);
    }

    public void setRegion(float u, float v, float u2, float v2) {
        this.u = u;
        this.v = v;
        this.u2 = u2;
        this.v2 = v2;
    }

    public float getU() {
        return u;
    }

    public void setU(float u) {
        this.u = u;
    }

    public float getV() {
        return v;
    }

    public void setV(float v) {
        this.v = v;
    }

    public float getU2() {
        return u2;
    }

    public void setU2(float u2) {
        this.u2 = u2;
    }

    public float getV2() {
        return v2;
    }

    public void setV2(float v2) {
        this.v2 = v2;
    }
}
