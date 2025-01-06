package com.monstrous.scene2d;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.utils.Disposable;

public class Label extends Widget implements Disposable {

    private String text;
    private LabelStyle style;
    private int tx, ty;


    public static class LabelStyle {
        public Color fontColor;
        public BitmapFont font;
    }

    public Label( String text, LabelStyle style ) {
        this.text = text;
        this.style = style;
    }

    public void setText(String text){
        this.text = text;
    }


    @Override
    public void setPosition(){
        super.setPosition();
        tx = x;
        ty = y+ style.font.getLineHeight();
        float textWidth = style.font.width(text);
        if(w < textWidth)
            w = (int)textWidth;
        if(h < style.font.getLineHeight())
            h = style.font.getLineHeight();
    }

    @Override
    public void draw(SpriteBatch batch, int xoffset, int yoffset){

        batch.setColor(style.fontColor);
        style.font.draw(batch, text, tx+xoffset+ parentCell.x, ty+yoffset+ parentCell.y);
    }

    @Override
    public void dispose() {

    }

}
