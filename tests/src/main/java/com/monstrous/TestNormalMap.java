package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g3d.Model;
import com.monstrous.graphics.g3d.ModelBatch;
import com.monstrous.graphics.g3d.ModelInstance;
import com.monstrous.graphics.lights.DirectionalLight;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;

public class TestNormalMap extends ApplicationAdapter {

    private ModelBatch modelBatch;
    private Camera camera;
    private Matrix4 modelMatrix;
    private Model model;
    private ModelInstance modelInstance1;
    private Environment environment;
    private float currentTime;
    private long startTime;
    private int frames;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        model = new Model("models/fourareen.obj");

        modelMatrix = new Matrix4();
        modelInstance1 = new ModelInstance(model, modelMatrix);

        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(0, 0.5f, -3);
        camera.direction.set(0,0f, 1f);
        camera.update();

        environment = new Environment();
        environment.add( new DirectionalLight(new Color(1,1,1,1), new Vector3(.3f,-.7f,0)));
        environment.ambientLightLevel = 1.0f;


        LibGPU.input.setInputProcessor(new CameraController(camera));

        modelBatch = new ModelBatch();
    }




    private void updateModelMatrix(Matrix4 modelMatrix, float currentTime){
        Matrix4 RT = new Matrix4().idt(); //setToXRotation((float) ( -0.5f*Math.PI ));
        Matrix4 R1 = new Matrix4().setToYRotation(currentTime*0.3f);
        Matrix4 T = new Matrix4(); //.translate(1.8f, 0f, 0f);
        modelMatrix.idt().mul(R1).mul(T).mul(RT);
    }





    public void render(  ){
        //currentTime += deltaTime;

        updateModelMatrix(modelMatrix, currentTime);

        modelBatch.begin(camera, environment);

        modelBatch.render(modelInstance1);

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

