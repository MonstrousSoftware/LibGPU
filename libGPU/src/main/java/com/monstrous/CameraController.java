package com.monstrous;

import com.monstrous.graphics.Camera;
import com.monstrous.math.Vector3;

// Simple turn table camera controller

public class CameraController extends InputAdapter {

    private Camera camera;
    public float anglex;    // 0 for camera on Z+ axis, PI/2 for camera on X+ axis
    public float angley;   // 0 for camera at ground level, PI/2 for camera directly above
    private float distance;
    private Vector3 pivotPoint;
    private Vector3 tmpRight;
    private final Vector3 worldUp;


    public CameraController(Camera camera) {
        this(camera, Vector3.Zero);
    }

    public CameraController(Camera camera, Vector3 pivotPoint) {
        this.camera = camera;

        distance = camera.position.len();
        this.pivotPoint = new Vector3(pivotPoint);
        camera.direction.set(pivotPoint).sub(camera.position).nor();
        angley = (float)Math.asin(-camera.direction.y);
        anglex = (float)Math.atan2(-camera.direction.x, -camera.direction.z);

        tmpRight = new Vector3();
        worldUp = new Vector3(0,1,0);
        update();
    }

    @Override
    public boolean mouseMoved(int x, int y) {
        // left of screen 2PI, right of screen 0
        anglex = 2f * (float) Math.PI * (1f - (float)x / LibGPU.graphics.getWidth());

        // mouse at top of the screen => angleY := PI/2, at bottom of the screen => angleY := -PI/2
        angley = (float) Math.PI * (0.5f - (float)y / LibGPU.graphics.getHeight());
        //update();
        return true;
    }

    /** Set 3d point around which the camera rotates. By default, it is (0,0,0) */
    public void setPivotPoint(Vector3 pivotPoint){
        this.pivotPoint.set(pivotPoint);
    }

    public void update(){
        float sinx = (float)Math.sin(anglex);
        float cosx = (float)Math.cos(anglex);
        float siny = (float)Math.sin(angley);
        float cosy = (float)Math.cos(angley);

        // camera direction is in the opposite direction of camera position
        camera.direction.set(-sinx*cosy, -siny, -cosx*cosy).nor();

        // orthonormalize the camera up vector
        tmpRight.set(camera.direction).crs(worldUp);
        camera.up.set(tmpRight).crs(camera.direction).nor();

        camera.position.set(camera.direction).scl(-distance).add(pivotPoint);
        camera.update();
    }

    @Override
    public boolean scrolled(float x, float y) {
        //System.out.println("cam controller: scroll: "+x+", "+y);
        if(y < 0)
            distance *= 1.1f;
        else if (y > 0 && distance > 0.0)
            distance *= 0.9f;
        return true;
    }
}
