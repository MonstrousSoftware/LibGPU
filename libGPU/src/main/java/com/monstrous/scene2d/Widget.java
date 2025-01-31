package com.monstrous.scene2d;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.g2d.ShapeRenderer;
import com.monstrous.graphics.g2d.SpriteBatch;

import java.util.ArrayList;

public class Widget {

    static public Color debugActorColor = new Color(0, 1, 0, 1);

    private Stage stage;
    protected int x, y, w, h;   // x, y relative to parentCell
    protected int padLeft, padRight, padTop, padBottom;
    protected Cell parentCell;
    protected int alignment;
    protected ArrayList<EventListener> eventListeners;

    public Widget() {
        eventListeners = new ArrayList<>(1);
        this.alignment = Align.center;
        pad(0);
    }

    public Widget setSize(int w, int h){
        this.w = w;
        this.h = h;
        return this;
    }

    public Widget pad(int pad){
        return pad(pad, pad, pad, pad);
    }

    public Widget pad(int top, int left, int bottom, int right){
        this.padTop = top;
        this.padLeft = left;
        this.padBottom = bottom;
        this.padRight = right;
        return this;
    }

    public void setCell( Cell parent ){
        this.parentCell = parent;
    }


    // position the widget within the enclosing cell taking into account alignment and padding
    // x,y is relative to cell position
    public void setPosition(){

        if((alignment & Align.left) != 0){
            x = padLeft;
        } else if((alignment & Align.right) != 0){
            x = parentCell.w - (w+padRight);
        } else {
            x = (parentCell.w - w) / 2;
        }
        // y goes up
        if((alignment & Align.bottom) != 0){
            y = padBottom;
        } else if((alignment & Align.top) != 0){
            y = parentCell.h - (h+padTop);
        } else {
            y = (parentCell.h - h) / 2;
        }
    }

    public Widget setAlign(int align){
        this.alignment = align;
        return this;
    }

    public void addListener( EventListener listener ){
        eventListeners.add(listener);
    }

    public void removeListener( EventListener listener ){
        eventListeners.remove(listener);
    }

    public void processEvent(int event){
        for(EventListener listener : eventListeners)
            listener.handle(event);
    }

    public void setStage(Stage stage){
        this.stage = stage;
    }

    public Stage getStage(){
        return stage;
    }

    public void pack(){

    }

    public void draw(SpriteBatch batch){

    }

    public boolean keyTyped(char character) {
        return false;
    }


    public void onDrag(int x, int y){

    }

    public Widget hit(float mx, float my){
        if(mx < x+parentCell.x || my < y+parentCell.y || mx > x+parentCell.x+w || my >  y+parentCell.y+h)
            return null;
        return this;
    }

    public void debugDraw(ShapeRenderer sr){
        sr.setColor(debugActorColor);
        sr.setLineWidth(1f);
        float xb = x+parentCell.x;
        float yb = y+parentCell.y;

        sr.box(xb-1, yb-1, xb+w, yb+h);
    }
}
