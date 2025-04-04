package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g3d.Model;
import com.monstrous.graphics.g3d.ModelBatch;
import com.monstrous.graphics.g3d.ModelInstance;
import com.monstrous.math.Matrix4;
import com.monstrous.utils.ScreenUtils;

import java.util.ArrayList;


// test that we can render multiple models efficiently, i.e. without too many material or pipeline switches.

public class TestRenderSwitching extends ApplicationAdapter {

    private ModelBatch modelBatch;
    private Camera camera;
    private Matrix4 modelMatrix, modelMatrix2;
    private Model model, model2;
    private ArrayList<ModelInstance> instances;
    private float currentTime;
    private long startTime;
    private int frames;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        model = new Model("models/cubes.gltf");

        model2 = new Model("models/fourareen.obj");

        modelMatrix = new Matrix4();
        instances = new ArrayList<>();
        instances.add( new ModelInstance(model, 0,0,0) );
        instances.add( new ModelInstance(model2, 15,0,0) );
        instances.add( new ModelInstance(model, 15,0,15) );
        instances.add( new ModelInstance(model2, 30,0,15) );
        instances.add( new ModelInstance(model, 10,0,0) );
        instances.add( new ModelInstance(model2, 2,0,3) );
        instances.add( new ModelInstance(model, 0,10,0) );
        instances.add( new ModelInstance(model2, 15,10,0) );
        instances.add( new ModelInstance(model, 15,10,15) );
        instances.add( new ModelInstance(model2, 30,10,15) );
        instances.add( new ModelInstance(model, 10,10,0) );
        instances.add( new ModelInstance(model2, 2,10,3) );

        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(0, 0.5f, -6);
        camera.direction.set(0,0f, 1f);
        camera.far = 1000f;

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





    public void render(){
        currentTime += LibGPU.graphics.getDeltaTime();
        ScreenUtils.clear(.7f, .7f, .7f, 1);

        updateModelMatrix(modelMatrix, currentTime);

        modelBatch.begin(camera);

        modelBatch.render(instances);

        modelBatch.end();

        // At the end of the frame

        if (System.nanoTime() - startTime > 1000000000) {
            System.out.println("fps: " + frames  );
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

