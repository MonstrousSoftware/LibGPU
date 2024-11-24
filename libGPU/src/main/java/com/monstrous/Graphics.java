package com.monstrous;

public class Graphics {

    private int width;  // in pixels
    private int height;
    private float deltaTime;
    
    public void setSize(int w, int h){
        width = w;
        height = h;
    }
    
    public int getWidth(){
        return width;
    }
    
    public int getHeight(){
        return height;
    }

    public float getDeltaTime() {
        return deltaTime;
    }

    public void setDeltaTime(float delta) {
        deltaTime = delta;
    }
}
