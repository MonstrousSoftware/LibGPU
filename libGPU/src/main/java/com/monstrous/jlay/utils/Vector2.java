package com.monstrous.jlay.utils;

public class Vector2 {
    float x, y;

    public Vector2(){
        this(0f, 0f);
    }

    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void set(float x, float y){
        this.x = x;
        this.y = y;
    }

    public void set(Vector2 pos){
        this.x = pos.x;
        this.y = pos.y;
    }

    public void add(Vector2 pos){
        this.x += pos.x;
        this.y += pos.y;
    }

    public void setX(float x){
        this.x = x;
    }

    public float getX(){
        return x;
    }

    public void setY(float y){
        this.y = y;
    }

    public float getY(){
        return y;
    }

    /** index 0 returns x, index 1 returns y */
    public float getComponent(int index){
        return index == 0 ? x : y;
    }

    public void setComponent(int index, float value){
        if(index == 0)
            this.x = value;
        else
            this.y = value;
    }

}
