package com.monstrous;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.math.Matrix4;
import com.monstrous.utils.ScreenUtils;
import com.monstrous.utils.viewports.FitViewport;


public class TestViewport extends ApplicationAdapter {


    private SpriteBatch batch;
    private Texture background;
    private Texture drop;
    private Texture bucket;
    private FitViewport viewport;
    private long startTime;
    private int frames;
    private Matrix4 projectionMatrix;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        background = new Texture("textures/simplegame/background.png", true);
        bucket = new Texture("textures/simplegame/bucket.png", true);

        batch = new SpriteBatch();
        viewport = new FitViewport(8,5);
        projectionMatrix = new Matrix4();
    }

    public void render(  ){

        viewport.apply();
        //viewport.setScreenBounds(100, 100, 500, 200);

        projectionMatrix.setToOrtho(0f, LibGPU.graphics.getWidth(), 0f, LibGPU.graphics.getHeight(), -1f, 1f);
        batch.setProjectionMatrix(projectionMatrix);

        batch.setProjectionMatrix(viewport.getCamera().combined);

        ScreenUtils.clear(Color.WHITE);
        batch.begin();

        batch.draw(background, 0,0, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.draw(bucket, 0, 0, 1, 1);

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
        background.dispose();
        batch.dispose();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true); // true centers the camera
        //batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        //viewport.setScreenBounds(100, 100, 500, 200);
    }


}
