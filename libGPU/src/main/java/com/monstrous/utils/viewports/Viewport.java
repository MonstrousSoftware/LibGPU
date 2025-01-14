package com.monstrous.utils.viewports;

import com.monstrous.graphics.Camera;
import com.monstrous.graphics.webgpu.RenderPass;

public abstract class Viewport {

    protected Camera camera;
    protected float worldWidth;
    protected float worldHeight;
    protected float screenX, screenY;
    protected float screenWidth, screenHeight;


    public void apply(){
        apply(false);
    }

    public void apply(boolean centerCamera){
        // This is done in two steps because the render pass may not exist yet.
        RenderPass.setViewport(this);

        camera.viewportWidth = worldWidth;
        camera.viewportHeight = worldHeight;
        if (centerCamera) camera.position.set(worldWidth / 2, worldHeight / 2, 0);
        camera.update();
    }

    public void apply(RenderPass pass){
        pass.setViewport(screenX, screenY, screenWidth, screenHeight, 0, 1);
    }

    public void update(float screenWidth, float screenHeight, boolean centerCamera){
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        apply(centerCamera);
    }

    public Camera getCamera () {
        return camera;
    }

    public void setCamera (Camera camera) {
        this.camera = camera;
    }

    public void setScreenBounds (int screenX, int screenY, int screenWidth, int screenHeight) {
        this.screenX = screenX;
        this.screenY = screenY;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }
    public float getWorldWidth() {
        return worldWidth;
    }

    public float getWorldHeight() {
        return worldHeight;
    }


}
