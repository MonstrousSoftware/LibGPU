package com.monstrous;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.jlay.*;
import com.monstrous.jlay.utils.Align;
import com.monstrous.utils.ScreenUtils;


// Test of UI dynamic layout library

public class TestJLay extends ApplicationAdapter {

    private JLay stage;
    private Group group;
    private BitmapFont font;
    private SpriteBatch batch;

    @Override
    public void create() {
        font = new BitmapFont();
        batch = new SpriteBatch();

        stage = new JLay();
        stage.setDebug(true);

        group = new Group();
        group.setSize(500, 600);

        group.setPosition(50, 100);
        group.setColor(Color.TEAL);
        group.setPadding(5);
        group.setGap(10);
        group.setAlignment( Align.LEFT, Align.BOTTOM );
        stage.add(group);

        Label.Style style = new Label.Style(Color.WHITE, new BitmapFont(Files.internal("lsans32-sdf.fnt")));
        Label label = new Label("One Two Three Four Five Six", style);
        //label.setSize(200, 50);
        label.setColor(Color.GREEN_YELLOW);


        group.add(label);

//        Group group2 = new Group();
//        group2.setVertical();
//        group2.setColor(Color.BLUE);
//        group2.setSize(Widget.FIT, Widget.GROW);
//        group2.setAlignment(Align.MIDDLE, Align.START);
//        group2.setGap(5);
//        group2.setPadding(10);
//
//        //group2.setSize(200,200);
//        //group2.setPosition(10,10);
//
//        Box box0 = new Box();
//        box0.setColor(Color.ORANGE);
//        box0.setSize(150,150);
//        box0.setCornerRadius(5);
//        group2.add(box0);
//
//        Box box1 = new Box();
//        box1.setColor(Color.ORANGE);
//        box1.setSize(40,40);
//        box1.setCornerRadius(20);
//        group2.add(box1);
//
//
        Box box2 = new Box();
        box2.setColor(Color.ORANGE);
        box2.setSize(100,100);
        //box2.setGrow(true, false);
        box2.setCornerRadius(16);

        Box box3 = new Box();
        box3.setColor(Color.ORANGE);
        box3.setSize(100,100);
        //box3.setGrow(true, true);
        box3.setCornerRadius(16);



        Label label2 = new Label("Hello! What is this nonsense? Do we not have anything better to do?", style);
        label2.setColor(Color.GREEN_YELLOW);

        Group groupText = new Group();
        groupText.setSize(Widget.FIT, Widget.FIT);
        groupText.setColor(Color.BLUE);
        groupText.setPadding(20);
        groupText.add(label2);


//
//        //group2.add(box2);
//
//        group.add(group2);
//        group.add(box2);
        group.add(box3);
        group.add(groupText);

    }

    @Override
    public void render(   ){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){
            LibGPU.app.exit();
            return;
        }
        if(LibGPU.input.isButtonPressed(Input.Buttons.LEFT)) {
            float mw = LibGPU.input.getX() - 100;
            float mh = (LibGPU.graphics.getHeight() - LibGPU.input.getY()) - 100;
            if(mw > 0 && mh > 0)
                group.setSize(mw, mh);
        }

        ScreenUtils.clear(Color.WHITE);

        stage.draw();

        batch.begin();
        batch.setColor(Color.BLACK);
        font.draw(batch, "You can drag the top-right corner with the left mouse button.", 10, 50);
        batch.end();


    }

    @Override
    public void dispose(){
        // cleanup
        stage.dispose();
    }

    @Override
    public void resize(int width, int height) {
        stage.resize(width, height);
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }


}
