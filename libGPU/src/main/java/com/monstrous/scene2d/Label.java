package com.monstrous.scene2d;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.g2d.SpriteBatch;

public class Label extends Widget  {

    private String text;
    private final Style style;
    private int ty;

    public static class Style {
        public Color fontColor;
        public BitmapFont font;
    }

    public Label( String text, Style style ) {
        this.style = style;
        setText(text);
    }

    public void setText(String text){
        this.text = text;

        int lineHeight = style.font.getLineHeight();
        int textWidth = (int) style.font.width(text);
        setPreferredSize(textWidth, lineHeight);
    }


    @Override
    public void setPosition(){
        super.setPosition();
        ty = y+ style.font.getLineHeight();
    }

    @Override
    public void draw(SpriteBatch batch){
        batch.setColor(style.fontColor);
        style.font.draw(batch, text, x+ parentCell.x, ty+ parentCell.y);
    }

}
