package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.webgpu.WGPUTextureFormat;
import com.monstrous.webgpu.WGPUVertexFormat;


public class TestTexture extends ApplicationAdapter {

    private SpriteBatch batch;
    private Texture texture;



    @Override
    public void create() {

        texture = new Texture("textures/monstrous.png", false);
        //texture2 = new Texture("textures/jackRussel.png", true);
        //texture = new Texture("textures/alien.png", false);
        System.out.println("Texture format:" + texture.getFormat());
        batch = new SpriteBatch();
    }

    @Override
    public void render(){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){
            LibGPU.app.exit();
            return;
        }

        // SpriteBatch testing
        batch.begin(Color.TEAL);
        batch.draw(texture, 100, 100);
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
