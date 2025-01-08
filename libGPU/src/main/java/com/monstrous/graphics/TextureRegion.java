package com.monstrous.graphics;

public class TextureRegion {
    public Texture texture;
    public int regionWidth, regionHeight;   // pixels
    public float u,v;
    public float u2, v2;


    public TextureRegion(){

    }

    // pixel positions from bottom left, width and height in pixels
    public TextureRegion(Texture texture, int x, int y, int width, int height) {
        this.texture = texture;
        setRegion(x,y,width, height);
    }

    // u, v in 0..1 starting top left
    public TextureRegion(Texture texture, float u, float v, float u2, float v2) {
        this.texture = texture;
        setRegion(u, v, u2, v2);
    }

    // pixel positions from bottom left, width and height in pixels
    public void setRegion(int x, int y, int width, int height) {
        float tw = texture.getWidth();
        float th = texture.getHeight();
        setRegion(x/tw, (y+height)/th, (x+width)/tw, y/th);
//        setRegion(x/tw, (th-y)/th, (x+width)/tw, (th-(y+height))/th);
    }

    // u goes right, v goes down. Origin is at top left.
    public void setRegion(float u, float v, float u2, float v2) {
        this.u = u;
        this.v = v;
        this.u2 = u2;
        this.v2 = v2;
        this.regionWidth = Math.round(texture.getWidth() * (u2-u));
        this.regionHeight = Math.round(texture.getHeight() * (v2-v));
    }

}
