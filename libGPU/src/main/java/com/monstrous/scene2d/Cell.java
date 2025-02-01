package com.monstrous.scene2d;

// cell position is absolute, i.e. screen position
// widget position is relative to its parent cell
// alignment and padding apply to the widget within this cell
//
public class Cell {
    public int x, y;
    public int w, h;
    public int row, col;
    protected int padLeft, padRight, padTop, padBottom;
    protected int alignment;

    public Cell() {
        this.alignment = Align.center;
    }

    public void setSize(int w, int h){
        this.w = w;
        this.h = h;
    }

    public Cell setAlign(int align){
        this.alignment = align;
        return this;
    }

    public Cell pad(int pad){
        return pad(pad, pad, pad, pad);
    }

    public Cell pad(int top, int left, int bottom, int right) {
        this.padTop = top;
        this.padLeft = left;
        this.padBottom = bottom;
        this.padRight = right;
        return this;
    }


    public void setWidth(int w){
        this.w = w;
    }

    public void setHeight(int h){
        this.h = h;
    }

    public void setPosition(int x, int y){
        this.x = x;
        this.y = y;
    }
}
