package com.monstrous.utils.viewports;

import com.monstrous.graphics.Camera;
import com.monstrous.graphics.webgpu.RenderPass;
import com.monstrous.graphics.webgpu.RenderPassBuilder;

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
        RenderPassBuilder.setViewport(this);

        camera.viewportWidth = worldWidth;
        camera.viewportHeight = worldHeight;
        if (centerCamera) camera.position.set(worldWidth / 2, worldHeight / 2, 0);
        camera.update();
    }

    public void apply(RenderPass pass){
        // webgpu does not allow out of bound viewports
        // viewport has to be contained in the render target dimensions
        //
        pass.setViewport(screenX, screenY, screenWidth, screenHeight, 0, 1);
    }

    public void update(int screenWidth, int screenHeight, boolean centerCamera){
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

    public void setWorldSize( float worldWidth, float worldHeight){
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
    }

    public float getWorldWidth() {
        return worldWidth;
    }

    public float getWorldHeight() {
        return worldHeight;
    }


}
