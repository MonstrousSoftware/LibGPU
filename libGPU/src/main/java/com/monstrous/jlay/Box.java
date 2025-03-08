package com.monstrous.jlay;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.g2d.RoundedRectangleBatch;

public class Box extends Widget {
    public float radius = 16f;

    @Override
    public void draw(RoundedRectangleBatch rrBatch) {
        rrBatch.setColor(0.5f, 0.5f, 0.5f, 0.1f);
        rrBatch.draw(absolute.getX()+5, absolute.getY()-10, width, height, radius);
        rrBatch.setColor(color);
        rrBatch.draw(absolute.getX(), absolute.getY(), width, height, radius);
    }

    public void setCornerRadius(float radius){
        this.radius = radius;
    }

}
