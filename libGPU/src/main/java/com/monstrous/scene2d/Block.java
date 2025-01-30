package com.monstrous.scene2d;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.utils.Disposable;

public class Block extends Widget implements Disposable {


    private Texture texture;
    private Color color;

    public Block() {
        texture = new Texture(1,1);
        texture.fill(Color.WHITE);
        color = new Color(Color.WHITE);
    }

    public void setColor( Color color ){
        this.color.set(color);
    }

    @Override
    public void draw(SpriteBatch batch, int xoffset, int yoffset){
        batch.setColor(color);
        batch.draw(texture, x+xoffset+parentCell.x, y+yoffset+parentCell.y, w, h);
    }

    @Override
    public void onMouseEnters(){
        setColor(Color.BLUE);
    }

    @Override
    public void onMouseExits(){
        setColor(Color.WHITE);
    }

    public void onClick(){
        setColor(Color.RED);
    }


    @Override
    public void dispose() {
        texture.dispose();
    }
}
