package com.monstrous.graphics.g2d;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.TextureRegion;

public class Sprite extends TextureRegion {
    final static int VERTEX_SIZE = 5;       // x, y, col, u, v
    final static int SPRITE_SIZE = 4*VERTEX_SIZE;

    final static int X = 0;
    final static int Y = 1;
    final static int COL = 2;
    final static int U = 3;
    final static int V = 4;

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

        vertexData[X+2*VERTEX_SIZE] = x+width;
        vertexData[X+3*VERTEX_SIZE] = x+width;
        vertexData[Y+VERTEX_SIZE] = y+height;
        vertexData[Y+2*VERTEX_SIZE] = y+height;
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
        vertexData[X] = x;
        vertexData[X+VERTEX_SIZE] = x;
        vertexData[X+2*VERTEX_SIZE] = x+width;
        vertexData[X+3*VERTEX_SIZE] = x+width;
        vertexData[Y] = y;
        vertexData[Y+VERTEX_SIZE] = y+height;
        vertexData[Y+2*VERTEX_SIZE] = y+height;
        vertexData[Y+3*VERTEX_SIZE] = y;
    }

    public void setX(float x){
        this.x = x;
        vertexData[X] = x;
        vertexData[X+VERTEX_SIZE] = x;
        vertexData[X+2*VERTEX_SIZE] = x+width;
        vertexData[X+3*VERTEX_SIZE] = x+width;
    }

    public float getX(){
        return x;
    }

    public void setY(float y){
        this.y = y;
        vertexData[Y] = y;
        vertexData[Y+VERTEX_SIZE] = y+height;
        vertexData[Y+2*VERTEX_SIZE] = y+height;
        vertexData[Y+3*VERTEX_SIZE] = y;
    }
    public float getY(){
        return y;
    }

    public void setColor(Color color){
        this.color.set(color);
        for(int v = 0; v < 4; v++){
            vertexData[COL+v*VERTEX_SIZE] = color.toFloatBits();
        }
    }

    public void setRegion(int srcX, int srcY, int srcWidth, int srcHeight) {
        super.setRegion(srcX, srcY, srcWidth, srcHeight);
        float u = (float) srcX /texture.getWidth();
        float u2 = (float) (srcX+srcWidth) /texture.getWidth();
        float v = (float) srcY /texture.getHeight();
        float v2 = (float) (srcY+srcHeight) /texture.getHeight();
        vertexData[U] = u;
        vertexData[U+VERTEX_SIZE] = u;
        vertexData[U+2*VERTEX_SIZE] = u2;
        vertexData[U+3*VERTEX_SIZE] = u2;

        vertexData[V] = v2;
        vertexData[V+VERTEX_SIZE] = v;
        vertexData[V+2*VERTEX_SIZE] = v;
        vertexData[V+3*VERTEX_SIZE] = v2;
    }


    public void draw(SpriteBatch batch){
        batch.draw(texture, vertexData);
    }

    public void translateX(float dx){
        x += dx;
        vertexData[X] += dx;
        vertexData[X+VERTEX_SIZE] += dx;
        vertexData[X+2*VERTEX_SIZE] += dx;
        vertexData[X+3*VERTEX_SIZE] += dx;
    }

    public void translateY(float dy){
        y += dy;
        vertexData[Y] += dy;
        vertexData[Y+VERTEX_SIZE] += dy;
        vertexData[Y+2*VERTEX_SIZE] += dy;
        vertexData[Y+3*VERTEX_SIZE] += dy;
    }
}
