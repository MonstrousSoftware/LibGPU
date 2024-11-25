package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;
import com.monstrous.utils.ScreenUtils;

import java.util.ArrayList;

// Test instancing

public class TestInstancing extends ApplicationAdapter {

    private final float  FIELD_SIZE = 10f;

    private ModelBatch modelBatch;
    private Camera camera;
    private Model model;
    private ArrayList<ModelInstance> modelInstances;
    private Environment environment;
    private long startTime;
    private int frames;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        model = new Model("models/ducky.obj");
        modelInstances = new ArrayList<>();

        //modelInstances.add(new ModelInstance(new Model("models/pyramid.obj"), 0, 0,0));

        ModelInstance modelInstance = new ModelInstance(model, 0, 0,0);
        modelInstance.instanceTransforms = makeTransforms();;
        modelInstances.add(modelInstance);

        modelInstances.add(new ModelInstance(new Model("models/pyramid.obj"), 0, 3,0));


        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(0, 1, -3);
        camera.direction.set(0,0f, 1f);
        camera.far = 1000f;
        camera.update();

        environment = new Environment();
        environment.add( new DirectionalLight( new Color(1,1,1,1), new Vector3(0,-1,0)));

        LibGPU.input.setInputProcessor(new CameraController(camera));

        modelBatch = new ModelBatch();
    }

    private ArrayList<Matrix4> makeTransforms(){
        ArrayList<Matrix4> transforms = new ArrayList<>();
        for (float x = -FIELD_SIZE; x < FIELD_SIZE; x += 2) {
            for (float z = -FIELD_SIZE; z < FIELD_SIZE; z += 2) {
                Matrix4 transform = new Matrix4().translate(x, 0, z);
                transforms.add(transform);
            }
        }
        System.out.println("Instances: "+transforms.size());
        return transforms;
    }



    public void render( ){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE))
            LibGPU.app.exit();


        ScreenUtils.clear(.7f, .7f, .7f, 1);

        modelBatch.begin(camera, environment);
        modelBatch.render(modelInstances);
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
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }


}

