package com.monstrous.math;

public class Vector2 {
    public float x;
    public float y;

    public Vector2(){}

    public Vector2(float x, float y){
        this.set(x, y);
    }

    public Vector2( Vector2 v2 ){
        this.set(v2.x, v2.y);
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

    public Vector2 scl (float scalar) {
        return this.set(this.x * scalar, this.y * scalar);
    }


    public float len2(){
        return x*x + y*y;
    }

    public float len(){
        return (float)Math.sqrt( x*x + y*y);
    }

    public Vector2 nor () {
        final float len2 = this.len2();
        if (len2 == 0f || len2 == 1f) return this;
        return this.scl(1f / (float)Math.sqrt(len2));
    }

    @Override
    public String toString () {
        return "(" + x + "," + y + ")";
    }

}
