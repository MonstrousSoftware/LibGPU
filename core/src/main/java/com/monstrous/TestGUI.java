package com.monstrous;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.TextureRegion;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.scene2d.*;
import com.monstrous.utils.ScreenUtils;


public class TestGUI extends ApplicationAdapter {


    private long startTime;
    private int frames;
    private Stage stage;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        stage = new Stage();

        Block block = new Block();
        block.setSize( 100, 100);
        block.setAlign(Align.left | Align.top);
        block.pad(10);
        stage.add(block);

        Block block2 = new Block();
        block2.setSize( 50, 50);
        block2.setAlign(Align.right | Align.top);
        block2.pad(10);
        stage.add(block2);
        stage.row();

        Table table = new Table();
        Block b1 = new Block();
        b1.setColor(Color.GREEN);
        b1.setSize(20,20);
        table.add(b1);
        Block b2 = new Block();
        b2.setColor(Color.RED);
        b2.setSize(20,20);
        table.add(b2);
        table.row();
        Block b3 = new Block();
        b3.setColor(Color.BLUE);
        b3.setSize(20,20);
        table.add(b3);
        Block b4 = new Block();
        b4.setColor(Color.GREEN);
        b4.setSize(20,20);
        table.add(b4);



        stage.add(table);

        Label.LabelStyle style = new Label.LabelStyle();
        style.font = new BitmapFont();
        style.bgColor = Color.GRAY;
        style.fontColor = Color.WHITE;

        Label label = new Label("libGPU", style);
        label.setSize( 1, 1);
        label.setAlign(Align.center);
        label.pad(10);
        stage.add(label);

        stage.debug();

        LibGPU.input.setInputProcessor(stage);

    }

    public void render(  ){


        ScreenUtils.clear(Color.BLACK);
        stage.draw();


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
        stage.dispose();
    }

    @Override
    public void resize(int width, int height) {

        System.out.println("demo got resize");
    }


}
