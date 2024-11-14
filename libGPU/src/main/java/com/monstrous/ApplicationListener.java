package com.monstrous;

public interface ApplicationListener {

    public void create();
    public void render( float deltaTime );
    public void dispose();

    public void resize(int width, int height);
}
