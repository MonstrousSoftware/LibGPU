package com.monstrous.scene2d;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.utils.Disposable;

public class TextButton extends Button implements Disposable {

    private String text;
    private Style style;
    private int tx, ty;
    private int textAlignment;
    private int textPad;

    public static class Style {
        public Color fontColor;
        public Color bgColor;
        public BitmapFont font;
    }

    public TextButton(String text, Style style ) {
        this.text = text;
        this.style = style;
        setColor( style.bgColor );
        textAlignment = Align.center;
        textPad = 10;
    }

    public void setText(String text){
        this.text = text;
    }

    @Override
    public void setPosition(){
        super.setPosition();
        float textWidth = style.font.width(text);
        if(w < textWidth + 2*textPad)
            w = (int)textWidth + 2*textPad;
        if(h < style.font.getLineHeight()+2*textPad)
            h = style.font.getLineHeight() + 2*textPad;

        ty = y + style.font.getLineHeight() + (h- style.font.getLineHeight())/2;
        tx = x + (int)(w  - textWidth)/2;
    }

    @Override
    public void draw(SpriteBatch batch){
        super.draw(batch);
        batch.setColor(style.fontColor);
        style.font.draw(batch, text, tx+parentCell.x, ty+parentCell.y);
    }

}
