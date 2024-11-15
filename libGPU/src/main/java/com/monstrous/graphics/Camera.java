package com.monstrous.graphics;

import com.monstrous.LibGPU;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;

public class Camera {
    static final public Vector3 origin = new Vector3(0,0,0);

    public Vector3 position;
    public Vector3 direction;
    public Vector3 up;
    public Matrix4 projectionMatrix;
    public Matrix4 viewMatrix;

    public Camera() {
        projectionMatrix = new Matrix4();
        viewMatrix = new Matrix4();
        position = new Vector3(0f,0f,0f);
        direction = new Vector3(0,0,1);
        up = new Vector3(0, 1, 0);

//        float aspectRatio = (float) LibGPU.graphics.getWidth()/(float)LibGPU.graphics.getHeight();
//        projectionMatrix.setToPerspective(1.5f, 0.01f, 9.0f, aspectRatio);
//
        update();

    }

    public void update(){
        viewMatrix.setToLookAt(position, origin, up);
    }
}
