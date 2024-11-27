package com.monstrous;

import com.monstrous.graphics.SpriteBatch;
import com.monstrous.graphics.Texture;
import com.monstrous.utils.ScreenUtils;


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

        batch.end();
    }

    public void dispose(){
        // cleanup

        textureSmile.dispose();
        batch.dispose();
    }


}
