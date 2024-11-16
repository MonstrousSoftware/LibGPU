package com.monstrous.graphics;

public class OrthographicCamera extends Camera {
    float zoom;

    public OrthographicCamera() {
        this.near = 0;
    }

    public OrthographicCamera( int viewportWidth, int viewportHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.near = 0;
    }


    @Override
    public void update() {
        projectionMatrix.setToOrtho(zoom * -viewportWidth / 2, zoom * (viewportWidth / 2), zoom * -(viewportHeight / 2),
                zoom * viewportHeight / 2, near, far);
        super.update();
    };
}
