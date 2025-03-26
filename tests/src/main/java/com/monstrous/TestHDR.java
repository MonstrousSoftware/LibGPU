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
import com.monstrous.webgpu.WGPUPrimitiveTopology;
import com.monstrous.webgpu.WGPUVertexFormat;

import java.io.IOException;


public class TestHDR extends ApplicationAdapter {

    private SpriteBatch batch;
    private Texture texture;
    private ShaderProgram shader;
    private Mesh mesh;
    private Model model;
    private ModelInstance instance;
    private ModelBatch modelBatch;
    private PerspectiveCamera camera;
    CameraController camController;
    Environment environment;



    @Override
    public void create() {

        batch = new SpriteBatch();
        //shader = new ShaderProgram(Files.internal("shaders/sprite-HDR.wgsl"));

        FileHandle file = Files.internal("hdr/brown_photostudio_02_1k.hdr");
        IBLComposer ibl = new IBLComposer();

        try {
            ibl.loadHDR(file);
            texture = ibl.getHDRTexture();
        } catch(IOException e) {
            System.out.println("Cannot load HDR file.");
        }

        model = buildUnitCube(texture);
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

    }



    @Override
    public void render(){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){
            LibGPU.app.exit();
            return;
        }

        camController.update();

        ScreenUtils.clear(Color.BLUE);

        batch.begin();
        //batch.setShader(shader);
        batch.draw(texture, 0,0);
        batch.end();

        modelBatch.begin(camera, environment);
        modelBatch.render(instance);
        modelBatch.end();
    }

    @Override
    public void dispose(){
        // cleanup
        texture.dispose();
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
