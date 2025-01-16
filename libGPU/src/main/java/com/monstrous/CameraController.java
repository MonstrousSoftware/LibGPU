package com.monstrous;

import com.monstrous.graphics.Camera;

// Simple turn table camera controller

public class CameraController extends InputAdapter {

    private Camera camera;
    public float anglex;    // 0 for camera on Z+ axis, PI/2 for camera on X+ axis
    public float angley;   // 0 for camera at ground level, PI/2 for camera directly above
    private float distance;


    public CameraController(Camera camera) {
        this.camera = camera;

        distance = camera.position.len();
        angley = (float)Math.asin(camera.position.y/distance);
        anglex = (float)Math.atan2(camera.position.x, camera.position.z);
    }

    @Override
    public boolean mouseMoved(int x, int y) {
        // left of screen 2PI, right of screen 0
        anglex = 2f * (float) Math.PI * (1f - (float)x / LibGPU.graphics.getWidth());

        // mouse at top of the screen => angleY := PI/2, at bottom of the screen => angleY := -Y/2
        angley = (float) Math.PI * (0.5f - (float)y / LibGPU.graphics.getHeight());
        update();
        return true;
    }

    public void update(){
        float sinx = (float)Math.sin(anglex);
        float cosx = (float)Math.cos(anglex);
        float siny = (float)Math.sin(angley);
        float cosy = (float)Math.cos(angley);

        // camera direction is in the opposite direction of camera position
        camera.direction.set(-sinx*cosy, -siny, -cosx*cosy);
        camera.position.set(camera.direction).scl(-distance);
        camera.update();
    }

    @Override
    public boolean scrolled(float x, float y) {
        //System.out.println("cam controller: scroll: "+x+", "+y);
        if(y < 0)
            distance *= 1.1f;
        else if (y > 0 && distance > 0.0)
            distance *= 0.9f;
        update();
        return true;
    }
}
