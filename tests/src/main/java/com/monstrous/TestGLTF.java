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

public class TestGLTF extends ApplicationAdapter {

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
        startTime = System.nanoTime();
        frames = 0;



        instances = new ArrayList<>();

        //model = new Model("models/lantern/Lantern.gltf");
        //model = new Model("models/ToyCar/ToyCar.gltf");
        //model = new Model("models/Buggy/Buggy.gltf");
        model = new Model("models/DamagedHelmet/DamagedHelmet.gltf");
        //model = new Model("models/fourareen.obj")


        modelMatrix = new Matrix4();
        modelInstance1 = new ModelInstance(model, modelMatrix);
        instances.add(modelInstance1);




        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(0, 1f, -1.6f);
        camera.direction.set(0,0f, 1f);
        camera.far = 200f;
        camera.near = 0.001f;
        camera.update();

        environment = new Environment();

        FileHandle file = Files.internal("hdr/leadenhall_market_2k.hdr");
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



        environment.add( new DirectionalLight( new Color(1,1,1,1), new Vector3(0,-1,0)));
        environment.ambientLightLevel = 0.5f;

        camController = new CameraController(camera);
        LibGPU.input.setInputProcessor( camController );

        modelBatch = new ModelBatch();

        batch = new SpriteBatch();
        font = new BitmapFont();
        status = "...";
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

        updateModelMatrix(modelMatrix, currentTime);
        camController.update();

        modelBatch.begin(camera, environment, Color.GRAY);
        modelBatch.render(instances);
        modelBatch.end();


        batch.begin();
        font.draw(batch, status, 10, 50);
        batch.end();


        // At the end of the frame

        if (System.nanoTime() - startTime > 1000000000) {
            status = "fps: " + frames + " GPU: "+(int)LibGPU.app.getAverageGPUtime()+" microseconds";
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
        batch.dispose();
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }


}

