package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.g3d.*;
import com.monstrous.graphics.g3d.ibl.IBLComposer;
import com.monstrous.graphics.g3d.shapeBuilder.BoxShapeBuilder;
import com.monstrous.graphics.lights.DirectionalLight;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.math.Vector3;
import com.monstrous.utils.ScreenUtils;
import com.monstrous.webgpu.*;
import jnr.ffi.Pointer;

import java.io.IOException;


public class TestHDR extends ApplicationAdapter {

    private static final int SIZE = 256;

    private SpriteBatch batch;
    private Texture textureEquirectangular;
    private Texture[] textureSides;
    private Texture depthMap;
    private ShaderProgram shader;
    private Mesh mesh;
    private Model model;
    private ModelInstance instance;
    private ModelBatch modelBatch;
    private PerspectiveCamera camera;
    CameraController camController;
    Environment environment;
    PerspectiveCamera snapCam;
    Texture cubeMap;
    Texture refCubeMap;


    @Override
    public void create() {

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
        environment.add( new DirectionalLight( Color.WHITE, new Vector3(0.1f,-1,0)));
        environment.ambientLightLevel = 0.8f;

        camController = new CameraController(camera);
        LibGPU.input.setInputProcessor( camController );

        modelBatch = new ModelBatch();


        textureSides = new Texture[6];

//        String[] fileNames = {
//                "textures/leadenhall/pos-x.jpg",
//                "textures/leadenhall/neg-x.jpg",
//                "textures/leadenhall/pos-y.jpg",
//                "textures/leadenhall/neg-y.jpg",
//                "textures/leadenhall/pos-z.jpg",
//                "textures/leadenhall/neg-z.jpg",
//        };
//
//        refCubeMap = new Texture(fileNames, true, WGPUTextureFormat.RGBA8Unorm);       // format should be taken from the image files....
//        environment.setSkybox(new SkyBox(refCubeMap));

        depthMap = new Texture(SIZE, SIZE, false, true, WGPUTextureFormat.Depth32Float, 1);

        snapCam = new PerspectiveCamera(90, SIZE, SIZE);
        snapCam.position.set(0,0,-1);
        snapCam.direction.set(0,0,1);
        snapCam.update();

        prepare();

    }



    @Override
    public void render(){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){
            LibGPU.app.exit();
            return;
        }

        camController.update();


        modelBatch.begin(camera, environment);
        modelBatch.render(instance);
        modelBatch.end();

        batch.begin();
        //batch.setShader(shader);
        //batch.draw(textureEquirectangular, 0,0);
        for(int side = 0; side < 6; side++) {
            batch.draw(textureSides[side], SIZE*side, LibGPU.graphics.getHeight() - SIZE);
        }
        batch.end();


    }

    private void prepare(){
        environment.shaderSourcePath = "shaders/modelbatchEquilateral.wgsl";        // hacky
        LibGPU.commandEncoder = LibGPU.app.prepareEncoder();
        constructSideTextures();
        cubeMap = copyTextures();
        LibGPU.app.finishEncoder(LibGPU.commandEncoder);

        environment.setCubeMap(cubeMap);
        environment.shaderSourcePath = null;
        environment.setSkybox(new SkyBox(cubeMap));
    }


    // the order of the layers is +X, -X, +Y, -Y, +Z, -Z
    private final Vector3[] directions = { new Vector3(1, 0, 0), new Vector3(-1, 0, 0), new Vector3(0, -1, 0), new Vector3(0, 1, 0),
            new Vector3(0,0,1), new Vector3(0, 0, -1)
    };

    // note: for use as a skybox the images are mirrored
    // also seem more pixelated than expected
    private void constructSideTextures(){
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

            textureSides[side] = new Texture(SIZE, SIZE, false, true, WGPUTextureFormat.RGBA8Unorm, 1);

            modelBatch.begin(snapCam, environment, Color.GREEN, textureSides[side], depthMap);
            modelBatch.render(instance);
            modelBatch.end();
        }
    }


    private Texture copyTextures(){
        Texture cube = new Texture(SIZE, SIZE, 6);

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

            WGPUExtent3D ext = WGPUExtent3D.createDirect()
                    .setWidth(SIZE)
                    .setHeight(SIZE)
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

}
