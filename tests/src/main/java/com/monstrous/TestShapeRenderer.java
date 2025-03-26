package com.monstrous;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.g2d.ShapeRenderer;
import com.monstrous.utils.ScreenUtils;


public class TestShapeRenderer extends ApplicationAdapter {

    private long startTime;
    private int frames;
    private ShapeRenderer sr;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        sr = new ShapeRenderer();
    }

    public void render(  ){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){
            LibGPU.app.exit();
            return;
        }


        ScreenUtils.clear(Color.BLACK);
        sr.begin();

        sr.setLineWidth(1f);
        sr.setColor(Color.GREEN);
        sr.box(10,10,420, 160);


        sr.setLineWidth(5f);
        sr.setColor(Color.BLUE);
        sr.box(200,110,520, 460);

        sr.setLineWidth(1f);
        sr.setColor(Color.RED);
        sr.line(250,160,480, 410);

        sr.setLineWidth(1f);
        sr.setColor(Color.WHITE);
        sr.triangle(50,50,100, 100, 150, 50);

        sr.end();


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
        sr.dispose();
    }

    @Override
    public void resize(int width, int height) {
        sr.getProjectionMatrix().setToOrtho2D(0,0, width, height);
    }


}
