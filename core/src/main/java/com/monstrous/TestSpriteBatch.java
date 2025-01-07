package com.monstrous;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.utils.ScreenUtils;

import java.util.Random;


public class TestSpriteBatch extends ApplicationAdapter {


    private SpriteBatch batch;
    private Texture texture;
    private Texture texture2;
    private long startTime;
    private int frames;
    private Random rng;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        texture = new Texture("textures/monstrous.png", false);
        texture2 = new Texture("textures/jackRussel.png", true);

        batch = new SpriteBatch();
        rng = new Random(1234);
    }

    public void render(  ){

        // SpriteBatch testing
        ScreenUtils.clear(Color.WHITE);
        batch.begin();

        batch.setColor(Color.WHITE);
        batch.draw(texture, 0, 0, 100, 100);


        batch.draw(texture, 400, 100, 100, 100);

        batch.draw(texture2, 600, 200, 100, 100);

        batch.draw(texture, 800, 300, 100, 100);

//        batch.draw(texture, 0, 0, 300, 300, 0.5f, 0.5f, 0.9f, 0.1f);
//        batch.draw(texture, 300, 300, 50, 50);
//        batch.setColor(1, 1, 1, 1);
//
//        batch.draw(texture2, 400, 100, 100, 100);
//
//        TextureRegion region = new TextureRegion(texture2, 0, 0, 512, 512);
//        batch.draw(region, 200, 300, 64, 64);
//
//        TextureRegion region2 = new TextureRegion(texture2, 0f, 1f, .5f, 0.5f);
//        batch.draw(region2, 400, 300, 64, 64);

        //rng.setSeed(1234);

//        int W = LibGPU.graphics.getWidth();
//        int H = LibGPU.graphics.getHeight();
//        batch.setColor(0, 1, 0, 1);
////        for(int x = 0; x < 100; x++){
////            for(int y = 0; y < 80; y++){
////                batch.draw(texture2,  x*16, y*16, 32, 32);
////            }
////        }
//        float x = 0;
//        float y = 0;
//        for (int i = 0; i < 8000; i++) {
//            x = (x+23)%W;
//            y = (y+13)%H;
//            //float y = rng.nextFloat()*H;
//            batch.draw(texture2,  x, y, 32, 32);
//        }
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
