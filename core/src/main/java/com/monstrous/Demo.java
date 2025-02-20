package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.g3d.Mesh;
import com.monstrous.graphics.g3d.MeshPart;
import com.monstrous.graphics.g3d.ModelBatch;
import com.monstrous.graphics.loaders.MeshData;
import com.monstrous.graphics.loaders.ObjLoader;
import com.monstrous.math.Matrix4;
import com.monstrous.wgpu.*;


public class Demo extends ApplicationAdapter {
    private WebGPU webGPU;

    private Mesh mesh;
    private MeshPart meshPart;
    private Renderable renderable;
    private ModelBatch modelBatch;

    private Camera camera;

    private Texture texture;
    private Texture texture2;
    private Matrix4 modelMatrix;
    private Texture textureFont;
    private float currentTime;
    private SpriteBatch batch;
    private long startTime;
    private int frames;

    public void create() {

        startTime = System.nanoTime();
        frames = 0;

        webGPU = LibGPU.webGPU;

        MeshData meshData = ObjLoader.load("pyramid.txt");
        mesh = new Mesh(meshData);


        texture = new Texture("monstrous.png", false);
        texture2 = new Texture("jackRussel.png", false);
        textureFont = new Texture("font/lsans-15.png", false);

        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(0, 1, -3);
        camera.direction.set(0,0f, 1f);
        camera.update();

        LibGPU.input.setInputProcessor(new CameraController(camera));


        modelMatrix = new Matrix4();

        batch = new SpriteBatch();

        modelBatch = new ModelBatch();

    }




    private void updateModelMatrix(float currentTime){
        Matrix4 RT = new Matrix4().setToXRotation((float) ( -0.5f*Math.PI ));
        Matrix4 R1 = new Matrix4().setToYRotation(currentTime*0.6f);
        Matrix4 T = new Matrix4().translate(0.8f, 0f, 0f);
        modelMatrix.idt().mul(R1).mul(T).mul(RT);
    }





    public void render( float deltaTime ){
        currentTime += deltaTime;


// [...] Use Render Pass

        boolean testSprites = false;
        if(testSprites) {


            // SpriteBatch testing
            batch.begin();    // todo param for now


            batch.setColor(1, 0, 0, 0.1f);
            batch.draw(texture, 0, 0, 100, 100);

            batch.draw(texture, 0, 0, 300, 300, 0.5f, 0.5f, 0.9f, 0.1f);
            batch.draw(texture, 300, 300, 50, 50);
            batch.setColor(1, 1, 1, 1);

            batch.draw(texture2, 400, 100, 100, 100);

            TextureRegion region = new TextureRegion(texture2, 0, 0, 512, 512);
            batch.draw(region, 200, 300, 64, 64);

            TextureRegion region2 = new TextureRegion(texture2, 0f, 1f, .5f, 0.5f);
            batch.draw(region2, 400, 300, 64, 64);

            int W = LibGPU.graphics.getWidth();
            int H = LibGPU.graphics.getHeight();
            batch.setColor(0, 1, 0, 1);
            for (int i = 0; i < 8000; i++) {
                batch.draw(texture2, (int) (Math.random() * W), (int) (Math.random() * H), 32, 32);
            }
            batch.end();
        }
        else {

            updateModelMatrix(currentTime);

            modelBatch.begin(camera);

            modelBatch.render(meshPart, new Material(texture2), modelMatrix);

            modelBatch.end();
        }
        // At the end of the frame


        if (System.nanoTime() - startTime > 1000000000) {
            System.out.println("SpriteBatch : fps: " + frames  );
            frames = 0;
            startTime = System.nanoTime();
        }
        frames++;

    }

    public void dispose(){
        // cleanup
        System.out.println("demo exit");
        texture.dispose();
        texture2.dispose();
        batch.dispose();
        mesh.dispose();
        modelBatch.dispose();
    }

    @Override
    public void resize(int width, int height) {
        System.out.println("demo got resize");
    }


}
