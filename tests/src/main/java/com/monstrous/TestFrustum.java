package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.g3d.Mesh;
import com.monstrous.graphics.g3d.Model;
import com.monstrous.graphics.g3d.ModelBatch;
import com.monstrous.graphics.g3d.ModelInstance;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;
import com.monstrous.utils.ScreenUtils;
import com.monstrous.webgpu.WGPUVertexFormat;

import java.util.ArrayList;

/** Test building a model from scratch, rather than reading a file
 *
 */

public class TestFrustum extends ApplicationAdapter {

    private ModelBatch modelBatch;
    private Camera camera;
    private PerspectiveCamera subCam;
    private Matrix4 subCamMatrix;
    private Model frustumModel;
    private Model blockModel;
    private ArrayList<ModelInstance> instances;
    private ArrayList<ModelInstance> visibleInstances;
    private Environment environment;
    private CameraController camController;
    private float time;
    private SpriteBatch spriteBatch;
    private BitmapFont font;

    public void create() {

        Vector3 worldUp = new Vector3(0,1,0);

        subCam = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        subCam.position.set(0,0,0);
        subCam.direction.set(0,1,1);
        Vector3 tmpRight = new Vector3().set(subCam.direction).crs(worldUp);
        subCam.up.set(tmpRight).crs(subCam.direction).nor();
        subCam.near = 2;
        subCam.far = 10;
        subCam.update();
        time = 0;


        frustumModel = buildFrustumModel(subCam);
        instances = new ArrayList<>();
        visibleInstances = new ArrayList<>();
        subCamMatrix = new Matrix4();


        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(6, 4, -6);
        camera.direction.set(camera.position).scl(-1).nor();

        camera.far = 100f;
        camera.update();

        camController = new CameraController(camera);
        LibGPU.input.setInputProcessor(camController);

        environment = new Environment();
        //environment.add( new DirectionalLight( new Color(1,1,1,1), new Vector3(0,-1,0)));
        environment.ambientLightLevel = 0.8f;

        camController = new CameraController(camera);
        LibGPU.input.setInputProcessor( camController );
        camController.update();

        modelBatch = new ModelBatch();

        blockModel = buildModel();

        populate();

        spriteBatch = new SpriteBatch();
        font = new BitmapFont();

    }

    private void populate(){
        instances.clear();

        instances.add( new ModelInstance(frustumModel, subCamMatrix) );
        instances.add( new ModelInstance(blockModel, 0,0,0) );

        for(int x = -15; x <= 15; x+=1){
            for(int z = -15; z <= 20; z += 2){
                Matrix4 transform = new Matrix4().idt().setToTranslation(x, 0, z).scale(0.1f);
                ModelInstance instance = new ModelInstance(blockModel, transform);
                instances.add( instance );
            }
        }
        for(int y = -25; y <= 25; y+=1){
            for(int z = -25; z <= 25; z += 1){
                Matrix4 transform = new Matrix4().idt().setToTranslation(0, y, z).scale(0.1f);
                instances.add( new ModelInstance(blockModel, transform));
            }
        }


    }

    private void cull(){
        visibleInstances.clear();

        for(ModelInstance instance : instances ){
            if(!subCam.frustum.boundsInFrustum(instance.boundingBox))
                visibleInstances.add(instance);
        }
        visibleInstances.add( new ModelInstance(frustumModel, subCamMatrix) );
        visibleInstances.add( new ModelInstance(blockModel, 0,0,0) );
    }




    private Model buildFrustumModel(PerspectiveCamera cam) {
        VertexAttributes vertexAttributes = new VertexAttributes();
        vertexAttributes.add(VertexAttribute.Usage.POSITION, "position", WGPUVertexFormat.Float32x4, 0);
        vertexAttributes.end();


        int stride = 4;
        float[]  vertexData = new float[stride*8];
        for(int i = 0; i < 8; i++){
            vertexData[stride*i] = cam.frustum.corners[i].x;
            vertexData[stride*i+1] = cam.frustum.corners[i].y;
            vertexData[stride*i+2] = cam.frustum.corners[i].z;
            vertexData[stride*i+3] = 1.0f;
        }

        // standard winding order is counter-clockwise
        short[] indexData = {
                0, 1, 2, 0, 2, 3,   // near face
                4, 6, 5, 4, 7, 6,   // far face
                1, 5, 6, 1, 6, 2,   // right face
                0, 7, 4, 0, 3, 7,   // left face
                3, 6, 7, 3, 2, 6,   // top face
                0, 5, 1, 0, 4, 5    // bottom face
        };

        Mesh mesh = new Mesh();
        mesh.setVertexAttributes(vertexAttributes);
        mesh.setVertices(vertexData);
        mesh.setIndices(indexData);

        Material material = new Material( new Color(0,1,0,0.1f) );

        return new Model(mesh, material);
    }

    private Model buildModel(){
        // build a cube
        float[]  vertexData = {
                // float4 position, float4 color, float2 uv,
                1, -1, 1, 1, 1, 0, 1, 1, 0, 1,
                -1, -1, 1, 1, 0, 0, 1, 1, 1, 1,
                -1, -1, -1, 1, 0, 0, 0, 1, 1, 0,
                1, -1, -1, 1, 1, 0, 0, 1, 0, 0,
                1, -1, 1, 1, 1, 0, 1, 1, 0, 1,
                -1, -1, -1, 1, 0, 0, 0, 1, 1, 0,

                1, 1, 1, 1, 1, 1, 1, 1, 0, 1,
                1, -1, 1, 1, 1, 0, 1, 1, 1, 1,
                1, -1, -1, 1, 1, 0, 0, 1, 1, 0,
                1, 1, -1, 1, 1, 1, 0, 1, 0, 0,
                1, 1, 1, 1, 1, 1, 1, 1, 0, 1,
                1, -1, -1, 1, 1, 0, 0, 1, 1, 0,

                -1, 1, 1, 1, 0, 1, 1, 1, 0, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, -1, 1, 1, 1, 0, 1, 1, 0,
                -1, 1, -1, 1, 0, 1, 0, 1, 0, 0,
                -1, 1, 1, 1, 0, 1, 1, 1, 0, 1,
                1, 1, -1, 1, 1, 1, 0, 1, 1, 0,

                -1, -1, 1, 1, 0, 0, 1, 1, 0, 1,
                -1, 1, 1, 1, 0, 1, 1, 1, 1, 1,
                -1, 1, -1, 1, 0, 1, 0, 1, 1, 0,
                -1, -1, -1, 1, 0, 0, 0, 1, 0, 0,
                -1, -1, 1, 1, 0, 0, 1, 1, 0, 1,
                -1, 1, -1, 1, 0, 1, 0, 1, 1, 0,

                1, 1, 1, 1, 1, 1, 1, 1, 0, 1,
                -1, 1, 1, 1, 0, 1, 1, 1, 1, 1,
                -1, -1, 1, 1, 0, 0, 1, 1, 1, 0,
                -1, -1, 1, 1, 0, 0, 1, 1, 1, 0,
                1, -1, 1, 1, 1, 0, 1, 1, 0, 0,
                1, 1, 1, 1, 1, 1, 1, 1, 0, 1,

                1, -1, -1, 1, 1, 0, 0, 1, 0, 1,
                -1, -1, -1, 1, 0, 0, 0, 1, 1, 1,
                -1, 1, -1, 1, 0, 1, 0, 1, 1, 0,
                1, 1, -1, 1, 1, 1, 0, 1, 0, 0,
                1, -1, -1, 1, 1, 0, 0, 1, 0, 1,
                -1, 1, -1, 1, 0, 1, 0, 1, 1, 0,
        };

        VertexAttributes vertexAttributes = new VertexAttributes();
        vertexAttributes.add(VertexAttribute.Usage.POSITION, "position", WGPUVertexFormat.Float32x4, 0);
        vertexAttributes.add(VertexAttribute.Usage.COLOR, "color", WGPUVertexFormat.Float32x4, 1);
        vertexAttributes.add(VertexAttribute.Usage.TEXTURE_COORDINATE,"uv", WGPUVertexFormat.Float32x2, 2);
        vertexAttributes.end();

        Mesh mesh = new Mesh();
        mesh.setVertexAttributes(vertexAttributes);
        mesh.setVertices(vertexData);


        Material material = new Material( Color.BLUE );

        return new Model(mesh, material);
    }

    public void render( ){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE))
            LibGPU.app.exit();

        time += 10*LibGPU.graphics.getDeltaTime();
        float angle = time*(float)Math.PI/180f;
        float yangle = 45f*(float)Math.PI/180f;
        subCam.direction.set((float)Math.sin(angle)*(float)Math.cos(yangle),(float)Math.sin(yangle),(float)Math.cos(angle)*(float)Math.cos(yangle));
        subCam.update();
        subCamMatrix.setToYRotation(-angle);

        camController.update();
        cull();

        ScreenUtils.clear(Color.TEAL);

        modelBatch.begin(camera, environment);
        modelBatch.render(visibleInstances);
        modelBatch.end();

        spriteBatch.begin();
        font.draw(spriteBatch, "visible/modelInstances: "+visibleInstances.size()+"/"+instances.size(), 10, 50);
        spriteBatch.end();
    }

    public void dispose(){
        // cleanup
        frustumModel.dispose();
        modelBatch.dispose();
        spriteBatch.dispose();
        font.dispose();
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }


}

