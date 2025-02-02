package com.monstrous.scene2d;

// cell position is absolute, i.e. screen position
// widget position is relative to its parent cell
// alignment and padding apply to the widget within this cell
//
public class Cell {
    public int x, y;
    public int w, h;
    public int row, col;
    public int padLeft, padRight, padTop, padBottom;
    private int alignment;

    public Cell() {
        this.alignment = Align.center;
    }

    // the following methods return this Cell to support chaining of method calls



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

    public Cell setSize(int w, int h){
        this.w = w;
        this.h = h;
        return this;
    }

    public Cell setWidth(int w){
        this.w = w;
        return this;
    }

    public Cell setHeight(int h){
        this.h = h;
        return this;
    }

    public Cell setPosition(int x, int y){
        this.x = x;
        this.y = y;
        return this;
    }

    public void positionWidget(Widget widget){
        if(widget.parentCell != this)
            throw new RuntimeException("Widget does not belong in this Cell.");

        if((alignment & Align.left) != 0){
            widget.x = padLeft;
        } else if((alignment & Align.right) != 0){
            widget.x = w - (widget.w + padRight);
        } else {
            widget.x = (w - widget.w) / 2;
        }
        // y goes up
        if((alignment & Align.bottom) != 0){
            widget.y = padBottom;
        } else if((alignment & Align.top) != 0){
            widget.y = h - (widget.h + padTop);
        } else {
            widget.y = (h - widget.h) / 2;
        }
    }
}
