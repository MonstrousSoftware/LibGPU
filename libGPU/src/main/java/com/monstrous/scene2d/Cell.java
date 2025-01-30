package com.monstrous.scene2d;

// cell position is absolute, i.e. screen position
// widget position is relative to its parent cell
//
public class Cell {
    public int x, y;
    public int w, h;
    public int row, col;

    public Cell() {
    }

    public void setSize(int w, int h){
        this.w = w;
        this.h = h;
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
