package com.monstrous.jlay;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.g2d.RoundedRectangleBatch;
import com.monstrous.graphics.g2d.ShapeRenderer;
import com.monstrous.jlay.utils.Vector2;

public abstract class Widget {
    public static float FIT = -1f;
    public static float GROW = -2f;

    protected Vector2 position;
    protected Vector2 absolute;
    protected float width, height;
    protected Color color;
    protected boolean widthCanGrow;
    protected boolean heightCanGrow;

    public Widget() {
        position = new Vector2();
        absolute = new Vector2();
        widthCanGrow = false;
        heightCanGrow = false;
    }

    public void fitSizing(){};

    public void growAndShrinkSizing(){};

    public void place(){}

    public void fixScreenPosition(Widget parent) {
        // position is relative to parent's position
        // determine absolute position (parent absolute position was already done before)
        absolute.set(position);
        if(parent != null){
            absolute.add(parent.absolute);
        }
    }


    public abstract void draw(RoundedRectangleBatch rrBatch);



    public void setPosition(float x, float y){
        position.set(x,y);
    }


    /** use pixel size for a fixed size,
     *  use FIT for a container to fit its content
     *  use GROW for a widget to grow to the available space
     */
    public void setSize(float width, float height){
        widthCanGrow = (width == GROW);
        heightCanGrow = (height == GROW);

        this.width =  width;
        this.height = height;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void debugDraw(ShapeRenderer sr){
        sr.setColor(Color.GREEN);
        sr.setLineWidth(1f);
        float xb = absolute.getX();
        float yb = absolute.getY();

        sr.box(xb, yb, xb+width, yb+height);
    }
}
