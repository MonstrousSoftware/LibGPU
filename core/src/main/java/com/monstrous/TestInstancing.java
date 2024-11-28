package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.lights.DirectionalLight;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;
import com.monstrous.utils.ScreenUtils;

import java.util.ArrayList;

// Test instancing
// Shows a model instance with instancing and one without

public class TestInstancing extends ApplicationAdapter {

    private final float  FIELD_SIZE = 1f;

    private ModelBatch modelBatch;
    private Camera camera;
    private Model model;
    private ArrayList<ModelInstance> modelInstances;
    private Environment environment;
    private ArrayList<Matrix4> transforms;
    private long startTime;
    private int frames;
    private final Vector3 up = new Vector3(0,1,0);

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        model = new Model("models/Ducky.obj");
        //model = new Model("models/ToyCar/ToyCar.gltf");
        modelInstances = new ArrayList<>();

        transforms = makeTransforms();
        ModelInstance modelInstance = new ModelInstance(model, transforms);
        modelInstances.add(modelInstance);


        //modelInstances.add(new ModelInstance(new Model("models/pyramid.obj"), 0, 0,0));



//        ArrayList<Matrix4> transforms = makeTransforms();
//        for(Matrix4 transform: transforms)
//            modelInstances.add(new ModelInstance(model, transform));



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
        float x = 0;
        for(int i = 0; i < 10; i++, x += 3f)
            transforms.add(new Matrix4().translate(x, 0, 0).rotate(up, 30f*x));
        System.out.println("Instances: "+transforms.size());
        return transforms;
    }



    private void rotate(ArrayList<Matrix4> transforms, float deltaTime){
        for(Matrix4 transform : transforms)
            transform.rotate(up, 30f*deltaTime);
    }



    public void render( ){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE))
            LibGPU.app.exit();

        rotate(transforms, LibGPU.graphics.getDeltaTime());

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

