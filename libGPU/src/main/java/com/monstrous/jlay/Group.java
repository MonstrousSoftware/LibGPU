package com.monstrous.jlay;

import com.monstrous.graphics.g2d.RoundedRectangleBatch;
import com.monstrous.graphics.g2d.ShapeRenderer;
import com.monstrous.jlay.utils.Vector2;

import java.util.ArrayList;

/**
 * Group - widget container
 * * note: alignment is defined by the container. Perhaps to be overruled by a child?
 */
public class Group extends Box {

    protected ArrayList<Widget> children;
    protected Vector2 padStart;     // padding from container edges
    protected Vector2 padEnd;
    protected float gap;            // gap between children
    protected Vector2 alignment;
    protected int mainAxis;         // 0 for horizontal, 1 for vertical
    protected int crossAxis;        // always the opposite of mainAxis

    public Group() {
        children = new ArrayList<>();
        padStart = new Vector2();
        padEnd = new Vector2();
        gap = 0;
        alignment = new Vector2();
        setHorizontal();
    }

    /**
     * Fit container snugly around its content if sizing is defined as "FIT".
     */
    @Override
    public void fitSizing(){
        for(Widget child: children) // work bottom up
            child.fitSizing();

        if(size.get(mainAxis) == Widget.FIT)
            size.set(mainAxis, calcContentAlongMainAxis());          // sum of children + padding and gaps
        if(size.get(crossAxis) == Widget.FIT)
            size.set(crossAxis, calcContentAcrossMainAxis());         // max of children + padding
    };

    /**
     * Grow/shrink children to match container size (for children sized as "GROW")
     */
    @Override
    public void growAndShrinkSizing(){
        float w = calcContentAcrossMainAxis();

        float remainder = size.get(mainAxis) - w;

        // grow
        while(remainder > 0) {

            float smallest = Float.MAX_VALUE;
            // count number of children than can grow
            // and also find the smallest size
            int numGrow = 0;
            for (Widget child : children) {
                if (child.canGrow.get(mainAxis)) {
                    numGrow++;
                    if (child.size.get(mainAxis) < smallest)
                        smallest = child.size.get(mainAxis);
                }
            }

            if (numGrow == 0)
                break;

            // allocate remainder between growing children of the smallest size
            float extra = remainder / numGrow;
            for (Widget child : children) {
                if (child.canGrow.get(mainAxis) && child.size.get(mainAxis) == smallest) {
                    child.size.set(mainAxis, extra + child.size.get(mainAxis));
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
                if (child.canGrow.get(mainAxis)) {
                    numShrink++;
                    if (child.size.get(mainAxis) > largest)
                        largest = child.size.get(mainAxis);
                }
            }

            if (numShrink == 0 || largest == 0)
                break;

            // allocate remainder between growing children of the smallest size
            float extra = remainder / numShrink;    // negative!
            for (Widget child : children) {
                if (child.canGrow.get(mainAxis) && child.size.get(mainAxis) == largest) {
                    child.size.set(mainAxis, extra + child.size.get(mainAxis));
                    remainder -= extra;
                }
            }
        }

        // expand children that can grow in the cross axis to the container size minus padding
        for(Widget child: children){
            if(child.canGrow.get(crossAxis))
                child.size.set(crossAxis, size.get(crossAxis) - (padStart.get(crossAxis)+padEnd.get(crossAxis)));
        }

        // top-down traversal
        for(Widget child: children)
            child.growAndShrinkSizing();
    };

    private float calcContentAlongMainAxis(){
        float total = 0;
        for (Widget child : children) {
            total += child.size.get(mainAxis);
        }
        total += gap * (children.size() - 1);   // number of gaps between children
        total += padStart.get(mainAxis)+padEnd.get(mainAxis);
        return total;
    }

    private float calcContentAcrossMainAxis() {
        float max = 0;
        for (Widget child : children) {
            float sz = child.size.get(crossAxis);
            if (sz > max)
                max = sz;
        }
        return max + padStart.get(crossAxis)+padEnd.get(crossAxis);
    }

    /**
     * Place children in their proper position (relative to container) now that
     * child sizes are known. Takes care of alignment constraints.
     */
    @Override
    public void place(){
        float remaining = size.get(mainAxis) - calcContentAlongMainAxis();
        float childX = padStart.get(mainAxis) + remaining/2;    // MIDDLE: centre
        if(alignment.get(mainAxis) < 0)          // START: left or top
            childX = padStart.get(mainAxis);
        else if (alignment.get(mainAxis) > 0)   // END: right or bottom
            childX = padStart.get(mainAxis) + remaining;

        for(Widget child: children) {
            child.position.set( mainAxis, childX );
            childX += child.size.get(mainAxis) + gap;

            // alignment on cross axis
            remaining = (size.get(crossAxis) - (padStart.get(crossAxis)+padEnd.get(crossAxis))) - child.size.get(crossAxis);
            float y = padStart.get(crossAxis) + remaining/2;    // centre height
            if(alignment.get(crossAxis) < 0)
                y = padStart.get(crossAxis) + remaining;
            else if (alignment.get(crossAxis) > 0)
                y = padStart.get(crossAxis);
            child.position.set( crossAxis, y );
        }
        // top-down traversal
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

    /** Remove all children. */
    public void clear() {
        children.clear();
    }

    /** Add a widget to the container group. */
    public void add(Widget widget){
        children.add(widget);
    }

    /** Set the group to be a vertical group. Children are placed bottom to top. */
    public void setVertical(){
        mainAxis = 1;
        crossAxis = 0;
    }

    /** Set the group to be a horizontal group. Children are placed left to right. */
    public void setHorizontal(){
        mainAxis = 0;
        crossAxis = 1;
    }

    /** set alignment in horizontal and vertical direction. Use Align.START, Align.MIDDLE or Align.END. */
    public void setAlignment( float horizontal, float vertical ){
        alignment.set(horizontal, vertical);
    }

    public void setPadding(float pad) {
        setPadding(pad, pad, pad, pad);
    }

    public void setPadding(float top, float left, float bottom, float right) {
        padStart.setX(left);
        padEnd.setX(right);
        padStart.setY(bottom);
        padEnd.setY(top);
    }

    /** Spacing between children. */
    public void setGap(float gap){
        this.gap = gap;
    }
}
