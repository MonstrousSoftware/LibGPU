package com.monstrous;

import com.monstrous.graphics.*;
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
import com.monstrous.utils.ScreenUtils;

import java.io.IOException;

public class TestGLB extends ApplicationAdapter {

    private ModelBatch modelBatch;
    private Camera camera;
    private Environment environment;
    private Matrix4 modelMatrix;
    private Model model;
    private ModelInstance modelInstance;
    private float currentTime;
    private long startTime;
    private int frames;
    private CameraController camController;
    private Texture textureEquirectangular;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        model = new Model("models/waterbottle/waterbottle.glb");
        modelMatrix = new Matrix4();
        modelInstance = new ModelInstance(model, modelMatrix);

        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(0, 0f, -1);
        camera.direction.set(0,0f, 1f);
        camera.far = 1000f;
        camera.near = 0.001f;
        camera.update();

        environment = new Environment();
        environment.add( new DirectionalLight( new Color(1,1,1,1), new Vector3(0,-1,0)));
        environment.ambientLightLevel = 0.5f;
        FileHandle file = Files.internal("hdr/brown_photostudio_02_1k.hdr");
        HDRLoader hdrLoader = new HDRLoader();

        try {
            hdrLoader.loadHDR(file);
            textureEquirectangular = hdrLoader.getHDRTexture(true);
        } catch(IOException e) {
            System.out.println("Cannot load HDR file.");
        }

        // create image based environmental lighting
        ImageBasedLighting ibl = new ImageBasedLighting();

        CubeMap environmentMap = ibl.buildCubeMapFromEquirectangularTexture(textureEquirectangular, 2048);
        CubeMap irradianceMap = ibl.buildIrradianceMap(environmentMap, 32);
        CubeMap prefilterMap = ibl.buildRadianceMap(environmentMap, 128);
        Texture brdfLUT = ibl.getBRDFLookUpTable();

        environment.useImageBasedLighting = true;
        environment.setIrradianceMap(irradianceMap);
        environment.setRadianceMap(prefilterMap);
        environment.setBRDFLookUpTable( brdfLUT );

        environment.setSkybox(new SkyBox(irradianceMap));

        camController = new CameraController(camera);
        LibGPU.input.setInputProcessor( camController );


        modelBatch = new ModelBatch();
    }




    private void updateModelMatrix(Matrix4 modelMatrix, float currentTime){
        Matrix4 RT = new Matrix4().idt(); //setToXRotation((float) ( -0.5f*Math.PI ));
        Matrix4 R1 = new Matrix4().setToYRotation(currentTime*0.3f);
        Matrix4 T = new Matrix4(); //.translate(1.8f, 0f, 0f);
        modelMatrix.idt().mul(R1).mul(T).mul(RT);
    }


    public void render(){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){
            LibGPU.app.exit();
        }
        currentTime += LibGPU.graphics.getDeltaTime();
        ScreenUtils.clear(.7f, .7f, .7f, 1);

        updateModelMatrix(modelMatrix, currentTime);
        camController.update();

        modelBatch.begin(camera, environment);
        modelBatch.render(modelInstance);
        modelBatch.end();

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
        textureEquirectangular.dispose();
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }


}

