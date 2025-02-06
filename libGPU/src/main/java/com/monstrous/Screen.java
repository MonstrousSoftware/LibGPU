package com.monstrous;

import com.monstrous.utils.Disposable;

public interface Screen extends Disposable {


    public void show ();
    public void render (float delta);
    public void resize (int width, int height);
    public void pause ();
    public void resume ();
    public void hide ();
    public void dispose ();

}
