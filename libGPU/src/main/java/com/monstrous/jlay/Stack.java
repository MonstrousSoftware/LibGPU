package com.monstrous.jlay;

/**
 * A Stack is a container widget that shows its children overlapped.  The last child added is shown on top.
 */
public class Stack extends Group {

    @Override
    protected void fitWidth(){
        for(Widget child: children) // work bottom up
            child.fitWidth();

        if(fitContent.getX()){
            size.setComponent(crossAxis, measureContentAcrossMainAxis());         // max of children + padding
            minimumSize.setComponent(crossAxis, measureMinimumContentAcrossMainAxis());
        }
    };

    @Override
    protected void fitHeight(){
        for(Widget child: children) // work bottom up
            child.fitHeight();

        if(fitContent.getY()){
            size.setComponent(crossAxis, measureContentAcrossMainAxis());         // max of children + padding
            minimumSize.setComponent(crossAxis, measureMinimumContentAcrossMainAxis());
        }
    };

    @Override
    protected void growAndShrinkWidth(){
        for (Widget child : children) {
            if (child.canGrow.get(0))
                child.setSizeComponent(0, getSize().getComponent(0) - (padStart.getComponent(0) + padEnd.getComponent(0)));
        }
        // top-down traversal
        for(Widget child: children)
            child.growAndShrinkWidth();
    }


    @Override
    protected void growAndShrinkHeight(){
        for (Widget child : children) {
            if (child.canGrow.get(1))
                child.setSizeComponent(1, getSize().getComponent(1) - (padStart.getComponent(1) + padEnd.getComponent(1)));
        }
        // top-down traversal
        for(Widget child: children)
            child.growAndShrinkHeight();
    }


    @Override
    protected void place(){

        for(Widget child: children) {
            float remaining = (size.getComponent(mainAxis) - (padStart.getComponent(mainAxis)+padEnd.getComponent(mainAxis))) - child.getSize().getComponent(mainAxis);
            float x = padStart.getComponent(mainAxis) + remaining/2;    // MIDDLE: centre
            if(alignment.getComponent(mainAxis) < 0)          // START: left or top
                x = padStart.getComponent(mainAxis);
            else if (alignment.getComponent(mainAxis) > 0)   // END: right or bottom
                x = padStart.getComponent(mainAxis) + remaining;

            // alignment on cross axis
            remaining = (getSize().getComponent(crossAxis) - (padStart.getComponent(crossAxis)+padEnd.getComponent(crossAxis))) - child.getSize().getComponent(crossAxis);
            float y = padStart.getComponent(crossAxis) + remaining/2;    // centre height
            if(alignment.getComponent(crossAxis) < 0)
                y = padStart.getComponent(crossAxis) + remaining;
            else if (alignment.getComponent(crossAxis) > 0)
                y = padStart.getComponent(crossAxis);

            child.position.set(x,y);
        }
        // top-down traversal
        for(Widget child: children)
            child.place();
    }
}
