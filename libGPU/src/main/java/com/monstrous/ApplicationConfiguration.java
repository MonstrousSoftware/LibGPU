package com.monstrous;

public class ApplicationConfiguration {
    public int width;
    public int height;
    public String title;

    public ApplicationConfiguration() {
        // set to defaults
        width = 640;
        height = 480;
        title = "Application";
    }

    public void setSize(int w, int h){
        this.width = w;
        this.height = h;
    }
}
