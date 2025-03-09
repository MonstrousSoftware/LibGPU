package com.monstrous.jlay;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.g2d.RoundedRectangleBatch;

public class Box extends Widget {
    public float radius = 16f;
    public float dropX = 0;
    public float dropY = 0;

    @Override
    public void draw(RoundedRectangleBatch rrBatch) {
//        rrBatch.setColor(0.5f, 0.5f, 0.5f, 0.1f);
//        rrBatch.draw(absolute.getX()+5, absolute.getY()-10, width, height, radius);
//        rrBatch.setDropShadow(dropX, dropY);
        rrBatch.setColor(color);
        rrBatch.draw(absolute.getX(), absolute.getY()-dropY, size.getX()+dropX, size.getY()+dropY, radius);
    }

    public void setCornerRadius(float radius){
        this.radius = radius;
    }

}
