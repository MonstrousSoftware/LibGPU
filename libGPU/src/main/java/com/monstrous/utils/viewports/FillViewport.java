package com.monstrous.utils.viewports;

import com.monstrous.graphics.Camera;
import com.monstrous.graphics.OrthographicCamera;

// todo FillViewport doesn't work because WebGPU doesn't allow viewport to be outside the render target which is kind of the whole point of FillViewport.



public class FillViewport extends Viewport {

    public FillViewport( float worldWidth, float worldHeight ) {
        this(worldWidth, worldHeight, new OrthographicCamera());
    }

    public FillViewport( float worldWidth, float worldHeight, Camera camera ) {
        setWorldSize(worldWidth, worldHeight);
        setCamera( camera );
    }


    @Override
    public void update (int screenWidth, int screenHeight, boolean centerCamera) {
        float targetRatio = (float) screenHeight / screenWidth;
        float sourceRatio = worldHeight / worldWidth;
        float scale;
        if(targetRatio < sourceRatio)           // target is too wide
            scale = screenWidth/worldWidth;     // set the scale so the width will fit exactly
        else
            scale = screenHeight/worldHeight;

        int viewportWidth = (int) (scale * worldWidth);
        int viewportHeight = (int) (scale * worldHeight);

        int screenX = (screenWidth - viewportWidth) / 2;
        int screenY = (screenHeight - viewportHeight) / 2;

        // prevent a crash, but desired effect is not achieved
        setScreenBounds(Math.max(0,screenX), Math.max(0,screenY), Math.min(viewportWidth, screenWidth), Math.min(viewportHeight, screenHeight));

        apply(centerCamera);
    }


}
