package com.monstrous;

import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.Texture;


public class TestTextureMipMap extends ApplicationAdapter {

    private SpriteBatch batch;
    private Texture texture;

    public void create() {
        texture = new Texture("textures/jackRussel.png", true);
        //texture = new Texture("textures/input.jpg", true);


        batch = new SpriteBatch();
    }

    public void render(  ){

        batch.begin();


        batch.draw(texture, 0, 0, 1024, 1024);
        batch.draw(texture, 0, 0, 512, 512);
        batch.draw(texture, 512, 0, 256, 256);
        batch.draw(texture, 512+256, 0, 128, 128);
        batch.draw(texture, 512+256+128, 0, 64, 64);
        batch.draw(texture, 512+256+128+64, 0, 32, 32);
        batch.draw(texture, 512+256+128+64+32, 0, 16, 16);
        batch.draw(texture, 512+256+128+64+32+16, 0, 8, 8);
        batch.draw(texture, 512+256+128+64+32+16+8, 0, 4, 4);
        batch.draw(texture, 512+256+128+64+32+16+8+4, 0, 2, 2);


        batch.end();
    }

    public void dispose(){
        // cleanup
        System.out.println("demo exit");
        texture.dispose();
        batch.dispose();
    }


}
