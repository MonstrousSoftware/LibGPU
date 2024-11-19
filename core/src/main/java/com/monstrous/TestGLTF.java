package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.math.Matrix4;
import com.monstrous.utils.ScreenUtils;

public class TestGLTF implements ApplicationListener {

    private ModelBatch modelBatch;
    private Camera camera;
    private Matrix4 modelMatrix, modelMatrix2;
    private Model model, model2;
    private ModelInstance modelInstance1, modelInstance2;
    private float currentTime;
    private long startTime;
    private int frames;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        model = new Model("models/lantern/Lantern.gltf");

        //model2 = new Model("models/fourareen.obj");

        modelMatrix = new Matrix4();
        modelInstance1 = new ModelInstance(model, modelMatrix);

//        modelMatrix2 = new Matrix4();
//        modelInstance2 = new ModelInstance(model2, modelMatrix2);

        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(0, 0.5f, -6);
        camera.direction.set(0,0f, 1f);
        camera.update();

        LibGPU.input.setInputProcessor(new CameraController(camera));

        modelBatch = new ModelBatch();
    }




    private void updateModelMatrix(Matrix4 modelMatrix, float currentTime){
        Matrix4 RT = new Matrix4().idt(); //setToXRotation((float) ( -0.5f*Math.PI ));
        Matrix4 R1 = new Matrix4().setToYRotation(currentTime*0.3f);
        Matrix4 T = new Matrix4(); //.translate(1.8f, 0f, 0f);
        modelMatrix.idt().mul(R1).mul(T).mul(RT);
    }





    public void render( float deltaTime ){
        currentTime += deltaTime;
        ScreenUtils.clear(.7f, .7f, .7f, 1);

        updateModelMatrix(modelMatrix, currentTime);

        modelBatch.begin(camera);

        modelBatch.render(modelInstance1);
        //modelBatch.render(modelInstance2);

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
        model.dispose();
        modelBatch.dispose();
    }

    @Override
    public void resize(int width, int height) {
        System.out.println("demo got resize");
    }


}

