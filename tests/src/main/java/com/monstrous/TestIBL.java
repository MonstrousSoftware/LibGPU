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

public class TestIBL extends ApplicationAdapter {
    private ModelBatch modelBatch;
    private Camera camera;
    private CameraController camController;
    private Environment environment;
    private Model model;
    private ArrayList<ModelInstance> instances;
    private long startTime;
    private int frames;
    private SpriteBatch batch;
    private BitmapFont font;
    private Texture cubeMap;
    private Texture irradianceCubeMap;
    private SkyBox skybox;
    private int fps;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        instances = new ArrayList<>();

        model = new Model("models/sphere.gltf");

        ModelInstance modelInstance = new ModelInstance(model, 0,0,0);
        instances.add(modelInstance);

        // the order of the layers is +X, -X, +Y, -Y, +Z, -Z
        String[] fileNames = {
                "environment/Studio-envmap_posx.png",
                "environment/Studio-envmap_negx.png",
                "environment/Studio-envmap_posy.png",
                "environment/Studio-envmap_negy.png",
                "environment/Studio-envmap_posz.png",
                "environment/Studio-envmap_negz.png"
        };

        cubeMap = new Texture(fileNames, true, WGPUTextureFormat.RGBA8Unorm);       // format should be taken from the image files....


        String[] fileNamesIrradiance = {
                "environment/Studio-irradiance_posx.png",
                "environment/Studio-irradiance_negx.png",
                "environment/Studio-irradiance_posy.png",
                "environment/Studio-irradiance_negy.png",
                "environment/Studio-irradiance_posz.png",
                "environment/Studio-irradiance_negz.png"
        };

        irradianceCubeMap = new Texture(fileNamesIrradiance, false, WGPUTextureFormat.RGBA8Unorm);       // format should be taken from the image files....


        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());

        camera.position.set(0, 2, 0);
        camera.direction.set(0, 0, 1);
        camera.up.set(0,1,0);

        camera.far = 50f;
        camera.near = 0.1f;
        camera.update();

        // unit vector from main light source
        Vector3 lightDirection = new Vector3(1.3f, -1f, .3f).nor();


        environment = new Environment();
        DirectionalLight sun = new DirectionalLight( Color.WHITE, lightDirection);
        sun.setIntensity(1f);
        environment.add( sun );
        environment.ambientLightLevel = 0.5f;
        environment.setCubeMap(irradianceCubeMap);

        skybox = new SkyBox(cubeMap);
        environment.setSkybox(skybox);

        camController = new CameraController(camera);
        LibGPU.input.setInputProcessor(camController);


        modelBatch = new ModelBatch();

        batch = new SpriteBatch();
        font = new BitmapFont();
    }



    public void render(){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){    // to force shaders to be recompiled
            LibGPU.app.exit();
        }
        if(LibGPU.input.isKeyPressed(Input.Keys.L)){    // to force shaders to be recompiled
            modelBatch.invalidatePipelines();
        }

        camController.update();

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

