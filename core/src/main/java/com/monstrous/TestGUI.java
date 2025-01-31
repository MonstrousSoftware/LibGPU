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

        Block block = new Block();
        block.setSize( 100, 100);
        block.setAlign(Align.left | Align.top);
        block.pad(10);
        stage.add(block);

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

            CheckBox cb = new CheckBox(cheese, "Do you like cheese?", cbStyle);
            cb.setAlign(Align.left | Align.top);
            cb.pad(10);

            CheckBox cb2 = new CheckBox(fries, "You want fries with that?", cbStyle);
            cb2.setAlign(Align.left | Align.top);
            cb2.pad(10);

            CheckBox cb3 = new CheckBox(all, "Will that be all?", cbStyle);
            cb3.setAlign(Align.left | Align.top);
            cb3.pad(10);

//            Slider slider = new Slider(volume, 0, 10, 2f);
//            slider.setAlign(Align.left | Align.top);
//            slider.setSize(200,16);
//            slider.pad(10);

            Slider slider2 = new Slider(degrees, 0, 360, 5f);
            slider2.setAlign(Align.left | Align.top);
            slider2.setSize(200,32);
            slider2.pad(10);

            FloatLabel fl = new FloatLabel(degrees,"heading:", style);

            TextField tf = new TextField(editStyle);
            tf.setSize(200, 20);
            tf.setText("Name");



            Button button2 = new Button();
            button2.setSize(100, 30);
            button2.setAlign(Align.center);
            button2.addListener(new EventListener() {
                @Override
                public boolean handle(int event) {
                    if(event == Event.CLICKED)
                        System.out.println("CLICKED!");
                    return false;
                }
            });

            cbTable.add(cb);
            cbTable.row();
            cbTable.add(cb2);
            cbTable.row();
            cbTable.add(cb3);
            cbTable.row();
//            cbTable.add(slider);
//            cbTable.row();
            cbTable.add(slider2);
            cbTable.row();
            cbTable.add(fl);
            cbTable.row();
            cbTable.add(tf);
            cbTable.row();
            cbTable.add(button2);


        stage.add(cbTable);
        stage.row();


        Table table = new Table();
        Block b1 = new Block();
        b1.setColor(Color.GREEN).setSize(20,20);
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


        Label label = new Label("Welcome to the wonderful world of libGPU!", style);
        label.setAlign(Align.top).pad(10);

        t2.add(label);

        t2.row();

        Button button = new Button();
        button.setSize(100, 30).setAlign(Align.center);
        t2.add(button);
        t2.row();

        button.pack();
        //t2.pack();

        TextButton.Style TBstyle = new TextButton.Style();

        TBstyle.font = new BitmapFont();
        TBstyle.fontColor = Color.BLUE;
        TBstyle.bgColor = Color.WHITE;

        TextButton textButton = new TextButton("OKAY", TBstyle);
        textButton.setSize(100, 30).setAlign(Align.center);
        textButton.addListener(new EventListener() {
            @Override
            public boolean handle(int event) {
                if(event == Event.CLICKED)
                    System.out.println("Hello OKAY!");
                return false;
            }
        });
        t2.add(textButton);



        stage.add(t2);

        //t2.pack();



        //stage.pack();
        stage.debug();

        LibGPU.input.setInputProcessor(stage);

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

        System.out.println("demo got resize");
    }


}
