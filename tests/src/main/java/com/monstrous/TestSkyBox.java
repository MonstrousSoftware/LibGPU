package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.g3d.Model;
import com.monstrous.graphics.g3d.ModelBatch;
import com.monstrous.graphics.g3d.ModelInstance;
import com.monstrous.graphics.g3d.SkyBox;
import com.monstrous.graphics.lights.DirectionalLight;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;
import com.monstrous.utils.ScreenUtils;
import com.monstrous.webgpu.WGPUTextureFormat;

import java.util.ArrayList;

public class TestSkyBox extends ApplicationAdapter {

    private static int SHADOW_MAP_SIZE = 4096;      // size (in pixels) of depth map
    private static int SHADOW_VIEWPORT_SIZE = 25;   // area (in world units) covered by shadow

    private ModelBatch modelBatch;
    private Camera camera;
    private CameraController camController;
    private OrthographicCamera shadowCam;
    private Environment environment;
    private Matrix4 modelMatrix;
    private Model model, model2, model3;
    private ModelInstance modelInstance1;
    private ModelInstance modelInstance2;
    private ArrayList<ModelInstance> instances;
    private float currentTime;
    private long startTime;
    private int frames;
    private SpriteBatch batch;
    private BitmapFont font;
    private Texture cubeMap;
    private SkyBox skybox;
    private int fps;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        instances = new ArrayList<>();

        //model = new Model("models/stanfordDragon.gltf");
        model = new Model("models/Cube.gltf");
//
//        modelMatrix = new Matrix4();
//        Matrix4 modelMatrix2 = new Matrix4();
//        modelMatrix2.setToTranslation(5, 0, 0);
//        ArrayList<Matrix4> matrices = new ArrayList<>();
//        matrices.add(modelMatrix);
//        matrices.add(modelMatrix2);
//        modelInstance1 = new ModelInstance(model, matrices);
//        instances.add(modelInstance1);

//        model2 = new Model("models/groundplane.gltf");
//        modelInstance2 = new ModelInstance(model2, 0,0,0);
//        //instances.add(modelInstance2);

//        model3 = new Model("models/waterbottle/WaterBottle.gltf");
//        ModelInstance modelInstance3 = new ModelInstance(model3, 0,1,0);
//        instances.add(modelInstance3);



        // the order of the layers is +X, -X, +Y, -Y, +Z, -Z
        String[] fileNames = {
                "textures/leadenhall/pos-x.jpg",
                "textures/leadenhall/neg-x.jpg",
                "textures/leadenhall/pos-y.jpg",
                "textures/leadenhall/neg-y.jpg",
                "textures/leadenhall/pos-z.jpg",
                "textures/leadenhall/neg-z.jpg",
//                "textures/daysky/environment_posx.jpg",
//                 "textures/daysky/environment_negx.jpg",
//                "textures/daysky/environment_posy.jpg",
//                "textures/daysky/environment_negy.jpg",
//                "textures/daysky/environment_posz.jpg",
//                "textures/daysky/environment_negz.jpg"
        };

        cubeMap = new Texture(fileNames, true, WGPUTextureFormat.RGBA8Unorm);       // format should be taken from the image files....


        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());

        camera.position.set(0, 6, 0);
        //camera.direction.set(0, -1, 0.0001f);
        camera.direction.set(0, 0, 1);
        camera.up.set(0,1,0);

        camera.far = 50f;
        camera.near = 0.1f;
        camera.update();

        // unit vector from main light source
        Vector3 lightDirection = new Vector3(1.3f, -1f, .3f).nor();

        shadowCam = new OrthographicCamera(SHADOW_VIEWPORT_SIZE, SHADOW_VIEWPORT_SIZE); // in world units

        shadowCam.direction.set(lightDirection);
        shadowCam.position.set(lightDirection).scl(-6);
        shadowCam.up.set(0,0,1);
        shadowCam.near = 0f;
        shadowCam.far = 20f;
        shadowCam.zoom = 1f;
        shadowCam.update();



        environment = new Environment();
        DirectionalLight sun = new DirectionalLight( Color.WHITE, lightDirection);
        sun.setIntensity(1f);
        environment.add( sun );
        //environment.setShadowMap(shadowCam, depthMap);
        environment.ambientLightLevel = 0.5f;
        environment.setCubeMap(cubeMap);

        skybox = new SkyBox(cubeMap);
        environment.setSkybox(skybox);

        camController = new CameraController(camera);
        LibGPU.input.setInputProcessor(camController);


        modelBatch = new ModelBatch();

        batch = new SpriteBatch();
        font = new BitmapFont();
    }




    private void updateModelMatrix(Matrix4 modelMatrix, float currentTime){
        Matrix4 RT = new Matrix4().idt(); //setToXRotation((float) ( -0.5f*Math.PI ));
        Matrix4 R1 = new Matrix4().setToYRotation(currentTime*0.3f);
        Matrix4 T = new Matrix4();//.translate(0f, 1f, 0f);
        modelMatrix.idt().mul(R1).mul(T).mul(RT);
    }





    public void render(){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){    // to force shaders to be recompiled
            LibGPU.app.exit();
        }
        if(LibGPU.input.isKeyPressed(Input.Keys.L)){    // to force shaders to be recompiled
            modelBatch.invalidatePipelines();
        }

        currentTime += 0.2f*LibGPU.graphics.getDeltaTime();
        //updateModelMatrix(modelMatrix, currentTime);
        camController.update();



        // pass #1 : depth map
//        environment.depthPass = true;
//        environment.renderShadows = false;
//        environment.setShadowMap(shadowCam, null);
//
//        modelBatch.begin(shadowCam, environment, Color.GRAY, colorMap, depthMap);
//        modelBatch.render(instances);
//        modelBatch.end();

        // pass #2 : render colours
        environment.depthPass = false;
        environment.renderShadows = false;
        //environment.setShadowMap(shadowCam, depthMap);

        modelBatch.begin(camera, environment, Color.BLUE);
        modelBatch.render(instances);
        modelBatch.end();


        ScreenUtils.clear(null);
        batch.begin();
        font.draw(batch, "fps: " + fps , 10, 50);
        batch.end();


        // At the end of the frame

        if (System.nanoTime() - startTime > 1000000000) {
            System.out.println("fps: " + frames +
                    " GPU: "+(int)LibGPU.app.getAverageGPUtime()+" microseconds"  );
            fps = frames;
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
        skybox.dispose();
        cubeMap.dispose();
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }


}

