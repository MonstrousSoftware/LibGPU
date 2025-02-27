package com.monstrous;

import com.monstrous.graphics.Camera;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.PerspectiveCamera;
import com.monstrous.graphics.g3d.Model;
import com.monstrous.graphics.g3d.ModelBatch;
import com.monstrous.graphics.g3d.ModelInstance;
import com.monstrous.graphics.lights.DirectionalLight;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;

import java.util.ArrayList;

// Test instancing
// For comparison to DuckField demo but is lacking shadows, PBR shading,etc.

public class TestDuckField extends ApplicationAdapter {

    private ModelBatch modelBatch;
    private Camera camera;
    private CameraController camController;
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

        model = new Model("models/Ducky/ducky.gltf");
        modelInstances = new ArrayList<>();

        transforms = makeTransforms();
        ModelInstance modelInstance = new ModelInstance(model, transforms);
        modelInstances.add(modelInstance);

        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(0, 1, -3);
        camera.direction.set(0,0f, 1f);
        camera.far = 1000f;
        camera.update();

        environment = new Environment();
        environment.add( new DirectionalLight( Color.WHITE, new Vector3(0,-1,0)));
        environment.ambientLightLevel = 0.4f;

        camController = new CameraController(camera);
        LibGPU.input.setInputProcessor(camController);

        modelBatch = new ModelBatch();
    }

    private ArrayList<Matrix4> makeTransforms(){
        ArrayList<Matrix4> transforms = new ArrayList<>();
        float N = 25;
        float x = -N;

        for(int i = 0; i < N; i++, x += 2f) {
            float z = -N;
            for (int j = 0; j < N; j++, z += 2f) {
                transforms.add(new Matrix4().translate(x, 0, z).scale(new Vector3( 1f,1f+.3f*(float)Math.sin(x-z), 1f)).rotate(up, 30f * x + 20f * z));
            }
        }
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
        camController.update();

        rotate(transforms, LibGPU.graphics.getDeltaTime());



        modelBatch.begin(camera, environment, Color.GRAY);
        modelBatch.render(modelInstances);
        modelBatch.end();

        // At the end of the frame
        if (System.nanoTime() - startTime > 1000000000) {
            System.out.println("SpriteBatch : fps: " + frames +" instances: "+ transforms.size() );
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

