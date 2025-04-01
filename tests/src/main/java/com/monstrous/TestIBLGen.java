package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.g3d.*;
import com.monstrous.graphics.g3d.ibl.HDRLoader;
import com.monstrous.graphics.g3d.ibl.ImageBasedLighting;
import com.monstrous.graphics.g3d.shapeBuilder.SphereShapeBuilder;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.graphics.lights.PointLight;
import com.monstrous.math.Vector3;
import com.monstrous.utils.Disposable;
import com.monstrous.webgpu.*;

import java.io.IOException;
import java.util.ArrayList;

/** Tests generation of IBL textures from an HDR file.
 *
 */

public class TestIBLGen extends ApplicationAdapter {

    private static final int ENVMAP_SIZE = 2048;

    private SpriteBatch batch;
    private Texture textureEquirectangular;
    private ArrayList<Disposable> disposables;
    private ArrayList<ModelInstance> instances;
    private ModelBatch modelBatch;
    private PerspectiveCamera camera;
    private CameraController camController;
    private Environment environment;


    @Override
    public void create() {

        disposables = new ArrayList<>();
        instances = new ArrayList<>();

        batch = new SpriteBatch();

        //FileHandle file = Files.internal("hdr/brown_photostudio_02_1k.hdr");
        FileHandle file = Files.internal("hdr/leadenhall_market_2k.hdr");
        HDRLoader hdrLoader = new HDRLoader();

        try {
            hdrLoader.loadHDR(file);
            textureEquirectangular = hdrLoader.getHDRTexture(true);
        } catch(IOException e) {
            System.out.println("Cannot load HDR file.");
        }

        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(3, 2, -3);
        camera.direction.set(camera.position).scl(-1).nor();
        camera.update();

        camController = new CameraController(camera);
        LibGPU.input.setInputProcessor(camController);

        environment = new Environment();
        float intensity = 250f;
        environment.add( new PointLight(Color.WHITE, new Vector3(-10f,10f,10), intensity));
        environment.add( new PointLight(Color.WHITE, new Vector3(10f,-10f,10), intensity));
        environment.add( new PointLight(Color.WHITE, new Vector3(10f,10f,10), intensity));
        environment.add( new PointLight(Color.WHITE, new Vector3(-10f,-10f,10), intensity));

        camController = new CameraController(camera);
        LibGPU.input.setInputProcessor( camController );

        modelBatch = new ModelBatch();

        ImageBasedLighting ibl = new ImageBasedLighting();

        Texture environmentMap = ibl.buildEnvironmentMapFromEquirectangularTexture(textureEquirectangular, ENVMAP_SIZE);
        Texture irradianceMap = ibl.buildIrradianceMap(environmentMap, 32);
        Texture prefilterMap = ibl.buildRadianceMap(environmentMap, 128, 5);
        Texture brdfLUT = ibl.getBRDFLookUpTable();

        environment.useImageBasedLighting = true;
        environment.setIrradianceMap(irradianceMap);
        environment.setRadianceMap(prefilterMap);
        environment.setBRDFLookUpTable( brdfLUT );

        environment.setSkybox(new SkyBox(environmentMap));


        Model sphere;
        for(int y = 0; y <= 1; y++) {
            for (int x = 0; x <= 5; x++) {
                sphere = buildSphere(y, 0.2f*x);
                sphere.getMaterials().get(0).baseColor.set(1.0f, y, y, 1.0f);
                instances.add(new ModelInstance(sphere, 3 * (x - 2.5f), 3*y-1.5f, 0));
                disposables.add(sphere);
            }
        }
    }


    @Override
    public void render(){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){
            LibGPU.app.exit();
            return;
        }

        camController.update();

        modelBatch.begin(camera, environment);
        modelBatch.render(instances);
        modelBatch.end();
    }

    @Override
    public void dispose(){
        // cleanup
        textureEquirectangular.dispose();
        batch.dispose();
        for(Disposable d : disposables)
            d.dispose();
    }

    @Override
    public void resize(int width, int height) {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }

    private Model buildSphere(float metallic, float roughness){

        MeshBuilder mb = new MeshBuilder();
        // vertex attributes are fixed per mesh
        VertexAttributes vertexAttributes = new VertexAttributes();
        vertexAttributes.add(VertexAttribute.Usage.POSITION, "position", WGPUVertexFormat.Float32x4, 0);
        vertexAttributes.add(VertexAttribute.Usage.TEXTURE_COORDINATE, "uv", WGPUVertexFormat.Float32x2, 1);
        vertexAttributes.add(VertexAttribute.Usage.NORMAL, "normal", WGPUVertexFormat.Float32x3, 2);
        vertexAttributes.end();

        mb.begin(vertexAttributes, 4096, 4096);

        MeshPart meshPart = SphereShapeBuilder.build(mb, 1, 32,  WGPUPrimitiveTopology.TriangleStrip);
        Material material = new Material( Color.RED );
        material.metallicFactor = metallic;
        material.roughnessFactor = roughness;
        mb.end();

        return new Model(meshPart, material);
    }

}
