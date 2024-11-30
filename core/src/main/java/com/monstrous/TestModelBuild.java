package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g3d.Mesh;
import com.monstrous.graphics.g3d.Model;
import com.monstrous.graphics.g3d.ModelBatch;
import com.monstrous.graphics.g3d.ModelInstance;
import com.monstrous.graphics.lights.DirectionalLight;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.math.Vector3;
import com.monstrous.utils.ScreenUtils;
import com.monstrous.wgpu.WGPUVertexFormat;

// Test building a model from scratch, rather than reading a file
// DOESN'T WORK YET

public class TestModelBuild extends ApplicationAdapter {

    private ModelBatch modelBatch;
    private Camera camera;
    private Model model;
    private ModelInstance modelInstance;
    private Environment environment;

    public void create() {
        model = buildModel();//new Model("models/ducky.obj");
        modelInstance = new ModelInstance(model, 0,0,0);

        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(0, 1, -3);
        camera.direction.set(0,0f, 1f);
        camera.update();

        environment = new Environment();
        environment.add( new DirectionalLight( new Color(1,1,1,1), new Vector3(0,-1,0)));

        LibGPU.input.setInputProcessor(new CameraController(camera));

        modelBatch = new ModelBatch();
    }

    private Model buildModel(){
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
        vertexAttributes.add("position", WGPUVertexFormat.Float32x4, 0);
        vertexAttributes.add("color", WGPUVertexFormat.Float32x4, 1);
        vertexAttributes.add("uv", WGPUVertexFormat.Float32x2, 2);
        vertexAttributes.end();

        Mesh mesh = new Mesh();
        mesh.setVertexAttributes(vertexAttributes);
        mesh.setVertices(vertexData);
        mesh.setIndices(null);

        Material material = new Material( new Color(1,1,1,1));

        return new Model(mesh, material);
    }

    public void render( ){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE))
            LibGPU.app.exit();


        ScreenUtils.clear(.7f, .7f, .7f, 1);

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

