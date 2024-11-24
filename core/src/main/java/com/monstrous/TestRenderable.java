package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.loaders.MeshData;
import com.monstrous.graphics.loaders.ObjLoader;
import com.monstrous.math.Matrix4;


public class TestRenderable extends ApplicationAdapter {

    private ModelBatch modelBatch;
    private Camera camera;
    private Texture texture;
    private Texture texture2;
    private Mesh mesh;
    private MeshPart meshPart;
    private Matrix4 modelMatrix;
    private Matrix4 modelMatrix2;
    private Renderable renderable;
    private Renderable renderable2;
    private float currentTime;
    private long startTime;
    private int frames;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;


        //mesh = new Mesh("pyramidNoIndex.txt");
        MeshData meshData = ObjLoader.load("models/ducky.obj");
        mesh = new Mesh(meshData);

        if(mesh.getIndexCount() > 0)
            meshPart = new MeshPart(mesh, 0, mesh.getIndexCount());
        else
            meshPart = new MeshPart(mesh, 0, mesh.getVertexCount());
        texture = new Texture("textures/palette.png", false);
        texture2 = new Texture("textures/jackRussel.png", false);


        modelMatrix = new Matrix4();
        renderable = new Renderable(meshPart, new Material(texture2), modelMatrix);

        modelMatrix2 = new Matrix4();
        renderable2 = new Renderable(meshPart, new Material(texture), modelMatrix2);

        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(0, 1, -3);
        camera.direction.set(0,0f, 1f);
        camera.update();

        LibGPU.input.setInputProcessor(new CameraController(camera));


        modelBatch = new ModelBatch();
    }




    private void updateModelMatrix(Matrix4 modelMatrix, float currentTime){
        Matrix4 RT = new Matrix4().idt(); //setToXRotation((float) ( -0.5f*Math.PI ));
        Matrix4 R1 = new Matrix4().setToYRotation(currentTime);
        Matrix4 T = new Matrix4().translate(1.8f, 0f, 0f);
        modelMatrix.idt().mul(R1).mul(T).mul(RT);
    }





    public void render( float deltaTime ){
        currentTime += deltaTime;

        updateModelMatrix(modelMatrix, currentTime);
        updateModelMatrix(modelMatrix2, currentTime+3.14f);

        modelBatch.begin(camera);

        //modelBatch.render(meshPart, texture2, modelMatrix);

        modelBatch.render(renderable);
        modelBatch.render(renderable2);

        modelBatch.end();

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
        mesh.dispose();
        modelBatch.dispose();
    }

    @Override
    public void resize(int width, int height) {
        System.out.println("demo got resize");
    }


}
