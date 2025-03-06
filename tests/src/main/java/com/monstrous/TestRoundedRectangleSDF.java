package com.monstrous;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.ShaderProgram;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.g2d.RRBatch;


// Demonstrate rounded rectangles using an SDF shader
// todo something like this https://www.shadertoy.com/view/WtdSDs
// frag shader needs position, size, radius (pass via vertex buffer? or instance buffer?)

public class TestRoundedRectangleSDF extends ApplicationAdapter {

    private final float[] SCALES = { 0.25f, 0.5f, 1f, 2f, 4f, 8f };

    private RRBatch batch;
    private BitmapFont font;
    private Texture texture;
    private ShaderProgram sdfShader;
    private long startTime;
    private int frames;

    @Override
    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        texture = new Texture(1,1);
        texture.fill(Color.WHITE);

        font = new BitmapFont("lsans32-sdf.fnt");
        sdfShader = new ShaderProgram(Files.classpath("shaders/sprite-distanceField.wgsl"));

        batch = new RRBatch();
    }

    @Override
    public void render(   ){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){
            LibGPU.app.exit();
            return;
        }

        float x = LibGPU.input.getX();
        float y = LibGPU.input.getY();
        System.out.println("y = "+y);

        //
        batch.begin(Color.WHITE);
        batch.setColor(Color.ORANGE);
        batch.draw(x,y, 800, 400, 30);

        batch.setColor(Color.TEAL);
        batch.draw(100, 100, 400, 400, 30);

        batch.end();


        // At the end of the frame
        if (System.nanoTime() - startTime > 1000000000) {
            System.out.println("SpriteBatch : fps: " + frames  );
            frames = 0;
            startTime = System.nanoTime();
        }
        frames++;

    }

    @Override
    public void dispose(){
        // cleanup
        font.dispose();
        batch.dispose();
        sdfShader.dispose();
    }

    @Override
    public void resize(int width, int height) {
        //batch.resize(width, height);
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }


}
