package com.monstrous.jlay;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.g2d.SpriteBatch;

import java.util.ArrayList;

public class Label extends Widget {
    private String text;
    private ArrayList<String> lines;
    private final Style style;
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
        lines = new ArrayList<>();

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
        float minWidth = determineMinWidth(text, style.font);
        setSize(textWidth, lineHeight);                                 // todo do we set size at this point?
        setMinimumSize(minWidth, lineHeight);
        setPreferredSize(textWidth, lineHeight);
    }

    @Override
    public void draw(SpriteBatch batch){
//        batch.setColor(color);
//        batch.draw(texture, absolute.getX(), absolute.getY(), textWidth, lineHeight);

        batch.setColor(style.fontColor);
        float y = absolute.getY() + size.getY();
        for(String line : lines) {
            style.font.draw(batch, line, absolute.getX(), y);  // note for font.draw y is at the top of the text
            y -= lineHeight;
        }
    }


    @Override
    public void fitWidth(){
        // when we are in the fitting width phase, revert to the ideal width, not the width from last frame (perhaps the container sized up)
        size.set(preferredSize);
        lines.clear();
        lines.add(text);
    }

    @Override
    public void setSizeComponent(int axis, float value){
        if(axis == 1) {
            size.setY(value);
            return;
        }
        float width = value;
        System.out.println("Label set width: "+width+" minimum = "+minimumSize.getX()+" pref: "+preferredSize.getX());
        size.setX(width);
        float spaceWidth = style.font.width(" ");

        lines.clear();
        StringBuilder sb = new StringBuilder();
        sb.setLength(0);

        String[] words = text.split("[ ]");         // todo some caching
        float totalWidth = 0;
        for(String word : words ){
            float wordWidth = style.font.width(word);
            if(!sb.isEmpty())
                totalWidth += spaceWidth;
            totalWidth += wordWidth;
            if(totalWidth > width){ // line too long, force word wrap
                lines.add( sb.toString() );
                sb.setLength(0);
                totalWidth = wordWidth;
            }
            if(!sb.isEmpty())
                sb.append(' ');
            sb.append(word);
        }
        lines.add( sb.toString() );
        size.setY(lines.size() * lineHeight);
    }

    /** minimum width of a text is width of its longest word. */
    private float determineMinWidth(String text, BitmapFont font){
        String[] words = text.split("[ ]");
        float maxWordWidth = 0;
        for(String word : words){
            float wordWidth = font.width(word);
            if(wordWidth > maxWordWidth)
                maxWordWidth = wordWidth;
        }
        return maxWordWidth;
    }


}
