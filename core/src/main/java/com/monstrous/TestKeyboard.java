package com.monstrous;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.SpriteBatch;
import com.monstrous.graphics.Texture;
import com.monstrous.utils.ScreenUtils;


// Test of keyDown() and keyUp()

public class TestKeyboard extends InputAdapter implements ApplicationListener {

    private final float SPEED = 250f;

    private SpriteBatch batch;
    private Texture texture;
    private float x, y;
    private float vx, vy;
    private int viewWidth, viewHeight;
    private BitmapFont font;
    private String eventString;


    public void create() {
        texture = new Texture("textures/smile.png", false);

        batch = new SpriteBatch();
        font = new BitmapFont();
        LibGPU.input.setInputProcessor(this);
        eventString = "";
    }

    public void render(  ){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE))
            LibGPU.app.exit();

        moveSprite();

        ScreenUtils.clear(0.5f, 0.9f, 0.5f, 1f);
        batch.begin();
        batch.draw(texture, x, y);
        font.draw(batch, "Press W, A, S or D to move", 10, 80);
        font.draw(batch, eventString, 10, 40);
        batch.end();
    }

    private void moveSprite(){
        float deltaTime = LibGPU.graphics.getDeltaTime();
        x += vx * deltaTime;
        y += vy * deltaTime;
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
        x = (viewWidth-texture.getWidth())/2f;
        y = (viewHeight-texture.getHeight())/2f;
    }

    public void dispose(){
        // cleanup
        texture.dispose();
        batch.dispose();
        font.dispose();
    }

    @Override
    public boolean keyDown(int key){
        eventString = "keyDown "+key+ "      ";
        switch(key){
            case Input.Keys.A: vx = -SPEED; break;
            case Input.Keys.D: vx = SPEED; break;
            case Input.Keys.W: vy = SPEED; break;
            case Input.Keys.S: vy = -SPEED; break;
            default: return false;
        }
        return true;
    }

    @Override
    public boolean keyUp(int key){
        eventString = "keyUp "+key + "    ";
        switch(key){
            case Input.Keys.A, Input.Keys.D: vx = 0; break;
            case Input.Keys.W, Input.Keys.S: vy = 0; break;
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
