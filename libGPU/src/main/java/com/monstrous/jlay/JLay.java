package com.monstrous.jlay;

import com.monstrous.graphics.g2d.RoundedRectangleBatch;
import com.monstrous.graphics.g2d.ShapeRenderer;
import com.monstrous.utils.Disposable;

import java.util.ArrayList;

// todo do we need grid / table?
// todo text
// todo more widgets
// todo react to inputs: mouseOver, clicks, etc.
//

// Note: UI coordinates have (0,0) at bottom left (y-up).
// UI elements are positioned from their bottom left corner.


public class JLay implements Disposable {


    private RoundedRectangleBatch rrBatch;
    private ArrayList<Widget> widgets;          // replace with one group?
    private ShapeRenderer sr;
    private float width, height;
    private boolean debug = false;

    public JLay() {
        rrBatch = new RoundedRectangleBatch();
        sr = new ShapeRenderer();
        widgets = new ArrayList<>();
    }

    public void clear() {
        widgets.clear();
    }

    public void add(Widget widget){
        widgets.add(widget);
    }

    public void draw(){

        for(Widget widget : widgets) {
            // top level widgets: GROW means screen size
            if(widget.canGrow.getX()) {
                widget.size.setX(width);
                widget.position.setX(0);
            }
            if(widget.canGrow.getY()) {
                widget.size.setY(height);
                widget.position.setY(0);
            }
        }
        for(Widget widget : widgets) {
            widget.fitSizing();
        }
        for(Widget widget : widgets) {
            widget.growAndShrinkSizing();
        }
        for(Widget widget : widgets) {
            widget.place();
        }
        for(Widget widget : widgets) {
            widget.fixScreenPosition(null);
        }

        rrBatch.begin();
        for(Widget widget : widgets) {
            widget.draw(rrBatch);
        }
        rrBatch.end();

        if(debug) {
            sr.begin();
            for (Widget widget : widgets) {
                widget.debugDraw(sr);
            }
            sr.end();
        }
    }

    public void resize(int width, int height){
        this.width = width;
        this.height = height;
        rrBatch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        sr.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }

    public void setDebug(boolean debug){
        this.debug = debug;
    }

    @Override
    public void dispose() {

    }
}
