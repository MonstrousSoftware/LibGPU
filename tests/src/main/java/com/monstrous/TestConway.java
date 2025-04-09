package com.monstrous;


import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.utils.ScreenUtils;
import com.monstrous.utils.viewports.FitViewport;
import com.monstrous.utils.viewports.Viewport;

/**
 * Conway's Game of Life using WebGPU compute shader.
 *
 *
 * Based on LWJGL3 demo by Kai Burjack
 * https://www.youtube.com/watch?v=h7aCroRpkN0
 * https://github.com/LWJGL/lwjgl3-demos/blob/main/src/org/lwjgl/demo/opengl/shader/GameOfLife.java
 */

public class TestConway extends ApplicationAdapter {

    private static final int MAX_NUM_CELLS_X = 1024 * 4;
    private static final int MAX_NUM_CELLS_Y = 1024 * 4;
    private static final int WORK_GROUP_SIZE_X = 16;
    private static final int WORK_GROUP_SIZE_Y = 16;

    private SpriteBatch batch;
    private SpriteBatch batchText;
    private BitmapFont font;
    private Viewport viewport;


//    private int iterationProgram;
//    private Texture[] textures;
    private int readTexIndex;
//    private final List<GolPattern> patterns = new ArrayList<>();
//    private float zoom = 1f;
    private boolean paused = false;
    private boolean step = false;
    private int iteration = 0;
    private final StringBuffer sb = new StringBuffer();
//    private final Vector2 prevTouch = new Vector2();

    @Override
    public void create() {
        batch = new SpriteBatch();
        batchText = new SpriteBatch();
        font = new BitmapFont();
        viewport = new FitViewport(MAX_NUM_CELLS_X, MAX_NUM_CELLS_Y);
        viewport.getCamera().position.set(MAX_NUM_CELLS_X/2f, MAX_NUM_CELLS_Y/2f, 0);

        //LibGPU.input.setInputProcessor( this );
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
    }

    @Override
    public void render() {
        // process keyboard input
        handleKeys();

        // call compute shader to iterate one step
        if(!paused || step) {
            computeNextState();
            iteration++;
        }

        // render the texture to the screen
        ScreenUtils.clear(Color.BLACK);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        //batch.draw(textures[readTexIndex], 0, 0);
        batch.end();


        sb.setLength(0);
        sb.append("Iteration: ");
        sb.append(iteration);

        batchText.begin();
        font.draw(batchText, sb.toString() , 0,20);
        batchText.end();


        // switch input and output buffer for next iteration
        if(!paused || step)
            readTexIndex = 1 - readTexIndex;
        step = false;
    }

    private void handleKeys(){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE))
            LibGPU.app.exit();
        if(LibGPU.input.isKeyPressed(Input.Keys.SPACE))     // todo isKeyJustPressed()
            paused = !paused;
        if(LibGPU.input.isKeyPressed(Input.Keys.S))
            step = true;
    }


    private void computeNextState(){

    }


    @Override
    public void dispose() {
        batch.dispose();
        batchText.dispose();
//        for(Texture tex : textures )
//            tex.dispose();
//        for(GolPattern pat : patterns)
//            pat.pixmap.dispose();
        font.dispose();
    }

//    @Override
//    public boolean scrolled(float amountX, float amountY) {
//        if(amountY > 0)
//            zoom *= 1.1f;
//        else
//            zoom *= 0.9f;
//        ((OrthographicCamera)viewport.getCamera()).zoom = zoom;
//        return true;
//    }
//
//
//    @Override
//    public boolean keyDown(int keycode) {
//        return false;
//    }
//
//    @Override
//    public boolean keyUp(int keycode) {
//        return false;
//    }
//
//    @Override
//    public boolean keyTyped(char character) {
//        return false;
//    }
//
//    @Override
//    public boolean mouseMoved(int x, int y) {
//        return false;
//    }
//
//    @Override
//    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
//        prevTouch.set(screenX, screenY);
//        return true;
//    }
//
//    @Override
//    public boolean touchUp(int x, int y, int pointer, int button) {
//        return false;
//    }
//
//    @Override
//    public boolean touchDragged(int screenX, int screenY, int pointer) {
//        float dx = screenX - prevTouch.x;
//        float dy = screenY - prevTouch.y;
//        prevTouch.set(screenX, screenY);
//        viewport.getCamera().position.add(-dx*zoom*5f, dy*zoom*5f, 0);
//        return true;
//    }

}
