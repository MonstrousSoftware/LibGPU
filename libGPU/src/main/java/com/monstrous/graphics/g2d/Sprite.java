package com.monstrous.graphics.g2d;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.TextureRegion;

public class Sprite extends TextureRegion {
    final static int VERTEX_SIZE = 5;       // x, y, u, v, col
    final static int SPRITE_SIZE = 4*VERTEX_SIZE;

    private float[] vertexData = new float[SPRITE_SIZE];
    private final Color color;
    private float x, y;
    private float width, height;


    public Sprite(Texture texture) {
        this(texture, 0,0, texture.getWidth(), texture.getHeight());
    }

    public Sprite (Texture texture, int srcX, int srcY, int srcWidth, int srcHeight) {
        this.texture = texture;
        color = new Color(Color.WHITE);
        setPosition(0,0);
        setSize(srcWidth, srcHeight);
        setColor(color);
        setRegion(srcX, srcY, srcWidth, srcHeight);
    }

    public void setSize(float width, float height){
        this.width = width;
        this.height = height;

        vertexData[0+2*VERTEX_SIZE] = x+width;
        vertexData[0+3*VERTEX_SIZE] = x+width;
        vertexData[1+VERTEX_SIZE] = y+height;
        vertexData[1+2*VERTEX_SIZE] = y+height;
    }

    public float getWidth(){
        return width;
    }

    public float getHeight(){
        return height;
    }



    public void setPosition(float x, float y){
        this.x = x;
        this.y = y;
        vertexData[0] = x;
        vertexData[0+VERTEX_SIZE] = x;
        vertexData[0+2*VERTEX_SIZE] = x+width;
        vertexData[0+3*VERTEX_SIZE] = x+width;
        vertexData[1] = y;
        vertexData[1+VERTEX_SIZE] = y+height;
        vertexData[1+2*VERTEX_SIZE] = y+height;
        vertexData[1+3*VERTEX_SIZE] = y;
    }

    public void setX(float x){
        this.x = x;
        vertexData[0] = x;
        vertexData[0+VERTEX_SIZE] = x;
        vertexData[0+2*VERTEX_SIZE] = x+width;
        vertexData[0+3*VERTEX_SIZE] = x+width;
    }

    public float getX(){
        return x;
    }

    public void setY(float y){
        this.y = y;
        vertexData[1] = y;
        vertexData[1+VERTEX_SIZE] = y+height;
        vertexData[1+2*VERTEX_SIZE] = y+height;
        vertexData[1+3*VERTEX_SIZE] = y;
    }
    public float getY(){
        return y;
    }

    public void setColor(Color color){
        this.color.set(color);
        for(int v = 0; v < 4; v++){
            vertexData[4+v*VERTEX_SIZE] = color.toFloatBits();
        }
    }

    public void setRegion(int srcX, int srcY, int srcWidth, int srcHeight) {
        super.setRegion(srcX, srcY, srcWidth, srcHeight);
        float u = (float) srcX /texture.getWidth();
        float u2 = (float) (srcX+srcWidth) /texture.getWidth();
        float v = (float) srcY /texture.getHeight();
        float v2 = (float) (srcY+srcHeight) /texture.getHeight();
        vertexData[2] = u;
        vertexData[2+VERTEX_SIZE] = u;
        vertexData[2+2*VERTEX_SIZE] = u2;
        vertexData[2+3*VERTEX_SIZE] = u2;

        vertexData[3] = v2;
        vertexData[3+VERTEX_SIZE] = v;
        vertexData[3+2*VERTEX_SIZE] = v;
        vertexData[3+3*VERTEX_SIZE] = v2;
    }


    public void draw(SpriteBatch batch){
        batch.draw(texture, vertexData);
    }

    public void translateX(float dx){
        x += dx;
        vertexData[0] += dx;
        vertexData[0+VERTEX_SIZE] += dx;
        vertexData[0+2*VERTEX_SIZE] += dx;
        vertexData[0+3*VERTEX_SIZE] += dx;
    }

    public void translateY(float dy){
        y += dy;
        vertexData[1] += dy;
        vertexData[1+VERTEX_SIZE] += dy;
        vertexData[1+2*VERTEX_SIZE] += dy;
        vertexData[1+3*VERTEX_SIZE] += dy;
    }
}
