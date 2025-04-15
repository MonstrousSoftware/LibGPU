package com.monstrous.math;

public class Vector4 {
    public float x;
    public float y;
    public float z;
    public float w;

    public Vector4(){}

    public Vector4(float x, float y, float z, float w){
        this.set(x, y, z, w);
    }

    public Vector4(Vector4 v4 ){
        this.set(v4);
    }

    public Vector4 set(float x, float y, float z, float w){
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    public Vector4 set(Vector4 v){
        return set(v.x, v.y, v.z, v.w);
    }

    @Override
    public String toString () {
        return "(" + x + "," + y + ","+ z + "," + w +")";
    }

}
