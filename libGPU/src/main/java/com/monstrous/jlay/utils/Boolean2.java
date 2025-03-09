package com.monstrous.jlay.utils;

public class Boolean2 {
    boolean x, y;

    public Boolean2(boolean x, boolean y) {
        this.x = x;
        this.y = y;
    }

    public void set(boolean x, boolean y){
        this.x = x;
        this.y = y;
    }

    public void setX(boolean x){
        this.x = x;
    }

    public boolean getX(){
        return x;
    }

    public void setY(boolean y){
        this.y = y;
    }

    public boolean getY(){
        return y;
    }

    /** index 0 returns x, index 1 returns y */
    public boolean get(int index){
        return index == 0 ? x : y;
    }

    public void set(int index, boolean value){
        if(index == 0)
            this.x = value;
        else
            this.y = value;
    }

}
