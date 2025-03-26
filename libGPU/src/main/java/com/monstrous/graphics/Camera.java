package com.monstrous.graphics;

import com.monstrous.math.Frustum;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;

public abstract class Camera {
    public Vector3 position;
    public Vector3 direction;
    public Vector3 up;
    public float viewportWidth = 1;
    public float viewportHeight = 1;
    public float near;
    public float far;
    public Matrix4 projection;
    public Matrix4 view;
    public Matrix4 combined;      // P x V
    protected Matrix4 inverseProjectionView;
    public Frustum frustum;


    public Camera() {
        projection = new Matrix4();
        view = new Matrix4();
        combined = new Matrix4();
        inverseProjectionView = new Matrix4();
        position = new Vector3(0f,0f,0f);
        direction = new Vector3(0,0,1);
        up = new Vector3(0, 1, 0);
        near = 0.001f;
        far = 100;
        frustum = new Frustum();

        //update();
    }

    // make sure the position & direction are reflected into the view matrix
    abstract public void update();

//    public void update(){
//        view.setToLookAt(direction, up);
//        view.translate(-position.x, -position.y, -position.z);
//        combined.set(projection);
//        Matrix4.mul(combined.val, view.val);
//    }
}
