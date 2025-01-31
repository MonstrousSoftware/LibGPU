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

        addListener(new EventListener() {
            @Override
            public boolean handle(int event) {
                if(event == Event.CLICKED)
                    setColor(Color.RED);
                else if (event == Event.MOUSE_ENTERS)
                    setColor(Color.YELLOW);
                else if (event == Event.MOUSE_EXITS)
                    setColor(Color.WHITE);
                return false;
            }
        });
    }

    public Block setColor( Color color ){
        this.color.set(color);
        return this;
    }

    @Override
    public void draw(SpriteBatch batch){
        batch.setColor(color);
        batch.draw(texture, x+parentCell.x, y+parentCell.y, w, h);
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
