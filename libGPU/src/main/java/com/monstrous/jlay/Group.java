package com.monstrous.jlay;

import com.monstrous.graphics.g2d.RoundedRectangleBatch;
import com.monstrous.graphics.g2d.ShapeRenderer;
import com.monstrous.jlay.utils.Align;

import java.util.ArrayList;

/**
 * Group - widget container (horizontal)
 * todo vertical
 * note: alignment is defined by the container. Perhaps for the vertical it should be set per child?
 */
public class Group extends Box {

    protected ArrayList<Widget> children;
    protected boolean fit;          // fit group size to the content
    protected float padding;
    protected float gap;            // gap between children
    protected int alignment;

    public Group() {
        children = new ArrayList<>();
        fit = false;
        padding = 0;
        gap = 0;
        alignment = Align.CENTER;
    }

    /**
     * Fit container snugly around its content.
     */
    @Override
    public void fitSizing(){
        for(Widget child: children)
            child.fitSizing();
        if(width == Widget.FIT)
            width = calcContentWidth();
        if(height == Widget.FIT)
            height = calcContentHeight();
    };

    /**
     * Grow/shrink children to match container size.
     */
    @Override
    public void growAndShrinkSizing(){
        float w = calcContentWidth();

        float remainder = width - w;

        // grow
        while(remainder > 0) {

            float smallest = Float.MAX_VALUE;
            // count number of children than can grow
            // and find the smallest size
            int numGrow = 0;
            for (Widget child : children) {
                if (child.widthCanGrow) {
                    numGrow++;
                    if (child.width < smallest)
                        smallest = child.width;
                }
            }

            if (numGrow == 0)
                break;

            // allocate remainder between growing children of the smallest size
            float extra = remainder / numGrow;
            for (Widget child : children) {
                if (child.widthCanGrow && child.width == smallest) {
                    child.width += extra;
                    remainder -= extra;
                }
            }
        }

        // shrink
        while(remainder < 0) {

            float largest = 0;
            // count number of children than can grow/shrink
            // and find the largest size
            int numShrink = 0;
            for (Widget child : children) {
                if (child.widthCanGrow) {
                    numShrink++;
                    if (child.width > largest)
                        largest = child.width;
                }
            }

            if (numShrink == 0 || largest == 0)
                break;

            // allocate remainder between growing children of the smallest size
            float extra = remainder / numShrink;    // negative!
            for (Widget child : children) {
                if (child.widthCanGrow && child.width == largest) {
                    child.width += extra;
                    remainder -= extra;
                }
            }
        }

        for(Widget child: children){
            if(child.heightCanGrow)
                child.height = height - 2 * padding;
        }

        for(Widget child: children)
            child.growAndShrinkSizing();
    };

    private float calcContentWidth(){
        float w = 0;
        for (Widget child : children) {
            w += child.width;
        }
        w += gap * (children.size() - 1);
        w += padding + padding;
        return w;
    }

    private float calcContentHeight() {
        float height = 0;
        for (Widget child : children) {
            if (child.height > height)
                height = child.height;
        }
        height += padding + padding;
        return height;
    }

    @Override
    public void place(){
        float remaining = width - calcContentWidth();
        float childX = padding + remaining/2;    // centre
        if((alignment & Align.RIGHT ) !=0)
            childX = padding + remaining;
        else if ((alignment & Align.LEFT ) != 0)
            childX = padding;

        for(Widget child: children) {
            child.position.setX( childX );
            childX += child.width + gap;

            remaining = (height - 2*padding) - child.height;
            float y = padding + remaining/2;    // centre height
            if((alignment & Align.TOP ) != 0)
                y = padding + remaining;
            else if ((alignment & Align.BOTTOM ) != 0)
                y = padding;
            child.position.setY( y );
        }
        for(Widget child: children)
            child.place();
    }


    /**
     * Convert relative positions to (absolute) screen positions.
     */
    @Override
    public void fixScreenPosition(Widget parent){
        super.fixScreenPosition(parent);
        for(Widget child: children)
            child.fixScreenPosition(this);
    }

    @Override
    public void draw(RoundedRectangleBatch rrBatch) {
        super.draw(rrBatch);
        for(Widget child: children)
            child.draw(rrBatch);
    }

    @Override
    public void debugDraw(ShapeRenderer sr) {
        super.debugDraw(sr);
        for(Widget widget : children)
            widget.debugDraw(sr);
    }

    public void clear() {
        children.clear();
    }

    public void add(Widget widget){
        children.add(widget);
    }

//    public void sizeToFit( boolean fit ){
//        this.fit = fit;
//    }

    public void setAlignment( int alignment ){
        this.alignment = alignment;
    }

    public float getPadding() {
        return padding;
    }

    public void setPadding(float padding) {
        this.padding = padding;
    }

    public void setGap(float gap){
        this.gap = gap;
    }
}
