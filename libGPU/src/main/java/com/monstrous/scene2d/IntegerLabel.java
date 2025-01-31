package com.monstrous.scene2d;

import com.monstrous.graphics.g2d.SpriteBatch;

// a Label that dynamically displays an integer variable

public class IntegerLabel extends Label  {

    private final StringBuffer sb;
    private final WrappedInteger trackedInteger;
    private final String prefix;

    public IntegerLabel(WrappedInteger value, String prefix, Style style ) {
        super(value.toString(), style);
        trackedInteger = value;
        this.prefix = prefix;
        sb = new StringBuffer();
    }

    @Override
    public void draw(SpriteBatch batch){
        sb.setLength(0);
        if(prefix != null)
            sb.append(prefix);
        sb.append(trackedInteger.value);
        setText(sb.toString());
        super.draw(batch);
    }
}
