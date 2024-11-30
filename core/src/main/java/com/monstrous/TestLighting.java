package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g3d.Model;
import com.monstrous.graphics.g3d.ModelBatch;
import com.monstrous.graphics.g3d.ModelInstance;
import com.monstrous.graphics.lights.DirectionalLight;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.graphics.lights.PointLight;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;

import java.util.ArrayList;

// shows a pyramid model with a directional light on it

public class TestLighting extends ApplicationAdapter {

    private ModelBatch modelBatch;
    private Camera camera;
    //private Matrix4 modelMatrix;
    private Model model, model2;
    private ArrayList<ModelInstance> instances;
    private Environment environment;
    private float currentTime;
    private long startTime;
    private int frames;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        instances = new ArrayList<>();
        model = new Model("models/groundplane.gltf");
        ModelInstance instance = new ModelInstance(model, 0,-0.5f, 0);
        instances.add(instance);
        model2 = new Model("models/sphere.gltf");
        ModelInstance instance2 = new ModelInstance(model2, 0, 0.3f, 0);
        instance2.instanceTransforms.get(0).scale(0.5f);
        instances.add(instance2);

        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(0, 1, -3);
        camera.direction.set(0,0f, 1f);
        camera.update();

        environment = new Environment();
        environment.add( new DirectionalLight(new Color(1,1,1,1), new Vector3(.3f,-.7f,0)));
//
//        environment.add( new DirectionalLight(Color.BLUE, new Vector3(.7f,-.2f,0)));
//        environment.add( new DirectionalLight(Color.RED, new Vector3(0f,1f,0)));

//        environment.add( new PointLight(new Color(1,0,0,1), new Vector3(3f,1f,3), 15f));
//        environment.add( new PointLight(new Color(1,0,1,1), new Vector3(-3f,1f,3), 5f));
        environment.ambientLightLevel = 0.2f;


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
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE))
            LibGPU.app.exit();

        if(LibGPU.input.isKeyPressed(Input.Keys.L)){
            modelBatch.loadShaders();
        }
        //currentTime += LibGPU.graphics.getDeltaTime();

        //updateModelMatrix(modelMatrix, currentTime);

        modelBatch.begin(camera, environment);
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
        model2.dispose();
        modelBatch.dispose();
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }


}

