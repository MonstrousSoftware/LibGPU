package com.monstrous;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.ShaderProgram;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.utils.ScreenUtils;


public class TestHDR extends ApplicationAdapter {

    private SpriteBatch batch;
    private Texture texture;
    private ShaderProgram shader;


    @Override
    public void create() {
        texture = new Texture("hdr/brown_photostudio_02_1k.hdr", false);
        batch = new SpriteBatch();
        shader = new ShaderProgram(Files.internal("shaders/sprite-HDR.wgsl"));
    }

    @Override
    public void render(){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){
            LibGPU.app.exit();
            return;
        }

        ScreenUtils.clear(Color.WHITE);

        batch.begin();
        batch.setShader(shader);
        batch.draw(texture, 0,0);
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
