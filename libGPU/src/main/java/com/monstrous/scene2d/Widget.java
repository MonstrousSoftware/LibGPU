package com.monstrous.scene2d;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.g2d.ShapeRenderer;
import com.monstrous.graphics.g2d.SpriteBatch;

import java.util.ArrayList;

public class Widget {

    static public Color debugActorColor = new Color(0, 1, 0, 1);

    private Stage stage;
    protected int x, y;   // x, y relative to parentCell
    protected int w, h;     // actual width, height
    protected boolean fillParent;
    protected int preferredWidth, preferredHeight;
    protected int minWidth, minHeight;
    protected int maxWidth, maxHeight;
    protected Cell parentCell;

    protected ArrayList<EventListener> eventListeners;

    public Widget() {
        eventListeners = new ArrayList<>(1);
        fillParent = false;
    }

    public Widget setPreferredSize(int w, int h){
        this.preferredWidth = w;
        this.preferredHeight = h;
        this.w = w;
        this.h = h;
        return this;
    }

    public Widget setMinSize(int w, int h){
        this.minWidth = w;
        this.minHeight = h;
        return this;
    }

    public Widget setMaxSize(int w, int h){
        this.maxWidth = w;
        this.maxHeight = h;
        return this;
    }

    public Widget setFillParent(boolean fill){
        this.fillParent = fill;
        return this;
    }


    public void setCell( Cell parent ){
        this.parentCell = parent;
    }


    // position the widget within the enclosing cell taking into account alignment and padding
    // x,y is relative to cell position
    public void setPosition(){
        parentCell.positionWidget(this);
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
