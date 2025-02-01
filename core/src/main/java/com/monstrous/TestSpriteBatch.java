package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.utils.ScreenUtils;
import com.monstrous.wgpu.WGPUVertexFormat;


public class TestSpriteBatch extends ApplicationAdapter {


    private SpriteBatch batch;
    private Texture texture;
    private Texture texture2;
    private long startTime;
    private int frames;
    private VertexAttributes vaNoTex;
    private VertexAttributes vaNoColor;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        texture = new Texture("textures/monstrous.png", false);
        texture2 = new Texture("textures/jackRussel.png", true);

        vaNoTex = new VertexAttributes();
        vaNoTex.add(VertexAttribute.Usage.POSITION, "position",        WGPUVertexFormat.Float32x2, 0 );
        //vertexAttributes.add(VertexAttribute.Usage.TEXTURE_COORDINATE, "uv",    WGPUVertexFormat.Float32x2, 1 );
        vaNoTex.add(VertexAttribute.Usage.COLOR,"color",               WGPUVertexFormat.Float32x4, 2 );
        vaNoTex.end();

        vaNoColor = new VertexAttributes();
        vaNoColor.add(VertexAttribute.Usage.POSITION, "position",        WGPUVertexFormat.Float32x2, 0 );
        vaNoColor.add(VertexAttribute.Usage.TEXTURE_COORDINATE, "uv",    WGPUVertexFormat.Float32x2, 1 );
        //vaNoColor.add(VertexAttribute.Usage.COLOR,"color",               WGPUVertexFormat.Float32x4, 2 );
        vaNoColor.end();

        batch = new SpriteBatch();

    }

    public void render(  ){

        // SpriteBatch testing
        ScreenUtils.clear(Color.WHITE);

        batch.begin();
        //batch.setVertexAttributes(vaNoColor);

        batch.disableBlending();

        batch.setColor(Color.WHITE);
        batch.draw(texture, 100, 100, 100, 100);        // normal texture
        batch.setColor(Color.RED);
        batch.draw(texture, 400, 100, 100, 100);        // red tint
        batch.setColor(Color.WHITE);
        batch.draw(texture, 600, 200, 100, 100);        // back to default: white tint

        batch.draw(texture2, 800, 300, 100, 100);       // switch to different texture



        batch.setColor(1f, 1f, 1f, 0.5f);               // alpha 0.5
        batch.draw(texture2, 900, 400, 100, 100);

        batch.disableBlending();
        batch.draw(texture2, 1000, 500, 100, 100);      // disable blending

        batch.draw(texture, 0, 300, 300, 300, 0.5f, 0.5f, 0.9f, 0.1f);      // partial texture
        //batch.setVertexAttributes(vaNoTex);
        TextureRegion region = new TextureRegion(texture2, 0, 0, 512, 512);                 // partial texture via TextureRegion
        batch.draw(region, 200, 300, 64, 64);

        batch.end();


        // At the end of the frame
        if (System.nanoTime() - startTime > 1000000000) {
            System.out.println("SpriteBatch maxSpritesInBatch: " + batch.maxSpritesInBatch + " renderCalls: "+batch.renderCalls  );

            System.out.println("SpriteBatch : fps: " + frames  );
            frames = 0;
            startTime = System.nanoTime();
        }
        frames++;

    }

    public void dispose(){
        // cleanup
        texture.dispose();
        texture2.dispose();
        batch.dispose();
    }

    @Override
    public void resize(int width, int height) {

        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }


}
