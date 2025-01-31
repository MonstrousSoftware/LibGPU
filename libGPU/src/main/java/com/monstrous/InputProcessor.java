package com.monstrous;

public interface InputProcessor {

    boolean keyDown(int keycode);

    boolean keyUp(int keycode);

    boolean keyTyped(char character);

    boolean mouseMoved(int x, int y);

    boolean touchDown (int x, int y, int pointer, int button);

    boolean touchUp (int x, int y, int pointer, int button);

    boolean touchDragged(int x, int y, int pointer);

    boolean scrolled(float x, float y);

}
