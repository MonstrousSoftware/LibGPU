package com.monstrous.scene2d;

import com.monstrous.graphics.g2d.SpriteBatch;

// a Label that dynamically displays a float variable

public class FloatLabel extends Label  {

    private final StringBuffer sb;
    private final WrappedFloat trackedFloat;
    private final String prefix;

    public FloatLabel(WrappedFloat value, String prefix, Label.Style style ) {
        super(value.toString(), style);
        trackedFloat = value;
        this.prefix = prefix;
        sb = new StringBuffer();
    }

    @Override
    public void draw(SpriteBatch batch){
        sb.setLength(0);
        if(prefix != null)
            sb.append(prefix);
        sb.append(trackedFloat.value);
        setText(sb.toString());
        super.draw(batch);
    }
}
