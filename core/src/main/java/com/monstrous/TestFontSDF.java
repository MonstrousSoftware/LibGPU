package com.monstrous;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.ShaderProgram;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.utils.ScreenUtils;


// Demonstrate signed distance field font rendering
// Uses a special shader for SpriteBatch.
//
// To do: smoothing value is hard coded in the shader code. Should be set as a uniform so that it can adapt to the font scale (and font characteristics).
// But adding one extra uniform is not so trivial.


public class TestFontSDF extends ApplicationAdapter {

    private final float[] SCALES = { 0.25f, 0.5f, 1f, 2f, 4f, 8f };

    private SpriteBatch batch;
    private Texture texture;
    private BitmapFont font;
    private ShaderProgram sdfShader;
    private long startTime;
    private int frames;


    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        texture = new Texture("textures/jackRussel.png");

        font = new BitmapFont("lsans32-sdf.fnt");
        sdfShader = new ShaderProgram("shaders/sprite-distanceField.wgsl");

        batch = new SpriteBatch(8192);
    }

    public void render( ){

        ScreenUtils.clear(0,0,0,1);

        //
        batch.begin();

        batch.draw(texture, 0, 0, 300, 300);    // draw texture with default sprite batch shader
        batch.setShader(sdfShader);     // switch to SDF shader

        float y = LibGPU.graphics.getHeight() - 50f;
        for(float scale : SCALES) {
            font.setScale(scale);
            font.draw(batch, "The quick brown fox", 100, y);
            y -= font.getLineHeight();
        }
        batch.setShader(null);  // reset default shader in case we want to do more with this batch

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
        font.dispose();
        batch.dispose();
        sdfShader.dispose();
    }

    @Override
    public void resize(int width, int height) {
        batch.resize(width, height);
    }


}
