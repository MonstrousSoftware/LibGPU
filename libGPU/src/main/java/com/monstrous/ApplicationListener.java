package com.monstrous;

public interface ApplicationListener {

    public void init();
    public void render( float deltaTime );
    public void exit();

    public void resize(int width, int height);
}
