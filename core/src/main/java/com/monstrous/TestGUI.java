package com.monstrous;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.scene2d.*;
import com.monstrous.utils.ScreenUtils;


public class TestGUI extends ApplicationAdapter {


    private long startTime;
    private int frames;
    private Stage stage;

    CheckBox.Wrapper cheese = new CheckBox.Wrapper(true);
    CheckBox.Wrapper fries = new CheckBox.Wrapper(false);
    CheckBox.Wrapper all = new CheckBox.Wrapper(false);

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        stage = new Stage();

        Block block = new Block();
        block.setSize( 100, 100);
        block.setAlign(Align.left | Align.top);
        block.pad(10);
        stage.add(block);

        CheckBox.Style cbStyle = new CheckBox.Style();
        cbStyle.font = new BitmapFont();
        cbStyle.fontColor = Color.WHITE;

        Table cbTable = new Table();

            CheckBox cb = new CheckBox(cheese, "Do you like cheese?", cbStyle);
            cb.setAlign(Align.left | Align.top);
            cb.pad(10);


            CheckBox cb2 = new CheckBox(fries, "You want fries with that?", cbStyle);
            cb2.setAlign(Align.left | Align.top);
            cb2.pad(10);

            CheckBox cb3 = new CheckBox(all, "Will that be all?", cbStyle);
            cb3.setAlign(Align.left | Align.top);
            cb3.pad(10);

            cbTable.add(cb);
            cbTable.row();
            cbTable.add(cb2);
            cbTable.row();
            cbTable.add(cb3);


        stage.add(cbTable);
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
        //table.pack();

        Table t2 = new Table();

        Label.LabelStyle style = new Label.LabelStyle();
        style.font = new BitmapFont();
        style.fontColor = Color.WHITE;

        Label label = new Label("libGPU", style);
        label.setSize( 1, 1);
        label.setAlign(Align.center);
        label.pad(10);
        t2.add(label);

        t2.row();

        Button button = new Button();
        button.setSize(100, 30);
        button.setAlign(Align.center);
        //button.add(new Label("OKAY", style));
        t2.add(button);
        button.pack();
        //t2.pack();

        stage.add(t2);
        //t2.pack();

        //stage.pack();
        stage.debug();

        LibGPU.input.setInputProcessor(stage);

    }

    public void render(  ){


        ScreenUtils.clear(Color.BLACK);
        stage.draw();

        System.out.println("Values : "+cheese.value + ", "+ fries.value +" , "+ all.value );

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
