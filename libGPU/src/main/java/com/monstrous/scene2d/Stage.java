package com.monstrous.scene2d;

import com.monstrous.InputProcessor;
import com.monstrous.LibGPU;
import com.monstrous.graphics.g2d.ShapeRenderer;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.utils.Disposable;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class Stage implements Disposable, InputProcessor {

    private SpriteBatch batch;
    //private List<Widget> widgets;
    private Cell cell;                      // root cell
    private Table table;                    // root table
    private boolean debug;                  // debug mode
    private ShapeRenderer shapeRenderer;    // for debug lines
    private Widget keyboardFocus;           // widget that gets keyboard input (can be null)
    private Widget widgetUnderMouse;

    public Stage() {
        batch = new SpriteBatch();
        //widgets = new ArrayList<>();
        debug = false;

        cell = new Cell();
        cell.setPosition(0,0);
        cell.setSize(LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());

        table = new Table();
        table.setCell(cell);
        cell.setAlign(Align.center);
        table.setFillParent(true);

        table.setStage(this);
    }



    public Cell add( Widget widget ){
        return table.add(widget);
    }

    public void row(){
        table.row();
    }

    public void draw(){
        table.pack();   // can we avoid doing this for every draw call?
        table.setPosition();

        batch.begin(null);
        table.draw(batch);
        batch.end();

        if(debug){
            shapeRenderer.begin();
            table.debugDraw(shapeRenderer);
            shapeRenderer.end();
        }
    }

    public void debug(){
        debug(true);
    }

    public void debug(boolean mode){
        this.debug = mode;
        if(debug)
            shapeRenderer = new ShapeRenderer();
    }

    @Override
    public void dispose() {
        batch.dispose();
        if(shapeRenderer != null)
            shapeRenderer.dispose();
    }

    public void clear(){
        table.clear();
    }

    public void resize(int width, int height) {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        cell.setSize( width, height);
        //table.setSize( width, height);
        // to do recalculate layouts
    }


    // provide null to remove focus
    public void setKeyboardFocus( Widget widget ){
        keyboardFocus = widget;
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
        if(keyboardFocus != null)
            return keyboardFocus.keyTyped(character);
        return false;
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        //System.out.println("mouse click "+x+", "+y);
        y = cell.h - y;

        Widget found = table.hit(x, y);
        if(found != null) {
            if(button == GLFW_MOUSE_BUTTON_LEFT)
                found.processEvent(Event.CLICKED);
            else if(button == GLFW_MOUSE_BUTTON_RIGHT)
                found.processEvent(Event.CLICKED_RIGHT);
        }

        return false;
    }

    @Override
    public boolean touchDragged(int x, int y, int pointer) {
        //System.out.println("mouse drag "+x+", "+y);
        y = cell.h - y;

        Widget found = table.hit(x, y);

        if(found != null) {
            found.processEvent(Event.MOUSE_ENTERS);
            widgetUnderMouse = found;
            found.onDrag(x, y);
        }
        else if (widgetUnderMouse != null){
            widgetUnderMouse.processEvent(Event.MOUSE_EXITS);
            widgetUnderMouse = null;
        }


        return false;
    }

    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        return true;
    }

    @Override
    public boolean mouseMoved(int x, int y) {
        y = cell.h - y;

        Widget found = table.hit(x, y);
        if (widgetUnderMouse != null && found != widgetUnderMouse ){
            widgetUnderMouse.processEvent(Event.MOUSE_EXITS);
            widgetUnderMouse = null;
        }
        if(found != null) {
            found.processEvent(Event.MOUSE_ENTERS);
            widgetUnderMouse = found;
        }
        return false;
    }

    @Override
    public boolean scrolled(float x, float y) {
        return false;
    }

}
