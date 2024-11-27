package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.utils.ScreenUtils;


public class TestBitmapFont extends ApplicationAdapter {

    private SpriteBatch batch;
    private BitmapFont font;
    private long startTime;
    private int frames;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        font = new BitmapFont(); //"fonts/mario.fnt");
        batch = new SpriteBatch();
    }

    public void render( ){

        ScreenUtils.clear(0,0,0,1);

        batch.begin();
        font.draw(batch, "Hello, world!", 100, 100);
        font.draw(batch, "The quick brown fox jumped over the candlestick!", 100, 40);

        int y = 400;
        font.draw(batch, " From fairest creatures we desire increase,", 100, y-=30);
        font.draw(batch, " That thereby beauty’s rose might never die,", 100, y-=30);
        font.draw(batch, " But, as the riper should by time decease,", 100, y-=30);
        font.draw(batch, " His tender heir might bear his memory.", 100, y-=30);

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
