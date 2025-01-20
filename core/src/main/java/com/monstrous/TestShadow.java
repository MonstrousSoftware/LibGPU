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

public class TestShadow extends ApplicationAdapter {

    private static int SHADOW_MAP_SIZE = 4096;      // size (in pixels) of depth map
    private static int SHADOW_VIEWPORT_SIZE = 25;   // area (in world units) covered by shadow

    private ModelBatch modelBatch;
    private Camera camera;
    private CameraController camController;
    private OrthographicCamera shadowCam;
    private Environment environment;
    private Matrix4 modelMatrix;
    private Model model, model2;
    private ModelInstance modelInstance1;
    private ModelInstance modelInstance2;
    private ArrayList<ModelInstance> instances;
    private float currentTime;
    private long startTime;
    private int frames;
    private Texture colorMap, depthMap;
    private SpriteBatch batch;
    private ShaderProgram filter;
    private BitmapFont font;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        instances = new ArrayList<>();

        //model = new Model("models/stanfordDragon.gltf");
        model = new Model("models/torus.gltf");

        modelMatrix = new Matrix4();
        modelInstance1 = new ModelInstance(model, modelMatrix);
        instances.add(modelInstance1);

        model2 = new Model("models/groundplane.gltf");
        modelInstance2 = new ModelInstance(model2, 0,0,0);
        instances.add(modelInstance2);


        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(-0, 3.5f, -5f);
        camera.direction.set(0f,-0.3f, 0.8f);

        camera.position.set(0, 6, 0);
        camera.direction.set(0, -1, 0);

        camera.far = 50f;
        camera.near = 0.1f;
        camera.update();

        // unit vector from main light source
        Vector3 lightDirection = new Vector3(0.3f, -1f, .3f).nor();

        shadowCam = new OrthographicCamera(SHADOW_VIEWPORT_SIZE, SHADOW_VIEWPORT_SIZE); // in world units

        shadowCam.direction.set(lightDirection);
        shadowCam.position.set(lightDirection).scl(-6);
        shadowCam.up.set(0,0,1);
        shadowCam.near = 0f;
        shadowCam.far = 20f;
        shadowCam.zoom = 1f;
        shadowCam.update();

        colorMap = new Texture(SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, false, true, WGPUTextureFormat.RGBA8Unorm);
        depthMap = new Texture(SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, false, true, WGPUTextureFormat.Depth32Float);


        environment = new Environment();
        DirectionalLight sun = new DirectionalLight( Color.WHITE, lightDirection);
        sun.setIntensity(4f);
        environment.add( sun );
        environment.setShadowMap(shadowCam, depthMap);
        environment.ambientLightLevel = 0.5f;

        camController = new CameraController(camera);
        LibGPU.input.setInputProcessor(camController);


        modelBatch = new ModelBatch();

        batch = new SpriteBatch();
        font = new BitmapFont();

        filter = new ShaderProgram("shaders/sprite-greyscale.wgsl");
    }




    private void updateModelMatrix(Matrix4 modelMatrix, float currentTime){
        Matrix4 RT = new Matrix4().idt(); //setToXRotation((float) ( -0.5f*Math.PI ));
        Matrix4 R1 = new Matrix4().setToYRotation(currentTime*0.3f);
        Matrix4 T = new Matrix4();//.translate(0f, 1f, 0f);
        modelMatrix.idt().mul(R1).mul(T).mul(RT);
    }





    public void render(){
        if(LibGPU.input.isKeyPressed(Input.Keys.L)){    // to force shaders to be recompiled
            modelBatch.invalidatePipelines();
        }

        currentTime += LibGPU.graphics.getDeltaTime();
        ScreenUtils.clear(Color.GRAY);

        updateModelMatrix(modelMatrix, currentTime);
        camController.update();

        // pass #1 : depth map
        environment.depthPass = true;
        environment.renderShadows = false;
        environment.setShadowMap(shadowCam, null);

        modelBatch.begin(shadowCam, environment, colorMap, depthMap);
        modelBatch.render(instances);
        modelBatch.end();

        // pass #2 : render colours
        environment.depthPass = false;
        environment.renderShadows = true;

        environment.setShadowMap(shadowCam, depthMap);
        modelBatch.begin(camera, environment);
        modelBatch.render(instances);
        modelBatch.end();

//        ScreenUtils.clear(Color.WHITE);
//        batch.begin();
//        batch.draw(colorMap,0,0, 500, 500);
//        batch.setShader(filter);
//        batch.draw(colorMap,LibGPU.graphics.getWidth()/2f, 0, 500, 500);
//        batch.end();

        batch.begin();
        font.draw(batch, "camera "+shadowCam.position.toString()+" angleX:"+camController.anglex, 10, 500);
        batch.draw(colorMap,0, 0, 200, 200);
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
        modelBatch.dispose();
        font.dispose();
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }


}

