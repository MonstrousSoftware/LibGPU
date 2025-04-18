package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.g3d.*;
import com.monstrous.graphics.lights.DirectionalLight;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;

import java.util.ArrayList;

public class TestAnimatedGLTF extends ApplicationAdapter {

    private ModelBatch modelBatch;
    private Camera camera;
    private Environment environment;
    private Matrix4 modelMatrix;
    private Model model;
    private ModelInstance modelInstance1;
    private ArrayList<ModelInstance> instances;
    private SpriteBatch batch;
    private BitmapFont font;
    private CameraController camController;
    private AnimationController animController;

    public void create() {
        instances = new ArrayList<>();

//        model = new Model("models/AnimatedCube/glTF/AnimatedCube.gltf");
//        String animationId = "animation_AnimatedCube";
        model = new Model("models/BoxAnimated/glTF/BoxAnimated.gltf");
        String animationId = "CubeAction";

        modelMatrix = new Matrix4();
        modelInstance1 = new ModelInstance(model, modelMatrix);
        instances.add(modelInstance1);

        animController = new AnimationController(modelInstance1);
        animController.setAnimation(animationId, -1);


        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(3, 1f, -3);
        camera.direction.set(camera.position).scl(-1);
        camera.far = 200f;
        camera.near = 0.001f;
        camera.update();

        environment = new Environment();

        DirectionalLight sun = new DirectionalLight( Color.WHITE, new Vector3(.6f,-1,.3f).nor());
        sun.setIntensity(3f);
        environment.add( sun );
        environment.ambientLightLevel = 0.7f;

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
        float deltaTime =  LibGPU.graphics.getDeltaTime();
        if(LibGPU.input.isKeyPressed(Input.Keys.SPACE)){
            deltaTime =  0.f;
        }

        AnimationController.AnimationDesc desc = animController.update(deltaTime);
        camController.update();

        modelBatch.begin(camera, environment, Color.GRAY);
        modelBatch.render(instances);
        modelBatch.end();

//        batch.begin();
//        if(desc != null) {
//            font.draw(batch, "Animation name: "+desc.animation.name, 10, 70);
//            font.draw(batch, "time: " + desc.time, 10, 50);
//        }
//        batch.end();
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

