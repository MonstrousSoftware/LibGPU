package com.monstrous;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.g2d.ShapeRenderer;
import com.monstrous.scene2d.Align;
import com.monstrous.scene2d.Block;
import com.monstrous.scene2d.Stage;
import com.monstrous.scene2d.Table;
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
        System.out.println("demo exit");
        sr.dispose();
    }

    @Override
    public void resize(int width, int height) {

        System.out.println("demo got resize");
    }


}
