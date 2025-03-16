package com.monstrous.graphics.g3d;

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
        this.min.set(min);
        this.max.set(max);
    }

    public void clear(){
        min.set(0,0,0);
        max.set(0,0,0);
    }

    /** extend bounding box with vertex, growing it if needed */
    public void ext(Vector3 v){
        min.x = min(min.x, v.x);
        min.y = min(min.y, v.y);
        min.z = min(min.z, v.z);

        max.x = max(max.x, v.x);
        max.y = max(max.y, v.y);
        max.z = max(max.z, v.z);
    }

}
