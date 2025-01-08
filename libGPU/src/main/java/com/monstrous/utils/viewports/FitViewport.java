package com.monstrous.utils.viewports;

import com.monstrous.graphics.OrthographicCamera;

public class FitViewport extends Viewport {

    public FitViewport( float worldWidth, float worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.camera = new OrthographicCamera();
    }

    public void update (int screenWidth, int screenHeight, boolean centerCamera) {
        float targetRatio = (float) screenHeight / screenWidth;
        float sourceRatio = worldHeight / worldWidth;
        float scale;
        if(targetRatio > sourceRatio)           // target is too high
            scale = screenWidth/worldWidth;     // set the scale so the width will fit exactly
        else
            scale = screenHeight/worldHeight;

        int viewportWidth = (int) (scale * worldWidth);
        int viewportHeight = (int) (scale * worldHeight);

        // Center.
        setScreenBounds((screenWidth - viewportWidth) / 2, (screenHeight - viewportHeight) / 2, viewportWidth, viewportHeight);

        apply(centerCamera);
    }


}
