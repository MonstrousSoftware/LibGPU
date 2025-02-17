package com.monstrous.utils.viewports;

import com.monstrous.graphics.Camera;
import com.monstrous.graphics.OrthographicCamera;

// ScreenViewport - world units are mapped 1 to 1 to screen pixels

public class ScreenViewport extends Viewport {

    public ScreenViewport() {
        this( new OrthographicCamera() );
    }

    public ScreenViewport(  Camera camera ) {
        setCamera( camera );
    }

    @Override
    public void update (int screenWidth, int screenHeight, boolean centerCamera) {
        setWorldSize(screenWidth, screenHeight);
        setScreenBounds(0, 0, screenWidth, screenHeight);
        apply(centerCamera);
    }
}
