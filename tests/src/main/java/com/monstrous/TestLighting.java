package com.monstrous;

import com.monstrous.graphics.Camera;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.Material;
import com.monstrous.graphics.PerspectiveCamera;
import com.monstrous.graphics.g3d.Model;
import com.monstrous.graphics.g3d.ModelBatch;
import com.monstrous.graphics.g3d.ModelInstance;
import com.monstrous.graphics.g3d.shapeBuilder.BoxShapeBuilder;
import com.monstrous.graphics.g3d.shapeBuilder.SphereShapeBuilder;
import com.monstrous.graphics.lights.DirectionalLight;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.graphics.lights.PointLight;
import com.monstrous.math.Vector3;
import com.monstrous.utils.Disposable;

import java.util.ArrayList;

// shows a sphere on a ground plane reflecting different directional and point lights.

public class TestLighting extends ApplicationAdapter {

    private ModelBatch modelBatch;
    private Camera camera;
    private Model modelGround, modelSphere;
    private ArrayList<Disposable> disposables;
    private ArrayList<ModelInstance> instances;
    private Environment environment;
    private long startTime;
    private int frames;
    private CameraController camController;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        disposables = new ArrayList<>();
        instances = new ArrayList<>();
        Model modelGround = new Model(BoxShapeBuilder.build(20, 0.1f, 20), new Material(new Color(0x85E7A0)));
        disposables.add(modelGround);
        ModelInstance instance = new ModelInstance(modelGround, 0,0.0f, 0);
        instances.add(instance);

        Material mat = new Material(Color.WHITE);
        mat.metallicFactor = 0.5f;
        mat.roughnessFactor = 0.45f;

        modelSphere = new Model(SphereShapeBuilder.build(.5f, 64),mat);
        disposables.add(modelGround);
        ModelInstance instance3 = new ModelInstance(modelSphere, 0, 0.5f, 0);
        instances.add(instance3);

        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(0, 0, -3);
        camera.direction.set(0,0f, 1f);
        camera.update();

        environment = new Environment();
        DirectionalLight dir1 = new DirectionalLight(new Color(1,1,1,1), new Vector3(.3f,-.7f,0));
        dir1.setIntensity(2f);
        environment.add( dir1 );
        Model lightD1 = new Model(SphereShapeBuilder.build(0.2f, 16), new Material(Color.WHITE));
        disposables.add(lightD1);
        instances.add(new ModelInstance(lightD1, -3, 7, 0));

        DirectionalLight dir2 = new DirectionalLight(new Color(1,1,1,1), new Vector3(-.3f,-.5f,.3f));
        dir2.setIntensity(1f);
        environment.add( dir2 );
        Model lightD2 = new Model(SphereShapeBuilder.build(0.2f, 16), new Material(Color.WHITE));
        disposables.add(lightD2);
        instances.add(new ModelInstance(lightD2, 3, 5, -3));

        environment.add( new DirectionalLight(Color.BLUE, new Vector3(.7f,-.2f,0)));
        Model lightD3 = new Model(SphereShapeBuilder.build(0.2f, 16), new Material(Color.BLUE));
        disposables.add(lightD3);
        instances.add(new ModelInstance(lightD3, -7, 2, 0));

        environment.add( new DirectionalLight(Color.RED, new Vector3(0f,-1f,0)));
        Model lightD4 = new Model(SphereShapeBuilder.build(0.2f, 16), new Material(Color.RED));
        disposables.add(lightD4);
        instances.add(new ModelInstance(lightD4, 0, 10, 0));

        environment.add( new PointLight(Color.RED, new Vector3(3f,1f,3), 15f));
        Model light = new Model(SphereShapeBuilder.build(0.2f, 16), new Material(Color.RED));
        disposables.add(light);
        instances.add(new ModelInstance(light, 3f, 1f, 3f));

        environment.add( new PointLight(new Color(1,0,1,1), new Vector3(-3f,1f,3), 5f));
        Model light2 = new Model(SphereShapeBuilder.build(0.2f, 16), new Material(new Color(1, 0, 1, 1)));
        disposables.add(light2);
        instances.add(new ModelInstance(light2, -3f, 1f, 3f));

        environment.ambientLightLevel = 0.0f;


        camController = new CameraController(camera);
        LibGPU.input.setInputProcessor(camController);

        modelBatch = new ModelBatch();
        disposables.add(modelBatch);
    }


    public void render(){
        long frameStart = System.nanoTime();

        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)) {
            LibGPU.app.exit();
            return;
        }


        camController.update();

        modelBatch.begin(camera, environment);
        modelBatch.render(instances);
        modelBatch.end();

        // At the end of the frame

        long elapsedNanos = System.nanoTime() - frameStart;
        if (System.nanoTime() - startTime > 1000000000) {
            System.out.print("fps: " + frames  );
            frames = 0;
            startTime = System.nanoTime();

            System.out.println(" render time (microseconds): " + elapsedNanos/1000  );
        }
        frames++;



    }

    public void dispose(){
        // cleanup
        for(Disposable disposable : disposables)
            disposable.dispose();
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }


}

