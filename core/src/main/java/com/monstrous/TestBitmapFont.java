package com.monstrous;

import com.monstrous.graphics.SpriteBatch;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.TextureRegion;


public class TestBitmapFont implements ApplicationListener {

    private SpriteBatch batch;
    private Texture textureFont;
    private long startTime;
    private int frames;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        textureFont = new Texture("lsans-15.png", false);

        batch = new SpriteBatch();
    }

    public void render( float deltaTime ){

        // SpriteBatch testing
        batch.begin();

        //char id=65 x=80 y=33 width=11 height=13 xoffset=-1 yoffset=2 xadvance=9 page=0 chnl=0

        TextureRegion letterA = new TextureRegion(textureFont, 80f/256f, (33f+13f)/128f, (80+11f)/256f, 33f/128f);
        batch.draw(letterA, 100, 100);

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
        textureFont.dispose();
        batch.dispose();
    }

    @Override
    public void resize(int width, int height) {
        System.out.println("demo got resize");
    }


}
