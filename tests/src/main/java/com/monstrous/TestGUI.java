package com.monstrous;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.scene2d.*;
import com.monstrous.utils.ScreenUtils;


public class TestGUI extends ApplicationAdapter {


    private long startTime;
    private int frames;
    private Stage stage;

    WrappedBoolean cheese = new WrappedBoolean(true);
    WrappedBoolean fries = new WrappedBoolean(false);
    WrappedBoolean all = new WrappedBoolean(false);
    WrappedFloat volume = new WrappedFloat(0);
    WrappedFloat degrees = new WrappedFloat(0);

    public void create() {
        startTime = System.nanoTime();
        frames = 0;
        stage = new Stage();
        LibGPU.input.setInputProcessor(stage);
        fillStage();
    }

    private void fillStage(){
        stage.clear();

        Block block = new Block();
        block.setPreferredSize( 100, 100);
        stage.add(block).pad(10).setAlign(Align.left | Align.top).setWidth(400);

        CheckBox.Style cbStyle = new CheckBox.Style();
        cbStyle.font = new BitmapFont();
        cbStyle.fontColor = Color.WHITE;

        Label.Style style = new Label.Style();
        style.font = new BitmapFont();
        style.fontColor = Color.WHITE;

        Label.Style editStyle = new Label.Style();
        editStyle.font = new BitmapFont();
        editStyle.fontColor = Color.BLACK;




        Table cbTable = new Table();
        cbTable.setFillParent(true);

            CheckBox cb = new CheckBox(cheese, "Do you like cheese?", cbStyle);
            cbTable.add(cb).setAlign(Align.left | Align.top).pad(5);//.setHeight(80);

            cbTable.row();

            CheckBox cb2 = new CheckBox(fries, "You want fries with that?", cbStyle);
            cbTable.add(cb2).setAlign(Align.left | Align.top).pad(5);
            cbTable.row();

            CheckBox cb3 = new CheckBox(all, "Will that be all?", cbStyle);
            cbTable.add(cb3).setAlign(Align.left | Align.top).pad(5);;
            cbTable.row();

            Slider slider2 = new Slider(degrees, 0, 360, 5f);
            cbTable.add(slider2).setAlign(Align.center | Align.bottom).pad(0);
            cbTable.row();
            slider2.setPreferredSize(200,32);

            FloatLabel fl = new FloatLabel(degrees,"heading:", style);
            cbTable.add(fl).setAlign(Align.top);
            cbTable.row();

            TextField tf = new TextField(editStyle);
            tf.setPreferredSize(200, 20);
            tf.setText("Name");
            cbTable.add(tf);
            cbTable.row();


            Button button2 = new Button();
            button2.setPreferredSize(100, 30);
            cbTable.add(button2).setAlign(Align.center);
            button2.addListener(new EventListener() {
                @Override
                public boolean handle(int event) {
                    if(event == Event.CLICKED)
                        System.out.println("CLICKED!");
                    return false;
                }
            });


        stage.add(cbTable);
        stage.row();


        Table table = new Table();
        table.setFillParent(true);
            Block b1 = new Block();
            b1.setColor(Color.GREEN).setPreferredSize(20,20);
            table.add(b1).setAlign(Align.center);
            Block b2 = new Block();
            b2.setColor(Color.RED);
            b2.setPreferredSize(20,20);
            table.add(b2);
            table.row();
            Block b3 = new Block();
            b3.setColor(Color.BLUE);
            b3.setPreferredSize(20,20);
            table.add(b3);
            Block b4 = new Block();
            b4.setColor(Color.GREEN);
            b4.setPreferredSize(20,20);
            table.add(b4);
        stage.add(table);

        Table t2 = new Table();
        t2.setFillParent(true);


        Label label = new Label("Welcome to the wonderful world of libGPU!", style);
        t2.add(label).setAlign(Align.top).pad(10);
//
//        t2.row();
//
////        Button button = new Button();
////        button.setPreferredSize(100, 30);
////        t2.add(button).setAlign(Align.center);
////        t2.row();
//
        TextButton.Style TBstyle = new TextButton.Style();

        TBstyle.font = new BitmapFont();
        TBstyle.fontColor = Color.BLUE;
        TBstyle.bgColor = Color.WHITE;

        TextButton textButton = new TextButton("EXIT", TBstyle);
        textButton.setPreferredSize(100, 30);
        textButton.addListener(new EventListener() {
            @Override
            public boolean handle(int event) {
                if(event == Event.CLICKED)
                    LibGPU.app.exit();
                return false;
            }
        });
        t2.add(textButton).setAlign(Align.center).pad(10);



        stage.add(t2);

        //stage.debug();

    }

    public void render(  ){


        ScreenUtils.clear(Color.WHITE); // todo broken
        stage.draw();

        //System.out.println("Values : "+cheese.value + ", "+ fries.value +" , "+ all.value+", "+volume.value +", "+degrees.value);

        // At the end of the frame
//        if (System.nanoTime() - startTime > 1000000000) {
//            System.out.println("SpriteBatch : fps: " + frames  );
//            frames = 0;
//            startTime = System.nanoTime();
//        }
//        frames++;

    }

    public void dispose(){
        // cleanup
        System.out.println("demo exit");
        stage.dispose();
    }

    @Override
    public void resize(int width, int height) {
        stage.resize(width, height);
        System.out.println("demo got resize");
    }


}
