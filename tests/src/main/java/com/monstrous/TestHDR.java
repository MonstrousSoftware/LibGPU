package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.g3d.*;
import com.monstrous.graphics.g3d.ibl.IBLComposer;
import com.monstrous.graphics.g3d.shapeBuilder.BoxShapeBuilder;
import com.monstrous.graphics.g3d.shapeBuilder.SphereShapeBuilder;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.math.Vector3;
import com.monstrous.utils.Disposable;
import com.monstrous.webgpu.*;

import java.io.IOException;
import java.util.ArrayList;


public class TestHDR extends ApplicationAdapter {

    private static final int ENVMAP_SIZE = 2048;

    private SpriteBatch batch;
    private Texture textureEquirectangular;
    private Texture[] textureSides;
    //private Texture depthMap;
    private ShaderProgram shader;
    private Mesh mesh;
    private Model model;
    private ModelInstance sphereInstance;
    private ArrayList<Disposable> disposables;
    private ArrayList<ModelInstance> instances;
    private ModelInstance instance;
    private ModelBatch modelBatch;
    private PerspectiveCamera camera;
    CameraController camController;
    Environment environment;
    PerspectiveCamera snapCam;
    Texture environmentMap;     // cube map
    Texture irradianceMap;  // low resolution cube map
    Texture prefilterMap;    // mipmapped cube map
    Texture refCubeMap;


    @Override
    public void create() {

        disposables = new ArrayList<>();
        instances = new ArrayList<>();

        batch = new SpriteBatch();
        //shader = new ShaderProgram(Files.internal("shaders/sprite-HDR.wgsl"));

        //FileHandle file = Files.internal("hdr/brown_photostudio_02_1k.hdr");
        FileHandle file = Files.internal("hdr/leadenhall_market_2k.hdr");
        IBLComposer ibl = new IBLComposer();

        try {
            ibl.loadHDR(file);
            textureEquirectangular = ibl.getHDRTexture();
        } catch(IOException e) {
            System.out.println("Cannot load HDR file.");
        }

        model = buildUnitCube(textureEquirectangular);
        instance = new ModelInstance(model, 0,0,0);



        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(3, 2, -3);
        camera.direction.set(camera.position).scl(-1).nor();
        camera.update();

        camController = new CameraController(camera);
        LibGPU.input.setInputProcessor(camController);

        environment = new Environment();
        //environment.add( new DirectionalLight( Color.WHITE, new Vector3(0.1f,-1,0)));
        environment.ambientLightLevel = 0.5f;

        camController = new CameraController(camera);
        LibGPU.input.setInputProcessor( camController );

        modelBatch = new ModelBatch();



        textureSides = new Texture[6];




        snapCam = new PerspectiveCamera(90, ENVMAP_SIZE, ENVMAP_SIZE);
        snapCam.position.set(0,0,-1);
        snapCam.direction.set(0,0,1);
        snapCam.update();

        buildEnvMap();
        buildIrradianceMap();

        prefilterMap = new Texture(128, 128, true, 6 );  // mipmapped cube map

        buildRadianceMap();

        Texture brdfLUT = new Texture(Files.internal("environment/LUT.png"), false);
        environment.setCubeMap(null);
        environment.useImageBasedLighting = true;
        environment.setIrradianceMap(irradianceMap);
        environment.setRadianceMap(prefilterMap);
        environment.setBRDFLookUpTable( brdfLUT );

        environment.setSkybox(new SkyBox(environmentMap));

        //sphere =  new Model("models/sphere.gltf");

        Model sphere;
        for(int y = 0; y <= 5; y++) {
            for (int x = 0; x <= 5; x++) {
                sphere = buildSphere(0.2f * x, 0.2f*y);
                instances.add(new ModelInstance(sphere, 3 * (x - 2.5f), 3*(y-2.5f), 0));
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

        batch.begin();
        //batch.setShader(shader);
        //atch.draw(textureEquirectangular, 0,0);
//        for(int side = 0; side < 6; side++) {
//            batch.draw(textureSides[side], 128*side, LibGPU.graphics.getHeight() - 128);
//        }
        batch.end();


    }

    private void buildEnvMap(){
        // Convert an equirectangular image to a cube map
        environment.shaderSourcePath = "shaders/modelbatchEquilateral.wgsl";        // hacky
        LibGPU.commandEncoder = LibGPU.app.prepareEncoder();
        LibGPU.graphics.passNumber = 0;
        int size = ENVMAP_SIZE;
        Texture depthMap = new Texture(size, size, false, true, WGPUTextureFormat.Depth32Float, 1);
        constructSideTextures(size, depthMap);
        environmentMap = copyTextures(size);
        LibGPU.app.finishEncoder(LibGPU.commandEncoder);
        depthMap.dispose();

        //environment.setCubeMap(cubeMap);
        environment.shaderSourcePath = null;
        //environment.setSkybox(new SkyBox(environmentMap));
    }

    private void buildIrradianceMap(){
        // Convert an environment cube map to an irradiance cube map
        environment.shaderSourcePath = "shaders/modelbatchCubeMapIrradiance.wgsl";        // hacky
        environment.setCubeMap(environmentMap);
        LibGPU.commandEncoder = LibGPU.app.prepareEncoder();
        LibGPU.graphics.passNumber = 0;
        int size = 32;
        Texture depthMap = new Texture(size, size, false, true, WGPUTextureFormat.Depth32Float, 1);
        constructSideTextures(size, depthMap);
        irradianceMap = copyTextures(size);
        LibGPU.app.finishEncoder(LibGPU.commandEncoder);
        depthMap.dispose();

        //environment.setCubeMap(cubeMap);
        environment.shaderSourcePath = null;
        environment.setSkybox(new SkyBox(irradianceMap));
    }

    private void buildRadianceMap(){
        // Convert an environment cube map to an irradiance cube map
        environment.shaderSourcePath = "shaders/modelbatchCubeMapRadiance.wgsl";        // hacky
        environment.setSkybox(null);
        environment.setCubeMap(environmentMap);
        LibGPU.commandEncoder = LibGPU.app.prepareEncoder();
        LibGPU.graphics.passNumber = 0;
        Texture[] depthMaps = new Texture[5];
        int size = 128;
        for(int mip = 0; mip < 5; mip++) {
            environment.ambientLightLevel = 0.2f*mip;   // use this to pass roughness level
            depthMaps[mip] = new Texture(size, size, false, true, WGPUTextureFormat.Depth32Float, 1);

            constructSideTextures(size, depthMaps[mip]);
            copyTextures(prefilterMap, size, mip);
            size /= 2;
        }
        LibGPU.app.finishEncoder(LibGPU.commandEncoder);
        for(int mip = 0; mip < 5; mip++)
            depthMaps[mip].dispose();

        //environment.setCubeMap(cubeMap);
        environment.shaderSourcePath = null;
    }


    // the order of the layers is +X, -X, +Y, -Y, +Z, -Z
    private final Vector3[] directions = { new Vector3(1, 0, 0), new Vector3(-1, 0, 0), new Vector3(0, -1, 0), new Vector3(0, 1, 0),
            new Vector3(0,0,1), new Vector3(0, 0, -1)
    };

    // note: for use as a skybox the images are mirrored
    // also seem more pixelated than expected
    private void constructSideTextures(int size, Texture depthMap){

        for (int side = 0; side < 6; side++) {

            snapCam.direction.set(directions[side]);
            snapCam.position.set(new Vector3(directions[side]).scl(-1));
            if(side == 3)
                snapCam.up.set(0,0,-1);
            else if (side == 2)
                snapCam.up.set(0,0,1);
            else
                snapCam.up.set(0,1,0);
            snapCam.update();

            textureSides[side] = new Texture(size, size, false, true, WGPUTextureFormat.RGBA8Unorm, 1);


            modelBatch.begin(snapCam, environment, Color.GREEN, textureSides[side], depthMap);
            modelBatch.render(instance);
            modelBatch.end();
        }

    }


    /** copy 6 textures (textureSides[]) into a new cube map */
    private Texture copyTextures(int size) {
        Texture cube = new Texture(size, size, 6);
        return copyTextures(cube, size, 0);
    }


    private Texture copyTextures(Texture cube, int size, int mipLevel){
        for (int side = 0; side < 6; side++) {

            WGPUImageCopyTexture source = WGPUImageCopyTexture.createDirect()
                    .setTexture(textureSides[side].getHandle())
                    .setMipLevel(0)
                    .setAspect(WGPUTextureAspect.All);
            source.getOrigin().setX(0);
            source.getOrigin().setY(0);
            source.getOrigin().setZ(0);

            WGPUImageCopyTexture destination = WGPUImageCopyTexture.createDirect()
                    .setTexture(cube.getHandle())
                    .setMipLevel(0)
                    .setAspect(WGPUTextureAspect.All);
            destination.getOrigin().setX(0);
            destination.getOrigin().setY(0);
            destination.getOrigin().setZ(side);
            destination.setMipLevel(mipLevel);

            WGPUExtent3D ext = WGPUExtent3D.createDirect()
                    .setWidth(size)
                    .setHeight(size)
                    .setDepthOrArrayLayers(1);

            LibGPU.webGPU.wgpuCommandEncoderCopyTextureToTexture(LibGPU.commandEncoder, source, destination, ext);
        }
        return cube;
    }

    @Override
    public void dispose(){
        // cleanup
        textureEquirectangular.dispose();
        batch.dispose();
        mesh.dispose();
        model.dispose();
        for(Disposable d : disposables)
            d.dispose();
    }

    @Override
    public void resize(int width, int height) {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }

    private Model buildUnitCube(Texture texture){

        MeshBuilder mb = new MeshBuilder();
        // vertex attributes are fixed per mesh
        VertexAttributes vertexAttributes = new VertexAttributes();
        vertexAttributes.add(VertexAttribute.Usage.POSITION, "position", WGPUVertexFormat.Float32x4, 0);
        vertexAttributes.add(VertexAttribute.Usage.TEXTURE_COORDINATE, "uv", WGPUVertexFormat.Float32x2, 1);
        vertexAttributes.end();

        mb.begin(vertexAttributes, 32, 36);

        MeshPart meshPart = BoxShapeBuilder.build(mb, 1, 1, 1,  WGPUPrimitiveTopology.TriangleList);
        Material material = new Material( texture );
        mesh = mb.end();

        return new Model(meshPart, material);
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
        mesh = mb.end();

        return new Model(meshPart, material);
    }

}
