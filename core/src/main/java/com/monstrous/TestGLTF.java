package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g3d.Model;
import com.monstrous.graphics.g3d.ModelBatch;
import com.monstrous.graphics.g3d.ModelInstance;
import com.monstrous.graphics.lights.DirectionalLight;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;
import com.monstrous.utils.ScreenUtils;

import java.util.ArrayList;

public class TestGLTF extends ApplicationAdapter {

    private ModelBatch modelBatch;
    private Camera camera;
    private Environment environment;
    private Matrix4 modelMatrix;
    private Model model, model2;
    private ModelInstance modelInstance1;
    private ModelInstance modelInstance2;
    private ArrayList<ModelInstance> instances;
    private float currentTime;
    private long startTime;
    private int frames;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        instances = new ArrayList<>();

        //model = new Model("models/lantern/Lantern.gltf");
        //model = new Model("models/ToyCar/ToyCar.gltf");
        //model = new Model("models/Buggy/Buggy.gltf");
        model = new Model("models/DamagedHelmet/DamagedHelmet.gltf");
        //model = new Model("models/Sponza/Sponza.gltf");
        //model = new Model("models/fourareen.obj");


        modelMatrix = new Matrix4();
        modelInstance1 = new ModelInstance(model, modelMatrix);
        instances.add(modelInstance1);

//        for(int x = -30; x < 30; x += 5){
//            for(int z = -30; z < 30; z += 5){
//                if(x != 0 && z != 0)
//                    instances.add( new ModelInstance(model, x, 0, z));
//            }
//        }

        //modelInstance2 = new ModelInstance(model2, 5,0,0);


        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(0, 1f, -3);
        camera.direction.set(0,0f, 1f);
        camera.far = 1000f;
        camera.near = 0.001f;
        camera.update();

        environment = new Environment();
        environment.add( new DirectionalLight( new Color(1,1,1,1), new Vector3(0,-1,0)));
        environment.ambientLightLevel = 0.5f;


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

        modelBatch.begin(camera, environment);
        modelBatch.render(instances);
        modelBatch.end();

        // At the end of the frame

        if (System.nanoTime() - startTime > 1000000000) {
            System.out.println("fps: " + frames +
                    " GPU: "+(int)LibGPU.app.getAverageGPUtime()+" microseconds"  );
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
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }


}

