package com.monstrous.scene2d;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.g2d.ShapeRenderer;
import com.monstrous.graphics.g2d.SpriteBatch;

public class Widget {

    static public Color debugActorColor = new Color(0, 1, 0, 1);

    protected int x, y, w, h;
    protected int padLeft, padRight, padTop, padBottom;
    protected Cell parentCell;
    protected int alignment;



    public void setSize(int w, int h){
        this.w = w;
        this.h = h;
        this.alignment = Align.center;
        this.padLeft = this.padRight = this.padTop = this.padBottom = 0;
    }

    public void setCell( Cell parent ){
        this.parentCell = parent;
    }

    public void pad(int pad){
        pad(pad, pad, pad, pad);
    }

    public void pad(int top, int left, int bottom, int right){
        this.padTop = top;
        this.padLeft = left;
        this.padBottom = bottom;
        this.padRight = right;
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
        x += parentCell.x;

        // y goes up
        if((alignment & Align.bottom) != 0){
            y = padBottom;
        } else if((alignment & Align.top) != 0){
            y = parentCell.h - (h+padTop);
        } else {
            y = (parentCell.h - h) / 2;
        }
        y += parentCell.y;
    }

    public void setAlign(int align){
        this.alignment = align;
    }

    public void pack(){

    }

    public void draw(SpriteBatch batch){

    }

    public void debugDraw(ShapeRenderer sr){
        sr.setColor(debugActorColor);
        sr.setLineWidth(1f);
        sr.box(x, y, x+w, y+h);
    }
}
