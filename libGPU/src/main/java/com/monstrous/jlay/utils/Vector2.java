package com.monstrous.jlay.utils;

public class Vector2 {
    float x, y;

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

}
