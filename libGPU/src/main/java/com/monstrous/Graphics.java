package com.monstrous;

public class Graphics {

    private int width;  // in pixels
    private int height;
    
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
}
