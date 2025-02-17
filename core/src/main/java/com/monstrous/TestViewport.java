package com.monstrous;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.utils.ScreenUtils;
import com.monstrous.utils.viewports.FitViewport;
import com.monstrous.utils.viewports.ScreenViewport;
import com.monstrous.utils.viewports.StretchViewport;
import com.monstrous.utils.viewports.Viewport;


public class TestViewport extends ApplicationAdapter {

    private SpriteBatch batch;
    private Texture background;
    private Viewport viewport;
    private Viewport[] viewports;
    private String[] names;
    private int index;
    private BitmapFont font;
    private boolean keyUp = true;

    @Override
    public void create() {

        background = new Texture("textures/simplegame/background.png", true);

        batch = new SpriteBatch();

        getViewports();
        index = 0;
        viewport = viewports[index];
        resize(LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());

        font = new BitmapFont();
    }

    @Override
    public void render(  ){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){
            LibGPU.app.exit();
            return;
        }
        if(LibGPU.input.isKeyPressed(Input.Keys.SPACE)){
            if(keyUp) {
                index = (index + 1) % viewports.length;
                viewport = viewports[index];
                resize(LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
                System.out.println("Switch to " + names[index]);
            }
            keyUp = false;
        } else
            keyUp = true;

        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        ScreenUtils.clear(Color.BLACK);
        batch.begin();
        batch.draw(background, 0,0); //, viewport.getWorldWidth(), viewport.getWorldHeight());
        font.draw(batch, names[index], 50, 90);
        font.draw(batch, "Press SPACE to switch viewport", 50, 30);
        batch.end();
    }

    @Override
    public void dispose(){
        // cleanup
        background.dispose();
        batch.dispose();
        font.dispose();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true); // true centers the camera
    }

    private void getViewports(){
        viewports = new Viewport[3];

        viewports[0] = new StretchViewport(800, 500);
        viewports[1] = new ScreenViewport();
        viewports[2] = new FitViewport(800,500);

        names = new String[]{ "StretchViewport", "ScreenViewport", "FitViewport"};
    }


}
