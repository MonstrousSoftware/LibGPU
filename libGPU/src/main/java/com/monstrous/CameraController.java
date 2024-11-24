package com.monstrous;

import com.monstrous.graphics.Camera;

// Simple turn table camera controller

public class CameraController extends InputAdapter {

    private Camera camera;
    private float anglex, angley;
    private float distance;


    public CameraController(Camera camera) {
        this.camera = camera;
        anglex = 0; angley = 0;
        distance = camera.position.len();
    }

    @Override
    public boolean mouseMove(float x, float y) {
        anglex = -2f * (float) Math.PI * x / LibGPU.graphics.getWidth();
        angley = (float) Math.PI * (0.5f + y / LibGPU.graphics.getHeight());
        update();
        return true;
    }

    private void update(){
        float sinx = (float)Math.sin(anglex);
        float cosx = (float)Math.cos(anglex);
        float siny = (float)Math.sin(angley);
        float cosy = (float)Math.cos(angley);

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
