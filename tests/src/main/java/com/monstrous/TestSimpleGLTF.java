package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.g3d.Model;
import com.monstrous.graphics.g3d.ModelBatch;
import com.monstrous.graphics.g3d.ModelInstance;
import com.monstrous.graphics.g3d.SkyBox;
import com.monstrous.graphics.g3d.ibl.HDRLoader;
import com.monstrous.graphics.g3d.ibl.ImageBasedLighting;
import com.monstrous.graphics.lights.DirectionalLight;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;

import java.io.IOException;
import java.util.ArrayList;

public class TestSimpleGLTF extends ApplicationAdapter {

    private ModelBatch modelBatch;
    private Camera camera;
    private Environment environment;
    private Matrix4 modelMatrix;
    private Model model;
    private ModelInstance modelInstance1;
    private ArrayList<ModelInstance> instances;
    private float currentTime;
    private long startTime;
    private int frames;
    private SpriteBatch batch;
    private BitmapFont font;
    private String status;
    private CameraController camController;
    private Texture textureEquirectangular;

    public void create() {
        instances = new ArrayList<>();

        model = new Model("models/AnimatedCube/glTF/AnimatedCube.gltf");


        modelMatrix = new Matrix4();
        modelInstance1 = new ModelInstance(model, modelMatrix);
        instances.add(modelInstance1);


        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(3, 1f, -3);
        camera.direction.set(camera.position).scl(-1);
        camera.far = 200f;
        camera.near = 0.001f;
        camera.update();

        environment = new Environment();

        environment.add( new DirectionalLight( new Color(1,1,1,1), new Vector3(0,-1,0)));
        environment.ambientLightLevel = 0.5f;

        camController = new CameraController(camera);
        LibGPU.input.setInputProcessor( camController );

        modelBatch = new ModelBatch();

        batch = new SpriteBatch();
        font = new BitmapFont();
    }




    public void render(){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){
            LibGPU.app.exit();
        }
        currentTime += LibGPU.graphics.getDeltaTime();

        camController.update();

        modelBatch.begin(camera, environment, Color.GRAY);
        modelBatch.render(instances);
        modelBatch.end();
    }

    public void dispose(){
        // cleanup
        model.dispose();
        modelBatch.dispose();
        font.dispose();
        batch.dispose();
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }


}

