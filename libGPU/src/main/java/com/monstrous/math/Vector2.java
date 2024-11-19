package com.monstrous.math;

public class Vector2 {
    public float x;
    public float y;

    public Vector2(){}

    public Vector2(float x, float y){
        this.set(x, y);
    }

    public Vector2 set(float x, float y){
        this.x = x;
        this.y = y;
        return this;
    }

    public Vector2 set(Vector2 v){
        return set(v.x, v.y);
    }


    public Vector2 add(Vector2 v){
        set(x+v.x, y+v.y);
        return this;
    }

    public Vector2 sub(Vector2 v){
        set(x-v.x, y-v.y);
        return this;
    }

    @Override
    public String toString () {
        return "(" + x + "," + y + ")";
    }

}
