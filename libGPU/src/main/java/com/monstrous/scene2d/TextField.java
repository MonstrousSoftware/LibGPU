package com.monstrous.scene2d;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.g2d.SpriteBatch;

public class TextField extends Widget {

    private final StringBuffer sb;
    private final Label.Style style;
    private int tx, ty;
    private Texture texture;
    private Color color;
    private boolean isSelected;
    private boolean isFirstCharacter;

    public static class Style {
        public Color fontColor;
        public BitmapFont font;
    }

    public TextField( Label.Style style ) {
        this.style = style;
        texture = new Texture(1,1);
        texture.fill(Color.WHITE);
        this.color = new Color(Color.WHITE);
        sb = new StringBuffer();
        isSelected = false;

        addListener(new EventListener() {
            @Override
            public boolean handle(int event) {
                if(event == Event.CLICKED)
                    onSelect();
                else if(event == Event.CLICKED_RIGHT)               // use RMB to deselect text field, there should be other ways...
                    onDeselect();
                else if (event == Event.MOUSE_ENTERS && !isSelected)
                    setColor(Color.YELLOW);
                if (event == Event.MOUSE_EXITS && !isSelected)
                    setColor(Color.WHITE);
                return false;
            }
        });
    }

    private void onSelect(){
        setColor(Color.GRAY);
        isSelected = true;
        isFirstCharacter = true;
        getStage().setKeyboardFocus(this);
    }

    private void onDeselect(){
        setColor(Color.WHITE);
        isSelected = false;
        getStage().setKeyboardFocus(null);
    }

    static protected final char BACKSPACE = 8;


    // todo this is a very very basic line editor
    // also there is no visible cursor to see what we are doing.


    @Override
    public boolean keyTyped(char character) {
        // wipe old text when first new character is typed
        if(isFirstCharacter){
            sb.setLength(0);
            isFirstCharacter = false;
        }
        //System.out.println("Input character: "+(int)character);
        switch(character) {
            case BACKSPACE:
                if(!sb.isEmpty())
                    sb.deleteCharAt( sb.length()-1 );
                break;

            case '\n':
                onDeselect();
                break;

            case '\t':
                sb.append(' '); // handle tab as space for now
                break;

            default:
                sb.append(character);
                break;
        }
        return true;
    }

    public TextField setColor( Color color ){
        this.color.set(color);
        return this;
    }

    public void setText(String text){
        sb.setLength(0);
        sb.append(text);

//        int lineHeight = style.font.getLineHeight();
//        int textWidth = (int) style.font.width(sb.toString());
//        setSize(textWidth, lineHeight);
    }


    @Override
    public void setPosition(){
        super.setPosition();
        tx = x;
        ty = y+ style.font.getLineHeight();
    }

    @Override
    public void draw(SpriteBatch batch){
        batch.setColor(color);
        batch.draw(texture, x+parentCell.x, y+parentCell.y, w, h);
        batch.setColor(style.fontColor);
        style.font.draw(batch, sb.toString(), tx+ parentCell.x, ty+ parentCell.y);
    }
}
