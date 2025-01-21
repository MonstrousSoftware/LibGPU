package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.g3d.Model;
import com.monstrous.graphics.g3d.ModelBatch;
import com.monstrous.graphics.g3d.ModelInstance;
import com.monstrous.graphics.lights.DirectionalLight;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;
import com.monstrous.utils.ScreenUtils;
import com.monstrous.wgpu.WGPUTextureFormat;

import java.util.ArrayList;

public class TestPostProcessing extends ApplicationAdapter {

    private ModelBatch modelBatch;
    private Camera camera;
    private Environment environment;
    private Matrix4 modelMatrix;
    private Model model, model2;
    private ArrayList<ModelInstance> instances;
    private float currentTime;
    private long startTime;
    private int frames;
    private Texture colorMap;
    private SpriteBatch batch;
    private ShaderProgram filter;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        instances = new ArrayList<>();

        model = new Model("models/stanfordDragon.gltf");

        modelMatrix = new Matrix4();
        ModelInstance modelInstance1 = new ModelInstance(model, modelMatrix);
        instances.add(modelInstance1);

        model2 = new Model("models/groundplane.gltf");
        ModelInstance modelInstance2 = new ModelInstance(model2, 0, 0, 0);
        instances.add(modelInstance2);


        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(-5, 3.5f, -0.5f);
        camera.direction.set(0.8f,-0.6f, 0f);
        camera.far = 100f;
        camera.near = 0.1f;
        camera.update();

        environment = new Environment();
        environment.add( new DirectionalLight( new Color(1,1,1,1), new Vector3(0,-1,0)));


        LibGPU.input.setInputProcessor(new CameraController(camera));

        colorMap = new Texture(LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight(), false, true, WGPUTextureFormat.RGBA8Unorm);

        modelBatch = new ModelBatch();

        batch = new SpriteBatch();

        // post-processing shader
        filter = new ShaderProgram("shaders/sprite-greyscale.wgsl");
    }

    private void updateModelMatrix(Matrix4 modelMatrix, float currentTime){
        Matrix4 RT = new Matrix4().idt(); //setToXRotation((float) ( -0.5f*Math.PI ));
        Matrix4 R1 = new Matrix4().setToYRotation(currentTime*0.3f);
        Matrix4 T = new Matrix4(); //.translate(1.8f, 0f, 0f);
        modelMatrix.idt().mul(R1).mul(T).mul(RT);
    }

    public void render(){
        currentTime += LibGPU.graphics.getDeltaTime();

        updateModelMatrix(modelMatrix, currentTime);

        ScreenUtils.clear(Color.WHITE);

        // render 3d scene to texture
        modelBatch.begin(camera, environment,Color.WHITE, colorMap,  null);
        modelBatch.render(instances);
        modelBatch.end();

        // put texture on screen twice side to side: left without and right with the post-processing shader
        float W = LibGPU.graphics.getWidth();
        float H = LibGPU.graphics.getHeight();

        ScreenUtils.clear(Color.BLUE);
        batch.begin();
        batch.draw(colorMap,10,10, W/2-20, H-20);

        batch.setShader(filter);    // use the post-processing shader
        batch.draw(colorMap,W/2+10, 10, W/2-20, H-20);
        batch.end();


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

