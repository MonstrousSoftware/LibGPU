package com.monstrous.graphics;

import com.monstrous.math.Matrix4;

public class OrthographicCamera extends Camera {
    public float zoom;

    public OrthographicCamera() {
        this.near = -1f;
        this.far = 1f;
        this.zoom = 1f;
        this.direction.set(0,0,-1);
    }

    public OrthographicCamera( int viewportWidth, int viewportHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.near = -1f;
        this.far = 1f;
        this.zoom = 1f;
    }


    @Override
    public void update() {
        projection.setToOrtho(zoom * -viewportWidth / 2, zoom * (viewportWidth / 2), zoom * -(viewportHeight / 2),
                zoom * viewportHeight / 2, near, far);
        view.setToLookAt(direction, up);
        view.translate(-position.x, -position.y, -position.z);
        combined.set(projection);
        Matrix4.mul(combined.val, view.val);
        inverseProjectionView.set(combined).inv();
        frustum.update(inverseProjectionView);
    };
}
