package com.monstrous;

public class Graphics {

    private int width;  // in pixels
    private int height;
    private long startTime;
    private int frames;
    private float deltaTime;
    private int fps;
    public int passNumber;          // use this to keep track of nr of passes per frame, see Application and ModelBatch.

    public Graphics() {
        startTime = System.nanoTime();
        frames = 0;
    }

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

    public int getFramesPerSecond(){ return fps; }

    public void update(float delta) {

        deltaTime = delta;
        if (System.nanoTime() - startTime > 1000000000) {   // one second passed?
            fps = frames;
            frames = 0;
            startTime = System.nanoTime();
        }
        frames++;
    }
}
