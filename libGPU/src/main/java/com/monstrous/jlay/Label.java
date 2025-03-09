package com.monstrous.jlay;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.g2d.SpriteBatch;

public class Label extends Widget {
    private String text;
    private final Style style;
    private int ty;
    private float textWidth;
    private float lineHeight;
    private Texture texture;

    public static class Style {
        public Color fontColor;
        public BitmapFont font;

        public Style(Color fontColor, BitmapFont font) {
            this.fontColor = fontColor;
            this.font = font;
        }
    }

    public Label( String text ) {
        this(text, new Style(Color.BLACK, new BitmapFont()));
    }
    public Label( String text, Style style ) {
        this.style = style;
        setText(text);

        // debug: add a bg color
        texture = new Texture(1,1);
        if(color == null)
            color = Color.YELLOW;
        texture.fill(color);
    }

    public void setText(String text){
        this.text = text;

        lineHeight = style.font.getLineHeight();
        textWidth =  style.font.width(text);
        size.set(textWidth, lineHeight);
    }

    @Override
    public void draw(SpriteBatch batch){
//        batch.setColor(color);
//        batch.draw(texture, absolute.getX(), absolute.getY(), textWidth, lineHeight);
        batch.setColor(style.fontColor);
        style.font.draw(batch, text, absolute.getX(), absolute.getY()+lineHeight);
    }


}
