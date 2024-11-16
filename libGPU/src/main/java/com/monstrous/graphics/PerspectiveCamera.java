package com.monstrous.graphics;

import com.monstrous.LibGPU;

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
        projectionMatrix.setToPerspective(focalLength, near, far, aspectRatio);
        super.update();
    }
}
