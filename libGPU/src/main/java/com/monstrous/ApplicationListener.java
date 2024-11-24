package com.monstrous;

public interface ApplicationListener {

    public void create();
    public void render();
    public void dispose();

    public void pause();
    public void resume();

    public void resize(int width, int height);
}
