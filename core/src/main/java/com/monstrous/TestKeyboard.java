package com.monstrous;

import com.monstrous.graphics.SpriteBatch;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.TextureRegion;


public class TestKeyboard extends InputAdapter implements ApplicationListener {


    private SpriteBatch batch;
    private Texture texture;
    private int x, y;
    private int vx, vy;
    private int viewWidth, viewHeight;
    private long startTime;
    private int frames;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        texture = new Texture("textures/monstrous.png", false);

        batch = new SpriteBatch();
        LibGPU.input.setInputProcessor(this);
    }

    public void render(  ){

        // SpriteBatch testing
        batch.begin();

        batch.draw(texture, x, y, 100, 100);
        x += vx;
        y += vy;
//        x++;
//        if(x > viewWidth)
//            x = 0;
//        y++;
//        if(y > viewHeight)
//            y = 0;
        batch.end();


//        // At the end of the frame
//        if (System.nanoTime() - startTime > 1000000000) {
//            System.out.println("SpriteBatch : fps: " + frames  );
//            frames = 0;
//            startTime = System.nanoTime();
//        }
//        frames++;

    }

    public void dispose(){
        // cleanup
        System.out.println("demo exit");
        texture.dispose();
        batch.dispose();
    }

    @Override
    public boolean keyDown(int key){
        System.out.println("key: "+key);
        switch(key){
            case 30: vx = -1; break;
            case 32: vx = 1; break;
            case 17: vy = 1; break;
            case 31: vy = -1; break;
            default: return false;
        }
        return true;
    }

    @Override
    public boolean keyUp(int key){
        System.out.println("key: "+key);
        switch(key){
            case 30: vx = 0; break;
            case 32: vx = 0; break;
            case 17: vy = 0; break;
            case 31: vy = 0; break;
            default: return false;
        }
        return true;
    }


    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void resize(int width, int height) {

        System.out.println("demo got resize");
        viewWidth = width;
        viewHeight = height;
    }


}
