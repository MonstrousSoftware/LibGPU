package com.monstrous;

public interface InputProcessor {

    boolean keyDown(int keycode);

    boolean keyUp(int keycode);

    boolean mouseMoved(int x, int y);

    boolean touchDown (int x, int y, int pointer, int button);

    boolean scrolled(float x, float y);

}
