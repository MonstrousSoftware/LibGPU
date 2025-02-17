package com.monstrous.utils.viewports;

import com.monstrous.graphics.Camera;
import com.monstrous.graphics.OrthographicCamera;

// StretchViewport - world view is stretched to fit the screen window, aspect ratio is not preserved

public class StretchViewport extends Viewport {

    public StretchViewport(float worldWidth, float worldHeight ) {
        this(worldWidth, worldHeight, new OrthographicCamera());
    }

    public StretchViewport(float worldWidth, float worldHeight, Camera camera ) {
        setWorldSize(worldWidth, worldHeight);
        setCamera( camera );
    }

    @Override
    public void update (int screenWidth, int screenHeight, boolean centerCamera) {
        setScreenBounds(0, 0, screenWidth, screenHeight);

        apply(centerCamera);
    }


}
