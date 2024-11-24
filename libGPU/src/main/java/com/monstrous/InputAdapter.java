package com.monstrous;

public class InputAdapter implements InputProcessor {

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean mouseMove(float x, float y) {
        return false;
    }

    @Override
    public boolean scrolled(float x, float y) {
        return false;
    }
}
