package com.monstrous;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.g2d.Sprite;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.utils.ScreenUtils;
import com.monstrous.utils.viewports.FitViewport;

import java.util.ArrayList;

// Following LibGDX tutorial "a simple game"
// https://libgdx.com/wiki/start/a-simple-game#loading-assets

// Note: no sound or music, also no actual catching of drops (lacking Rectangle class).

public class TestSimpleGame extends ApplicationAdapter {


    private SpriteBatch batch;
    private Texture background;
    private Texture dropTexture;
    private Texture bucketTexture;
    private Sprite bucketSprite;
    private FitViewport viewport;
    private final float speed = 5f;
    private ArrayList<Sprite> dropSprites;
    private float dropTimer = 0;

    @Override
    public void create() {

        background = new Texture("textures/simplegame/background.png", true);
        bucketTexture = new Texture("textures/simplegame/bucket.png", true);
        dropTexture = new Texture("textures/simplegame/drop.png", true);

        bucketSprite = new Sprite(bucketTexture);
        bucketSprite.setSize(1,1);

        batch = new SpriteBatch();
        viewport = new FitViewport(8,5);

        dropSprites = new ArrayList<>();
        createDroplet();
    }

    private void createDroplet() {
        // create local variables for convenience
        float dropWidth = 1;
        float dropHeight = 1;
        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();

        // create the drop sprite
        Sprite dropSprite = new Sprite(dropTexture);
        dropSprite.setSize(dropWidth, dropHeight);
        dropSprite.setX((float) (Math.random()*(worldWidth-dropWidth)));
        dropSprite.setY(worldHeight);
        dropSprites.add(dropSprite); // Add it to the list
    }

    private void input(float deltaTime) {
        if (LibGPU.input.isKeyPressed(Input.Keys.RIGHT)) {
            bucketSprite.translateX(speed*deltaTime); // Move the bucket right
        }
        if (LibGPU.input.isKeyPressed(Input.Keys.LEFT)) {
            bucketSprite.translateX(-speed*deltaTime);
        }
    }

    @Override
    public void render(  ){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){
            LibGPU.app.exit();
            return;
        }
        float delta = LibGPU.graphics.getDeltaTime();
        input( delta );

       viewport.apply();


        batch.setProjectionMatrix(viewport.getCamera().combined);

        ScreenUtils.clear(Color.WHITE);
        batch.begin();

        batch.draw(background, 0,0, viewport.getWorldWidth(), viewport.getWorldHeight());

        bucketSprite.draw(batch);

        // draw each sprite
        for (Sprite dropSprite : dropSprites) {
            dropSprite.draw(batch);
        }

        // Loop through the sprites backwards to prevent out of bounds errors
        for (int i = dropSprites.size() - 1; i >= 0; i--) {
            Sprite dropSprite = dropSprites.get(i); // Get the sprite from the list
            float dropWidth = dropSprite.getWidth();
            float dropHeight =dropSprite.getHeight();

            dropSprite.translateY(-2f * delta);

            // if the top of the drop goes below the bottom of the view, remove it
            if (dropSprite.getY() < -dropHeight) dropSprites.remove(i);

            // Rectangle test is missing here
        }

        dropTimer -= delta;
        if(dropTimer < 0){
            createDroplet();
            dropTimer = 1;
        }

        batch.end();
    }

    @Override
    public void dispose(){
        // cleanup
        background.dispose();
        batch.dispose();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true); // true centers the camera
    }


}
