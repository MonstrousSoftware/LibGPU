package com.monstrous;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.scene2d.Event;
import com.monstrous.scene2d.EventListener;
import com.monstrous.scene2d.Stage;
import com.monstrous.scene2d.TextButton;
import com.monstrous.utils.ScreenUtils;


public class TestMenu extends ScreenAdapter {

    private static final String[] testNames = { "SpriteBatch", "FontSDF", "Shadow"};

    private Game game;
    private Stage stage;


    public TestMenu(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage();
        LibGPU.input.setInputProcessor(stage);
    }


    private void fillStage(){
        stage.clear();

        TextButton.Style TBstyle = new TextButton.Style();

        TBstyle.font = new BitmapFont();
        TBstyle.fontColor = Color.BLUE;
        TBstyle.bgColor = Color.WHITE;

        for(String name : testNames )
        {
            TextButton textButton = new TextButton(name, TBstyle);
            textButton.setPreferredSize(100, 30);
            textButton.addListener(new EventListener() {
                @Override
                public boolean handle(int event) {
                    if (event == Event.CLICKED) {
                        return switchScreen(name);
                    }
                    return false;
                }
            });
            stage.add(textButton);
            stage.row();
        }

    }


    private boolean switchScreen(String name){
        if(name.contentEquals("FontSDF"))
            game.setScreen(new TestFontSDF(game));
        else if(name.contentEquals("SpriteBatch"))
            game.setScreen(new TestSpriteBatch(game));
        else if(name.contentEquals("Shadow"))
            game.setScreen(new TestShadow(game));
        else
            throw new RuntimeException("No class known for test: "+name);
        return true;
    }


    @Override
    public void render( float deltaTime ){
        ScreenUtils.clear(Color.WHITE); // todo broken
        stage.draw();
    }


    @Override
    public void resize(int width, int height) {
        stage.resize(width, height);
        fillStage();
    }

    @Override
    public void dispose(){
        stage.dispose();
    }

}
