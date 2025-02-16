package com.monstrous;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.scene2d.Event;
import com.monstrous.scene2d.EventListener;
import com.monstrous.scene2d.Stage;
import com.monstrous.scene2d.TextButton;
import com.monstrous.utils.ScreenUtils;


public class TestMenu extends ApplicationAdapter {

    private static final String[] testNames = { "SpriteBatch", "FontSDF", "Shadow", "Post-Processing", "ShapeRenderer", "Simple Game"};

    private Stage stage;


    @Override
    public void create() {
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
        ApplicationListener listener = null;
        if(name.contentEquals("SpriteBatch"))
            listener = new TestSpriteBatch();
        else if(name.contentEquals("FontSDF"))
            listener = new TestFontSDF();
        else if(name.contentEquals("Shadow"))
            listener = new TestShadow();
        else if(name.contentEquals("Post-Processing"))
            listener = new TestPostProcessing();
        else if(name.contentEquals("ShapeRenderer"))
            listener = new TestShapeRenderer();
        else if(name.contentEquals("Simple Game"))
            listener = new TestSimpleGame();
        else
            throw new RuntimeException("No class known for test: "+name);

        LibGPU.app.setNextListener(listener, true);
        LibGPU.app.exit();
        return true;
    }


    @Override
    public void render( ){
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
