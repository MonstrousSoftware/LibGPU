package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.g3d.ibl.HDRLoader;

import java.io.IOException;

/** Tests processing of HDR content (pixels with 16-bit float values).
 *  Load HDR content into texture and render it using ACES tone mapping.
 */

public class TestHDR extends ApplicationAdapter {

    private static final int ENVMAP_SIZE = 2048;

    private SpriteBatch batch;
    private Texture textureEquirectangular;
    private ShaderProgram toneMapper;


    @Override
    public void create() {
        batch = new SpriteBatch();

        FileHandle file = Files.internal("hdr/brown_photostudio_02_1k.hdr");
        //FileHandle file = Files.internal("hdr/leadenhall_market_2k.hdr");
        HDRLoader hdrLoader = new HDRLoader();

        try {
            hdrLoader.loadHDR(file);
            textureEquirectangular = hdrLoader.getHDRTexture(false);
        } catch(IOException e) {
            System.out.println("Cannot load HDR file.");
        }

        toneMapper = new ShaderProgram(Files.internal("shaders/sprite-HDR.wgsl"), "#define TEXTURE_COORDINATE\n" );
     }


    @Override
    public void render(){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){
            LibGPU.app.exit();
            return;
        }

        batch.begin(Color.TEAL);
        batch.setShader(toneMapper);
        batch.draw(textureEquirectangular, 0,0);
        batch.end();
    }

    @Override
    public void dispose(){
        // cleanup
        textureEquirectangular.dispose();
        batch.dispose();
        toneMapper.dispose();
    }

    @Override
    public void resize(int width, int height) {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }



}
