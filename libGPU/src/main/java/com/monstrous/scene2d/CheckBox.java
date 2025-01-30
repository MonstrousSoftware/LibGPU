package com.monstrous.scene2d;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.utils.Disposable;

public class CheckBox extends Widget implements Disposable {


    private Texture textureTicked;  // should be static & shared (and from an atlas)
    private Texture textureNotTicked;
    private Color color;
    private String text;
    private Style style;
    private int labelX, labelY;         // relative position of label
    private int internalPadding = 10;   // between texture and label
    private Wrapper controlledValue;


    public static class Wrapper{
        public boolean value;
        public Wrapper(boolean value) {
            this.value = value;
        }
    }

    public static class Style {
        public Color fontColor;
        public BitmapFont font;
    }

    public CheckBox( Wrapper controlledValue, String label, Style style) {
        this.controlledValue = controlledValue != null ? controlledValue : new Wrapper(false);

        setText(label);

        textureTicked = new Texture("guiElements/checkbox-ticked.png");
        textureNotTicked = new Texture("guiElements/checkbox-not-ticked.png");

        // and adjust for the label
        labelX = internalPadding + textureTicked.getWidth();
        int lineHeight = style.font.getLineHeight();

        // width is checkbox texture + padding + label length
        // height is maximum of label and texture
        //
        int wt = (int) (labelX + style.font.width(label));
        int ht = Math.max(lineHeight,textureTicked.getHeight());

        labelY = lineHeight + (ht - lineHeight)/2;  // center-align label vertically with texture
        setSize(wt, ht);

        this.color = new Color(Color.WHITE);
        this.style = style;
    }

    public void setColor( Color color ){
        this.color.set(color);
    }

    public void setText(String text){
        this.text = text;
    }


    @Override
    public void draw(SpriteBatch batch, int xoffset, int yoffset){
        batch.setColor(color);
        batch.draw(controlledValue.value ? textureTicked : textureNotTicked, x+xoffset+parentCell.x, y+yoffset+parentCell.y, textureTicked.getWidth(), textureTicked.getHeight());

        batch.setColor(style.fontColor);
        style.font.draw(batch, text, x+labelX +xoffset+ parentCell.x, y+labelY+yoffset+ parentCell.y);
    }

    public void onClick(){
        controlledValue.value = !controlledValue.value;
    }


    @Override
    public void dispose() {
        textureTicked.dispose();
        textureNotTicked.dispose();
    }
}
