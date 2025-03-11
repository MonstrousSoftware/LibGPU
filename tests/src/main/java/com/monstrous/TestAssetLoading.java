package com.monstrous;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.g2d.SpriteBatch;


public class TestAssetLoading extends ApplicationAdapter {

    private SpriteBatch batch;
    private BitmapFont font;
    private String message;
    private Texture texture;



    @Override
    public void create() {

        batch = new SpriteBatch();
        font = new BitmapFont();

        FileHandle handle = Files.classpath("hello.txt");        // read file from assets folder
        message = handle.readString();
        System.out.println("Contents of file hello.txt: " +message);

        FileHandle handle2 = Files.classpath("font/lsans-15.png");
        texture = new Texture(handle2, false);
    }

    @Override
    public void render(){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){
            LibGPU.app.exit();
            return;
        }


        batch.begin(Color.TEAL);
        batch.draw(texture, 0, 400);

        font.draw(batch, message, 10, 50);

        batch.end();




    }

    @Override
    public void dispose(){
        // cleanup
        font.dispose();
        batch.dispose();
        texture.dispose();
    }

    @Override
    public void resize(int width, int height) {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }


}
