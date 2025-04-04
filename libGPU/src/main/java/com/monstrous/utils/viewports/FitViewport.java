package com.monstrous.utils.viewports;

import com.monstrous.graphics.Camera;
import com.monstrous.graphics.OrthographicCamera;

// FitViewport - world view is scaled to fully fit on screen, adding letterboxing as needed

public class FitViewport extends Viewport {

    public FitViewport( float worldWidth, float worldHeight ) {
        this(worldWidth, worldHeight, new OrthographicCamera());
    }

    public FitViewport( float worldWidth, float worldHeight, Camera camera ) {
        setWorldSize(worldWidth, worldHeight);
        setCamera( camera );
    }

    @Override
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
