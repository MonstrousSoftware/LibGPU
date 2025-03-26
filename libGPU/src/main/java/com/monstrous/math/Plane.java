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

    /** Set a plane based on 3 points on the plane */
    public void set (Vector3 point1, Vector3 point2, Vector3 point3) {
        this.normal.set(point1).sub(point2).crs(point2.x - point3.x, point2.y - point3.y, point2.z - point3.z).nor();
        this.distance = -point1.dot(normal);
    }

    public float distanceTo(Vector3 point){
        return normal.dot(point) + distance;
    }

    public boolean isInFront( Vector3 point ){
        return distanceTo(point) >= 0;
    }

}
