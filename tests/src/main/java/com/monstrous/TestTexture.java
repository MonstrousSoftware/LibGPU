package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g2d.Sprite;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.webgpu.WGPUTextureFormat;
import com.monstrous.webgpu.WGPUVertexFormat;


public class TestTexture extends ApplicationAdapter {

    private SpriteBatch batch;
    private Texture texture;
    private Texture background;
    private Sprite sprite;



    @Override
    public void create() {

        background = new Texture("textures/simplegame/background.png", true);
        //texture = new Texture("textures/simplegame/bucket.png", true);
        texture = new Texture("textures/simplegame/drop.png", true);
//        texture = new Texture("textures/monstrous.png", false);
        //texture2 = new Texture("textures/jackRussel.png", true);
        //texture = new Texture("textures/alien.png", false);
        System.out.println("Texture format:" + texture.getFormat());
        batch = new SpriteBatch();

        sprite = new Sprite(texture);
    }

    @Override
    public void render(){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){
            LibGPU.app.exit();
            return;
        }

        // SpriteBatch testing
        batch.begin(Color.TEAL);
        batch.draw(background, 0, 0);
        batch.draw(sprite, 300, 100);
        batch.end();
    }

    @Override
    public void dispose(){
        // cleanup
        texture.dispose();
        batch.dispose();
    }

    @Override
    public void resize(int width, int height) {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }


}
