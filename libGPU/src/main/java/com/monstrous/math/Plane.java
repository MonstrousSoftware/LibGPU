package com.monstrous.math;

public class Plane {
    public Vector3 normal;
    public float distance;

    public Plane() {
        normal = new Vector3();
    }

    public Plane(Vector3 normal, float distance) {
        this();
        set(normal, distance);
    }

    public void set(Vector3 normal, float distance) {
        this.normal.set(normal).nor();
        this.distance = distance;
    }

    /** Set a plane based on a normal vector and a point on the plane */
    public void set(Vector3 normal, Vector3 point) {
        this.normal.set(normal).nor();
        this.distance = -this.normal.dot(point);
    }

    public float distanceTo(Vector3 point){
        return normal.dot(point) + distance;
    }

    public boolean isInFront( Vector3 point ){
        return distanceTo(point) >= 0;
    }

}
