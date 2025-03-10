package com.monstrous.jlay;

import com.monstrous.graphics.g2d.RoundedRectangleBatch;
import com.monstrous.graphics.g2d.ShapeRenderer;
import com.monstrous.graphics.g2d.SpriteBatch;
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

        if(getSize().getComponent(mainAxis) == Widget.FIT) {
            size.setComponent(mainAxis, measureContentAlongMainAxis());          // sum of children + padding and gaps
            minimumSize.setComponent(mainAxis, measureMinimumContentAlongMainAxis());
        }
        if(getSize().getComponent(crossAxis) == Widget.FIT) {
            size.setComponent(crossAxis, measureContentAcrossMainAxis());         // max of children + padding
            minimumSize.setComponent(crossAxis, measureMinimumContentAcrossMainAxis());
        }
    };

    /**
     * Grow/shrink children to match container size (for children sized as "GROW")
     */
    @Override
    public void growAndShrinkSizing(){
        float w = measureContentAlongMainAxis();

        float remainder = getSize().getComponent(mainAxis) - w;

        // grow
        while(remainder > 0) {

            float smallest = Float.MAX_VALUE;
            // count number of children than can grow
            // and also find the smallest size
            int numGrow = 0;
            for (Widget child : children) {
                if(child.preferredSize.getComponent(mainAxis)> child.getSize().getComponent(mainAxis))
                    child.canGrow.set(mainAxis, true);
                else
                    child.canGrow.set(mainAxis, false);
                if (child.canGrow.get(mainAxis)) {
                    numGrow++;
                    if (child.getSize().getComponent(mainAxis) < smallest)
                        smallest = child.getSize().getComponent(mainAxis);
                }
            }

            if (numGrow == 0)
                break;

            // allocate remainder between growing children of the smallest size
            float extra = remainder / numGrow;
            for (Widget child : children) {
                if (child.canGrow.get(mainAxis) && child.getSize().getComponent(mainAxis) == smallest) {
                    child.setSizeComponent(mainAxis, extra + child.getSize().getComponent(mainAxis));
                    remainder -= extra;
                }
            }
        }

        // shrink
        while(remainder < 0) {

            float largest = 0;
            // count number of children than can shrink
            // and find the largest size
            int numShrink = 0;
            for (Widget child : children) {
                if(child.size.getComponent(mainAxis) > child.minimumSize.getComponent(mainAxis))
                    child.canShrink.set(mainAxis, true);
                else
                    child.canShrink.set(mainAxis, false);

                if (child.canShrink.get(mainAxis)) {
                    numShrink++;
                    if (child.size.getComponent(mainAxis) > largest)
                        largest = child.size.getComponent(mainAxis);
                }
            }

            if (numShrink == 0 || largest == 0)
                break;

            // allocate negative remainder between shrinking children of the largest size
            float extra = remainder / numShrink;    // negative!
            for (Widget child : children) {
                if (child.canShrink.get(mainAxis) && child.size.getComponent(mainAxis) == largest) {
                    float newSize = child.size.getComponent(mainAxis) + extra;
                    float minimum = child.minimumSize.getComponent(mainAxis);
                    if(newSize <= minimum){ // don't overshoot
                        remainder += (child.size.getComponent(mainAxis) - minimum);
                        newSize = minimum;
                        child.canShrink.set(mainAxis, false);   // reached minimum size, cannot shrink further
                    } else {
                        remainder -= extra;
                    }
                    child.setSizeComponent(mainAxis, newSize);
                }
            }
        }

        // expand children that can grow in the cross axis to the container size minus padding
        for(Widget child: children){
            if(child.canGrow.get(crossAxis))
                child.setSizeComponent(crossAxis, getSize().getComponent(crossAxis) - (padStart.getComponent(crossAxis)+padEnd.getComponent(crossAxis)));
        }

        // top-down traversal
        for(Widget child: children)
            child.growAndShrinkSizing();
    };

    private float measureContentAlongMainAxis(){
        float total = 0;
        for (Widget child : children) {
            total += child.size.getComponent(mainAxis);
        }
        // add padding at start and end plus all the gaps between children
        float spacing = padStart.getComponent(mainAxis) + padEnd.getComponent(mainAxis) + gap * (children.size() - 1);
        total += spacing;
        return total;
    }

    private float measureMinimumContentAlongMainAxis(){
        float minTotal = 0;
        for (Widget child : children) {
            minTotal += child.minimumSize.getComponent(mainAxis);
        }
        // add padding at start and end plus all the gaps between children
        float spacing = padStart.getComponent(mainAxis) + padEnd.getComponent(mainAxis) + gap * (children.size() - 1);
        minTotal += spacing;
        return minTotal;
    }

    private float measureContentAcrossMainAxis() {
        float max = 0;      // largest size of children
        for (Widget child : children) {
            float sz = child.size.getComponent(crossAxis);
            if (sz > max)
                max = sz;
        }
        float spacing = padStart.getComponent(crossAxis)+padEnd.getComponent(crossAxis);    // padding (no gaps between children)
        return max + spacing;
    }

    private float measureMinimumContentAcrossMainAxis() {
        float maxMin = 0;   // largest minimum size of children
        for (Widget child : children) {
            float min = child.minimumSize.getComponent(crossAxis);
            if(min > maxMin)
                maxMin = min;
        }
        float spacing = padStart.getComponent(crossAxis)+padEnd.getComponent(crossAxis);    // padding (no gaps between children)
        return maxMin + spacing;
    }

    /**
     * Place children in their proper position (relative to container) now that
     * child sizes are known. Takes care of alignment constraints.
     */
    @Override
    public void place(){
        float remaining = size.getComponent(mainAxis) - measureContentAlongMainAxis();
        float childX = padStart.getComponent(mainAxis) + remaining/2;    // MIDDLE: centre
        if(alignment.getComponent(mainAxis) < 0)          // START: left or top
            childX = padStart.getComponent(mainAxis);
        else if (alignment.getComponent(mainAxis) > 0)   // END: right or bottom
            childX = padStart.getComponent(mainAxis) + remaining;

        for(Widget child: children) {
            child.position.setComponent( mainAxis, childX );
            childX += child.getSize().getComponent(mainAxis) + gap;

            // alignment on cross axis
            remaining = (getSize().getComponent(crossAxis) - (padStart.getComponent(crossAxis)+padEnd.getComponent(crossAxis))) - child.getSize().getComponent(crossAxis);
            float y = padStart.getComponent(crossAxis) + remaining/2;    // centre height
            if(alignment.getComponent(crossAxis) < 0)
                y = padStart.getComponent(crossAxis) + remaining;
            else if (alignment.getComponent(crossAxis) > 0)
                y = padStart.getComponent(crossAxis);
            child.position.setComponent( crossAxis, y );
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
    public void draw(SpriteBatch batch) {
        for(Widget child: children)
            child.draw(batch);
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
