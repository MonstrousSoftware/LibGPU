package com.monstrous.jlay;

import com.monstrous.Files;
import com.monstrous.InputProcessor;
import com.monstrous.graphics.ShaderProgram;
import com.monstrous.graphics.g2d.RoundedRectangleBatch;
import com.monstrous.graphics.g2d.ShapeRenderer;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.jlay.utils.Event;
import com.monstrous.utils.Disposable;

import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

// todo do we need grid / table?
// todo more widgets
// todo react to inputs: mouseOver, clicks, etc.
// todo animation

// Note: UI coordinates have (0,0) at bottom left (y-up).
// UI elements are positioned from their bottom left corner.


public class JLay implements Disposable , InputProcessor {

    private ArrayList<Widget> widgets;          // replace with one group?
    private float width, height;
    private boolean debug = false;

    private RoundedRectangleBatch rrBatch;
    private SpriteBatch spriteBatch;
    private ShapeRenderer sr;
    private Widget widgetUnderMouse;


    public JLay() {
        rrBatch = new RoundedRectangleBatch();
        spriteBatch = new SpriteBatch(1000, new ShaderProgram(Files.classpath("shaders/sprite-distanceField.wgsl")));
        sr = new ShapeRenderer();
        widgets = new ArrayList<>();
        widgetUnderMouse = null;
    }

    public void clear() {
        widgets.clear();
    }

    public void add(Widget widget){
        widgets.add(widget);
    }

    public void draw(){

        for(Widget widget : widgets) {
            // top level widgets: GROW means screen size
            if(widget.canGrow.getX()) {
                widget.getSize().setX(width);
                widget.position.setX(0);
            }
            if(widget.canGrow.getY()) {
                widget.getSize().setY(height);
                widget.position.setY(0);
            }
        }
        for(Widget widget : widgets) {
            widget.fitWidth();
        }
        for(Widget widget : widgets) {
            widget.growAndShrinkWidth();
        }
        for(Widget widget : widgets) {
            widget.fitHeight();
        }
        for(Widget widget : widgets) {
            widget.growAndShrinkHeight();
        }
        for(Widget widget : widgets) {
            widget.place();
        }
        for(Widget widget : widgets) {
            widget.fixScreenPosition(null);
        }

        rrBatch.begin();
        for(Widget widget : widgets) {
            widget.draw(rrBatch);
        }
        rrBatch.end();

        spriteBatch.begin();
        for(Widget widget : widgets) {
            widget.draw(spriteBatch);
        }
        spriteBatch.end();

        if(debug) {
            sr.begin();
            for (Widget widget : widgets) {
                widget.debugDraw(sr);
            }
            sr.end();
        }
    }

    public void resize(int width, int height){
        this.width = width;
        this.height = height;
        rrBatch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        sr.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }

    public void setDebug(boolean debug){
        this.debug = debug;
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean mouseMoved(int x, int y) {
        y = (int)height - y;

        for(Widget widget : widgets) {
            Widget found = widget.hit(x, y);
            if ( found != widgetUnderMouse) {
                if(widgetUnderMouse != null) {
                    widgetUnderMouse.processEvent(Event.MOUSE_EXITS);
                    widgetUnderMouse = null;
                }
            }
            if (found != null && found != widgetUnderMouse) {

                found.processEvent(Event.MOUSE_ENTERS);
                widgetUnderMouse = found;
            }
        }
        return false;
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        y = (int)height - y;

        for(Widget widget : widgets) {
            Widget found = widget.hit(x, y);
            if (found != null) {
                if (button == GLFW_MOUSE_BUTTON_LEFT)
                    found.processEvent(Event.CLICKED);
                else if (button == GLFW_MOUSE_BUTTON_RIGHT)
                    found.processEvent(Event.CLICKED_RIGHT);
            }
        }

        return false;
    }

    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int x, int y, int pointer) {
        return false;
    }

    @Override
    public boolean scrolled(float x, float y) {
        return false;
    }

    @Override
    public void dispose() {
        rrBatch.dispose();
        spriteBatch.dispose();
        sr.dispose();
    }
}
