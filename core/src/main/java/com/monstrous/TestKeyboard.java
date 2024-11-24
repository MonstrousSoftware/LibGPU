package com.monstrous;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.SpriteBatch;
import com.monstrous.graphics.Texture;
import com.monstrous.utils.ScreenUtils;


// Test of keyDown() and keyUp()

public class TestKeyboard extends InputAdapter implements ApplicationListener {

    private SpriteBatch batch;
    private Texture texture;
    private int x, y;
    private int vx, vy;
    private int viewWidth, viewHeight;
    private BitmapFont font;


    public void create() {
        texture = new Texture("textures/monstrous.png", false);

        batch = new SpriteBatch();
        font = new BitmapFont();
        LibGPU.input.setInputProcessor(this);
    }

    public void render(  ){

        moveSprite();

        ScreenUtils.clear(0.5f, 0.9f, 0.5f, 1f);
        batch.begin();
        batch.draw(texture, x, y);
        font.draw(batch, "Press W, A, S or D to move", 10, 40);
        batch.end();
    }

    private void moveSprite(){
        x += vx;
        y += vy;
        if(x < 0){
            x = 0;
            vx = 0;
        } else if(x +texture.getWidth() > viewWidth){
            x = viewWidth - texture.getWidth();
            vx = 0;
        }
        if(y < 0){
            y = 0;
            vy = 0;
        } else if(y +texture.getHeight() > viewHeight){
            y = viewHeight - texture.getHeight();
            vy = 0;
        }
    }

    @Override
    public void resize(int width, int height) {
        viewWidth = width;
        viewHeight = height;
        x = (viewWidth-texture.getWidth())/2;
        y = (viewHeight-texture.getHeight())/2;
    }

    public void dispose(){
        // cleanup
        texture.dispose();
        batch.dispose();
        font.dispose();
    }

    @Override
    public boolean keyDown(int key){
        System.out.println("key: "+key);
        switch(key){
            case 30: vx = -5; break;
            case 32: vx = 5; break;
            case 17: vy = 5; break;
            case 31: vy = -5; break;
            default: return false;
        }
        return true;
    }

    @Override
    public boolean keyUp(int key){
        System.out.println("key: "+key);
        switch(key){
            case 30, 32: vx = 0; break;
            case 17, 31: vy = 0; break;
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




}
