package com.monstrous;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.utils.ScreenUtils;


public class TestSpriteBatchPerformance extends ApplicationAdapter {

    private SpriteBatch batch;
    private Texture texture;
    private long startTime;
    private int frames;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        texture = new Texture("textures/jackRussel.png", true);

        batch = new SpriteBatch(8000);
    }

    public void render(  ){

        // SpriteBatch testing
        ScreenUtils.clear(Color.WHITE);
        batch.begin();

        int W = LibGPU.graphics.getWidth();
        int H = LibGPU.graphics.getHeight();
        batch.setColor(Color.GREEN);

        float x = 0;
        float y = 0;
        for (int i = 0; i < 8000; i++) {
            x = (x+23)%W;
            y = (y+17)%H;

            batch.draw(texture,  x, y, 32, 32);
        }
        batch.end();


        // At the end of the frame
        if (System.nanoTime() - startTime > 1000000000) {
            System.out.println("SpriteBatch maxSpritesInBatch: " + batch.maxSpritesInBatch + " renderCalls: "+batch.renderCalls  );

            System.out.println("SpriteBatch : fps: " + frames  );
            frames = 0;
            startTime = System.nanoTime();
        }
        frames++;

    }

    public void dispose(){
        // cleanup
        System.out.println("demo exit");
        texture.dispose();
        batch.dispose();
    }

    @Override
    public void resize(int width, int height) {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }


}
