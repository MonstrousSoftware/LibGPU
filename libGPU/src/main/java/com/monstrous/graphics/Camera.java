package com.monstrous.graphics;

import com.monstrous.LibGPU;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;

public class Camera {
    public Vector3 position;
    public Vector3 direction;
    public Vector3 up;
    public int viewportWidth = 1;
    public int viewportHeight = 1;
    public float near;
    public float far;
    public Matrix4 projectionMatrix;
    public Matrix4 viewMatrix;

    public Camera() {
        projectionMatrix = new Matrix4();
        viewMatrix = new Matrix4();
        position = new Vector3(0f,0f,0f);
        direction = new Vector3(0,0,1);
        up = new Vector3(0, 1, 0);
        near = 0.1f;
        far = 100;

        update();
    }

    // make sure the position & direction are reflected into the view matrix
    public void update(){
        viewMatrix.setToLookAt(direction, up);
        viewMatrix.translate(-position.x, -position.y, -position.z);
    }
}
