package com.monstrous;

import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.Texture;
import com.monstrous.utils.ScreenUtils;

// This demonstrates that sprite batch can be set enable/disable blending mode.
// From left to right: default mode (blended), blending disabled, blending enabled
// Also demonstrates setColor: the top row has a red tint.



public class TestSpriteBatchAlpha extends ApplicationAdapter {

    private SpriteBatch batch;
    private Texture textureSmile;

    public void create() {
        textureSmile = new Texture("textures/smile.png", false);

        batch = new SpriteBatch();
    }

    public void render(  ){
        ScreenUtils.clear(0.5f, 0.5f, 0.5f, 1);

        batch.begin();

        batch.draw(textureSmile, 200, 200);
        batch.draw(textureSmile, 220, 220);

        batch.disableBlending();

        batch.draw(textureSmile, 300, 200);
        batch.draw(textureSmile, 320, 220);

        batch.enableBlending();

        batch.draw(textureSmile, 400, 200);
        batch.draw(textureSmile, 420, 220);

        batch.setColor(1,0,0,1);    // red

        batch.draw(textureSmile, 200, 300);
        batch.draw(textureSmile, 220, 320);

        batch.disableBlending();

        batch.draw(textureSmile, 300, 300);
        batch.draw(textureSmile, 320, 320);

        batch.enableBlending();

        batch.draw(textureSmile, 400, 300);
        batch.draw(textureSmile, 420, 320);

        batch.end();
    }

    public void dispose(){
        textureSmile.dispose();
        batch.dispose();
    }
}
