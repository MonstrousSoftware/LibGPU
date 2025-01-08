package com.monstrous.graphics;

import com.monstrous.math.Matrix4;

public class PerspectiveCamera extends Camera {
    /** the field of view of the height, in degrees **/
    public float fieldOfView = 67;
    public float focalLength = 1.5f;

    public PerspectiveCamera(){

    }

    public PerspectiveCamera(float fieldOfView, int viewportWidth, int viewportHeight) {
        this.fieldOfView = fieldOfView;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
    }

    @Override
    public void update(){
        float aspectRatio = (float) viewportWidth/(float) viewportHeight;
        projection.setToPerspective(focalLength, near, far, aspectRatio);
        view.setToLookAt(direction, up);
        view.translate(-position.x, -position.y, -position.z);
        combined.set(projection);
        Matrix4.mul(combined.val, view.val);
    }
}
