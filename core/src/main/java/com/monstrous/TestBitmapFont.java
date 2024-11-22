package com.monstrous;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.SpriteBatch;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.TextureRegion;
import com.monstrous.utils.ScreenUtils;


public class TestBitmapFont implements ApplicationListener {

    private SpriteBatch batch;
    private BitmapFont font;
    private long startTime;
    private int frames;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        font = new BitmapFont();
        batch = new SpriteBatch();
    }

    public void render( float deltaTime ){

        ScreenUtils.clear(0,0,0,1);

        batch.begin();
        font.draw(batch, "Hello, world!", 100, 100);

        batch.end();


        // At the end of the frame
        if (System.nanoTime() - startTime > 1000000000) {
            System.out.println("SpriteBatch : fps: " + frames  );
            frames = 0;
            startTime = System.nanoTime();
        }
        frames++;

    }

    public void dispose(){
        // cleanup
        System.out.println("demo exit");
        font.dispose();
        batch.dispose();
    }

    @Override
    public void resize(int width, int height) {
        System.out.println("demo got resize");
    }


}
