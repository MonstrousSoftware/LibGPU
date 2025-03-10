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

    protected void fitWidth(){};

    protected void fitHeight(){};

    protected void growAndShrinkWidth(){};

    protected void growAndShrinkHeight(){};

    protected void place(){}

    protected void fixScreenPosition(Widget parent) {
        // position is relative to parent's position
        // determine absolute position (parent absolute position was already done before)
        absolute.set(position);
        if(parent != null){
            absolute.add(parent.absolute);
        }
    }


    void draw(RoundedRectangleBatch rrBatch) {};

    void draw(SpriteBatch batch) {};


    public void setPosition(float x, float y){
        position.set(x,y);
    }


    /** use pixel size for a fixed size,
     *  use FIT for a container to fit its content
     *  use GROW for a widget to grow to the available space
     */
    public void setSize(float width, float height){
        if(width == GROW){
            canGrow.setX(true);
            canShrink.setX(true);       // do we need both?
            width = 0;
        }
        if(height == GROW){
            canGrow.setY(true);
            canShrink.setY(true);
            height = 0;
        }
        size.set(width, height);
        preferredSize.set(width, height);
        minimumSize.set(width, height);
    }

    protected void setSizeComponent(int axis, float value){
        canGrow.set(axis, value == GROW);
        canShrink.set(axis, value == GROW);      // ?
        size.setComponent(axis, value);
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

    protected void debugDraw(ShapeRenderer sr){
        sr.setColor(Color.GREEN);
        sr.setLineWidth(1f);
        float xb = absolute.getX();
        float yb = absolute.getY();

        sr.box(xb, yb, xb+size.getX(),  yb+size.getY());
    }
}
