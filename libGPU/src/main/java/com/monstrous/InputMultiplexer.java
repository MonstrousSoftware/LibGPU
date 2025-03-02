package com.monstrous;

import java.util.ArrayList;

/**
 * InputMultiplexer - allows to combine multiple InputProcessors
 */

public class InputMultiplexer implements InputProcessor {
    private ArrayList<InputProcessor> processors;

    public InputMultiplexer() {
        processors = new ArrayList<>();
    }

    public void addProcessor(InputProcessor processor){
        processors.add(processor);
    }

    @Override
    public boolean keyDown(int keycode) {
        for(InputProcessor processor: processors){
            if( processor.keyDown(keycode) )
                return true;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        for(InputProcessor processor: processors){
            if( processor.keyUp(keycode) )
                return true;
        }
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        for(InputProcessor processor: processors){
            if( processor.keyTyped(character) )
                return true;
        }
        return false;
    }

    @Override
    public boolean mouseMoved(int x, int y) {
        for(InputProcessor processor: processors){
            if( processor.mouseMoved(x,y) )
                return true;
        }
        return false;
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        for(InputProcessor processor: processors){
            if( processor.touchDown(x,y, pointer, button) )
                return true;
        }
        return false;
    }

    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        for(InputProcessor processor: processors){
            if( processor.touchUp(x,y, pointer, button) )
                return true;
        }
        return false;
    }

    @Override
    public boolean touchDragged(int x, int y, int pointer) {
        for(InputProcessor processor: processors){
            if( processor.touchDragged(x,y, pointer) )
                return true;
        }
        return false;
    }

    @Override
    public boolean scrolled(float x, float y) {
        for(InputProcessor processor: processors){
            if( processor.scrolled(x,y) )
                return true;
        }
        return false;
    }
}
