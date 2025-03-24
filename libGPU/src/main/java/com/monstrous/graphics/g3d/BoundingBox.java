package com.monstrous.graphics.g3d;

import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;

import static java.lang.Float.max;
import static java.lang.Float.min;

/** Axis-aligned bounding box */
public class BoundingBox {
    public Vector3 min;
    public Vector3 max;

    public BoundingBox() {
        min = new Vector3();
        max = new Vector3();
    }

    public BoundingBox(Vector3 min, Vector3 max){
        this();
        set(min, max);
    }

    public BoundingBox( BoundingBox bbox ) {
        min = new Vector3();
        max = new Vector3();
        set(bbox);
    }

    public void set(Vector3 min, Vector3 max){
        min.set(min);
        max.set(max);
    }

    public void set(BoundingBox bbox){
        min.set(bbox.min);
        max.set(bbox.max);
    }

    public void clear(){
        min.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        max.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
    }

    /** extend bounding box with vertex, growing it if needed */
    public void ext(Vector3 v){
        ext(v.x, v.y, v.z);
    }

    /** extend bounding box with vertex, growing it if needed */
    public void ext(float x, float y, float z){
        min.x = min(min.x, x);
        min.y = min(min.y, y);
        min.z = min(min.z, z);

        max.x = max(max.x, x);
        max.y = max(max.y, y);
        max.z = max(max.z, z);
    }

    public void transform(Matrix4 transform){
        Vector3 tmpMin = new Vector3(min);
        Vector3 tmpMax = new Vector3(max);

        tmpMin.mul(transform);
        tmpMax.mul(transform);
        // make a new AABB containing all 8 corners of the transformed AABB
        // note that this new AABB may be larger that the original and may not
        // be a snug fit for the original source data.
        clear();
        ext(tmpMin.x, tmpMin.y, tmpMin.z);
        ext(tmpMax.x, tmpMin.y, tmpMin.z);
        ext(tmpMin.x, tmpMax.y, tmpMin.z);
        ext(tmpMax.x, tmpMax.y, tmpMin.z);
        ext(tmpMin.x, tmpMin.y, tmpMax.z);
        ext(tmpMax.x, tmpMin.y, tmpMax.z);
        ext(tmpMin.x, tmpMax.y, tmpMax.z);
        ext(tmpMax.x, tmpMax.y, tmpMax.z);
    }

}
