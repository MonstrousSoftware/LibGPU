package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.math.Matrix4;
import com.monstrous.wgpu.WGPU;


public class TestSpriteBatch implements ApplicationListener {


    private SpriteBatch batch;
    private Texture texture;
    private Texture texture2;
    private long startTime;
    private int frames;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        texture = new Texture("textures/monstrous.png", false);
        texture2 = new Texture("textures/jackRussel.png", true);

        batch = new SpriteBatch();
    }

    public void render( float deltaTime ){

        // SpriteBatch testing
        batch.begin();

        batch.setColor(1, 0, 0, 0.1f);
        batch.draw(texture, 0, 0, 100, 100);

        batch.draw(texture, 0, 0, 300, 300, 0.5f, 0.5f, 0.9f, 0.1f);
        batch.draw(texture, 300, 300, 50, 50);
        batch.setColor(1, 1, 1, 1);

        batch.draw(texture2, 400, 100, 100, 100);

        TextureRegion region = new TextureRegion(texture2, 0, 0, 512, 512);
        batch.draw(region, 200, 300, 64, 64);

        TextureRegion region2 = new TextureRegion(texture2, 0f, 1f, .5f, 0.5f);
        batch.draw(region2, 400, 300, 64, 64);

        int W = LibGPU.graphics.getWidth();
        int H = LibGPU.graphics.getHeight();
        batch.setColor(0, 1, 0, 1);
        for (int i = 0; i < 8000; i++) {
            batch.draw(texture2, (int) (Math.random() * W), (int) (Math.random() * H), 32, 32);
        }
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
        texture.dispose();
        texture2.dispose();
        batch.dispose();
    }

    @Override
    public void resize(int width, int height) {
        System.out.println("demo got resize");
    }


}
