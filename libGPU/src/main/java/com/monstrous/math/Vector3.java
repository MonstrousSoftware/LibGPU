package com.monstrous.math;

public class Vector3 {
    public float x;
    public float y;
    public float z;


    public Vector3(){}

    public Vector3(float x, float y, float z){
        this.set(x, y, z);
    }

    public Vector3(Vector3 v){ this.set(v); }

    public Vector3 set(float x, float y, float z){
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3 set(Vector3 v){
        return set(v.x, v.y, v.z);
    }

    public Vector3 cpy(){
        return new Vector3(this);
    }

    public Vector3 add(Vector3 v){
        return set(this.x + v.x, this.y + v.y, this.z + v.z);
    }

    public Vector3 sub(Vector3 v){
        return set(this.x - v.x, this.y - v.y, this.z - v.z);
    }

    public float len2(){
        return x*x + y*y + z*z;
    }

    public float len(){
        return (float)Math.sqrt( x*x + y*y + z*z);
    }

    static public float len(float x, float y, float z){
        return (float)Math.sqrt( x*x + y*y + z*z);
    }

    public Vector3 scl (float scalar) {
        return this.set(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    public Vector3 crs (float x, float y, float z) {
        return this.set(this.y * z - this.z * y, this.z * x - this.x * z, this.x * y - this.y * x);
    }

    public Vector3 crs (Vector3 v) {
        return this.set(this.y * v.z - this.z * v.y, this.z * v.x - this.x * v.z, this.x * v.y - this.y * v.x);
    }



    public Vector3 nor () {
        final float len2 = this.len2();
        if (len2 == 0f || len2 == 1f) return this;
        return this.scl(1f / (float)Math.sqrt(len2));
    }

    public static float dot (float x1, float y1, float z1, float x2, float y2, float z2) {
        return x1 * x2 + y1 * y2 + z1 * z2;
    }
}
