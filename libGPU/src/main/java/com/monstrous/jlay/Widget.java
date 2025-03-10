package com.monstrous.jlay;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.g2d.RoundedRectangleBatch;
import com.monstrous.graphics.g2d.ShapeRenderer;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.jlay.utils.Boolean2;
import com.monstrous.jlay.utils.Vector2;


public abstract class Widget {
    public static float FIT = -1f;
    public static float GROW = -2f;

    protected Vector2 position;
    protected Vector2 absolute;
    protected Vector2 size;
    protected Vector2 minimumSize;
    protected Vector2 preferredSize;
    protected Color color;
    protected Boolean2 canGrow;
    protected Boolean2 canShrink;

    public Widget() {
        position = new Vector2();
        absolute = new Vector2();
        size = new Vector2();
        minimumSize = new Vector2(0f, 0f);
        preferredSize = new Vector2();
        canGrow = new Boolean2( false,false);
        canShrink = new Boolean2( false,false);
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


    public void draw(RoundedRectangleBatch rrBatch) {};

    public void draw(SpriteBatch batch) {};


    public void setPosition(float x, float y){
        position.set(x,y);
    }


    /** use pixel size for a fixed size,
     *  use FIT for a container to fit its content
     *  use GROW for a widget to grow to the available space
     */
    public void setSize(float width, float height){
        canGrow.setX(width == GROW);
        canGrow.setY(height == GROW);
        canShrink.setX(width == GROW);      // ?
        canShrink.setY(height == GROW);
        size.set(width, height);
        preferredSize.set(width, height);
        minimumSize.set(width, height);
    }

    public void setSizeComponent(int axis, float value){
        canGrow.set(axis, value == GROW);
        canShrink.set(axis, value == GROW);      // ?
        size.setComponent(axis, value);
        preferredSize.setComponent(axis, value);
        minimumSize.setComponent(axis, value);
    }

    public Vector2 getSize(){
        return size;
    }

    public void setMinimumSize(float width, float height){
        minimumSize.set(width, height);
        canShrink.setX(minimumSize.getX() < size.getX());
        canShrink.setY(minimumSize.getY() < size.getY());
    }

    public void setPreferredSize(float width, float height){
        preferredSize.set(width, height);
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

        sr.box(xb, yb, xb+size.getX(),  yb+size.getY());
    }
}
