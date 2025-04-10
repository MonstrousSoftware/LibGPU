package com.monstrous;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.scene2d.Event;
import com.monstrous.scene2d.EventListener;
import com.monstrous.scene2d.Stage;
import com.monstrous.scene2d.TextButton;
import com.monstrous.utils.ScreenUtils;

// This implements a selection menu for different test applications

public class Menu extends ApplicationAdapter {

    private static final String[] testNames = { "SpriteBatch", "ShapeRenderer", "FontSDF", "Rounded Rectangle", "Simple Game", "Viewport", "GUI", "Build Model", "Instancing", "Frustum demo",
            "Lighting", "Shadow", "Post-Processing", "Cube Map", "Skybox",  "GLTF", "GLTF (GLB format)", "GLTF (Sponza)", "Animation", "Image Based Lighting", "IBL Generator", "Compute MipMap"  };

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

        int index = 0;
        int NUM_COLS = 2;
        for(String name : testNames )
        {
            TextButton textButton = new TextButton(name, TBstyle);
            textButton.setPreferredSize(200, 20);
            textButton.addListener(new EventListener() {
                @Override
                public boolean handle(int event) {
                    if (event == Event.CLICKED) {
                        return switchScreen(name);
                    }
                    return false;
                }
            });
            index++;
            stage.add(textButton);
            if(index % NUM_COLS == 0)
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
        else if(name.contentEquals("Rounded Rectangle"))
            listener = new TestRoundedRectangleSDF();
        else if(name.contentEquals("Simple Game"))
            listener = new TestSimpleGame();
        else if(name.contentEquals("Cube Map"))
            listener = new TestCubeMap();
        else if(name.contentEquals("Build Model"))
            listener = new TestModelBuild();
        else if(name.contentEquals("Instancing"))
            listener = new TestDuckField();
        else if(name.contentEquals("Frustum demo"))
            listener = new TestFrustum();
        else if(name.contentEquals("Skybox"))
            listener = new TestSkyBox();
        else if(name.contentEquals("Viewport"))
            listener = new TestViewport();
        else if(name.contentEquals("GLTF"))
            listener = new TestGLTF();
        else if(name.contentEquals("GLTF (GLB format)"))
            listener = new TestGLB();
        else if(name.contentEquals("GLTF (Sponza)"))
            listener = new TestSponza();
        else if(name.contentEquals("GUI"))
            listener = new TestGUI();
        else if(name.contentEquals("Lighting"))
            listener = new TestLighting();
        else if(name.contentEquals("Animation"))
            listener = new TestAnimatedGLTF();
        else if(name.contentEquals("IBL Generator"))
            listener = new TestIBLGen();
        else if(name.contentEquals("Image Based Lighting"))
            listener = new TestIBL();
        else if(name.contentEquals("Compute MipMap"))
            listener = new TestComputeMipMap();
        else
            throw new RuntimeException("No class known for test: "+name);

        LibGPU.app.setNextListener(listener, true);
        LibGPU.app.exit();
        return true;
    }


    @Override
    public void render( ){
        ScreenUtils.clear(Color.TEAL);
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
