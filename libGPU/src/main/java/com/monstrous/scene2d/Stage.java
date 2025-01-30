package com.monstrous.scene2d;

import com.monstrous.InputProcessor;
import com.monstrous.LibGPU;
import com.monstrous.graphics.g2d.ShapeRenderer;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.utils.Disposable;

import java.util.ArrayList;
import java.util.List;

public class Stage implements Disposable, InputProcessor {

    private SpriteBatch batch;
    private List<Widget> widgets;
    private Cell cell;
    private Table table;
    private boolean debug;
    private ShapeRenderer shapeRenderer;

    public Stage() {
        batch = new SpriteBatch();
        widgets = new ArrayList<>();
        debug = false;

        cell = new Cell();
        cell.setPosition(0,0);
        cell.setSize(LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());

        table = new Table();
        table.setCell(cell);
        table.setSize(LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
    }

    public void debug(){
        debug(true);
    }

    public void debug(boolean mode){
        this.debug = mode;
        if(debug)
            shapeRenderer = new ShapeRenderer();
    }

    public void add( Widget widget ){
        table.add(widget);
    }

    public void row(){
        table.row();
    }

    public void draw(){
        table.pack();

        batch.begin();
        table.draw(batch, cell.x, cell.y);
        batch.end();

        if(debug){
            shapeRenderer.begin();
            table.debugDraw(shapeRenderer, cell.x, cell.y);
            shapeRenderer.end();
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        if(shapeRenderer != null)
            shapeRenderer.dispose();
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
    public boolean touchDown(int x, int y, int pointer, int button) {
        System.out.println("mouse click "+x+", "+y);
        y = cell.h - y;

        Widget found = table.hit(x, y, 0, 0);
        if(found != null)
            found.onClick();

        return false;
    }

    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        return true;
    }


    private Widget widgetUnderMouse;

    @Override
    public boolean mouseMoved(int x, int y) {
        y = cell.h - y;

        Widget found = table.hit(x, y, 0, 0);
        if(found != null) {
            found.onMouseEnters();
            widgetUnderMouse = found;
        }
        else if (widgetUnderMouse != null){
            widgetUnderMouse.onMouseExits();
            widgetUnderMouse = null;
        }
        return false;
    }



    @Override
    public boolean scrolled(float x, float y) {
        return false;
    }
}
