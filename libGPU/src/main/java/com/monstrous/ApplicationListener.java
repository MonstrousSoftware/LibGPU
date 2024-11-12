package com.monstrous;

public interface ApplicationListener {

    public void init();
    public void render( float deltaTime );
    public void exit();
}
