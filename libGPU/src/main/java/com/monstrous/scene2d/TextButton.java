package com.monstrous.scene2d;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.utils.Disposable;

public class TextButton extends Widget implements Disposable {

    private Texture texture;
    private String text;
    private LabelStyle style;
    private Color color;    // TMP
    private int tx, ty;
    private int textAlignment;
    private int textPad;

    public static class LabelStyle {
        public Color fontColor;
        public Color bgColor;
        public BitmapFont font;
    }

    public TextButton(String text, LabelStyle style ) {
        texture = new Texture("textures/white.png", false);
        this.text = text;
        this.style = style;
        this.color = new Color(style.bgColor);
        textAlignment = Align.center;
        textPad = 10;

    }

    public void setText(String text){
        this.text = text;
    }

    public void setColor( Color color ){
        this.color.set(color);
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

    public void draw(SpriteBatch batch, float xoffset, float yoffset){
        batch.setColor(color);
        batch.draw(texture, x+xoffset, y+yoffset, w, h);
        batch.setColor(style.fontColor);
        style.font.draw(batch, text, tx+(int)xoffset, ty+(int)yoffset);
    }

    @Override
    public void onMouseEnters(){
        setColor(Color.BLUE);
    }

    @Override
    public void onMouseExits(){
        setColor(style.bgColor);
    }

    public void onClick(){
        setColor(Color.RED);
    }


    @Override
    public void dispose() {
        texture.dispose();
    }
}
