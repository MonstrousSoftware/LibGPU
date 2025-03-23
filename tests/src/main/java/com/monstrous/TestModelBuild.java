package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g3d.*;
import com.monstrous.graphics.lights.DirectionalLight;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.math.Vector3;
import com.monstrous.utils.ScreenUtils;
import com.monstrous.webgpu.WGPUVertexFormat;

/** Test building a model from scratch, rather than reading a file
 *
 */

public class TestModelBuild extends ApplicationAdapter {

    private ModelBatch modelBatch;
    private Camera camera;
    private Model model;
    private ModelInstance modelInstance;
    private Environment environment;
    private CameraController camController;

    public void create() {
        model = buildCube();//
        //model = new Model("models/ducky.obj");
        modelInstance = new ModelInstance(model, 0,0,0);

        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(6, 4, -6);
        camera.direction.set(camera.position).scl(-1).nor();
        camera.update();

        camController = new CameraController(camera);
        LibGPU.input.setInputProcessor(camController);

        environment = new Environment();
        environment.add( new DirectionalLight( new Color(1,1,1,1), new Vector3(0.1f,-1,0)));

        camController = new CameraController(camera);
        LibGPU.input.setInputProcessor( camController );

        modelBatch = new ModelBatch();
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
        //mesh.setIndices(null);

        Material material = new Material( Color.GREEN );

        return new Model(mesh, material);
    }

    private Model buildCube(){
        Vector3[] corners = {
                new Vector3(-1, 1, -1), new Vector3(1, 1, -1), new Vector3(1,-1,-1), new Vector3(-1, -1, -1),// front
                new Vector3(-1, 1,  1), new Vector3(1, 1,  1), new Vector3(1,-1, 1), new Vector3(-1, -1,  1),// back
        };

        VertexAttributes vertexAttributes = new VertexAttributes();
        vertexAttributes.add(VertexAttribute.Usage.POSITION, "position", WGPUVertexFormat.Float32x4, 0);
        vertexAttributes.add(VertexAttribute.Usage.COLOR, "color", WGPUVertexFormat.Float32x4, 1);
        //vertexAttributes.add(VertexAttribute.Usage.TEXTURE_COORDINATE,"uv", WGPUVertexFormat.Float32x2, 2);
        vertexAttributes.end();

        MeshBuilder mb = new MeshBuilder();
        mb.begin(vertexAttributes, 6*4, 36);

        mb.setNormal(0,0,-1);
        mb.addRect(corners[0], corners[3], corners[2], corners[1]); // front

        mb.setNormal(0,0,1);
        mb.addRect(corners[4], corners[5], corners[6], corners[7]); // back

        mb.setNormal(0,1,0);
        mb.addRect(corners[0], corners[1], corners[5], corners[4]); // top

        mb.setNormal(0,-1,0);
        mb.addRect(corners[3], corners[7], corners[6], corners[2]); // bottom

        mb.setNormal(-1,0,0);
        mb.addRect(corners[0], corners[4], corners[7], corners[3]); // left

        mb.setNormal(1,0,0);
        mb.addRect(corners[1], corners[2], corners[6], corners[5]); // right

        Mesh mesh = mb.end();


        Material material = new Material( Color.GREEN );

        return new Model(mesh, material);
    }

    public void render( ){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE))
            LibGPU.app.exit();

        camController.update();

        ScreenUtils.clear(Color.TEAL);

        modelBatch.begin(camera, environment);
        modelBatch.render(modelInstance);
        modelBatch.end();
    }

    public void dispose(){
        // cleanup
        model.dispose();
        modelBatch.dispose();
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }


}

