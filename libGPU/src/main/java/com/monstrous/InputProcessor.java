package com.monstrous;

public interface InputProcessor {

    boolean keyDown(int keycode);

    boolean keyUp(int keycode);

    boolean mouseMove(float x, float y);

    boolean scrolled(float x, float y);

}
