package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.g3d.*;
import com.monstrous.graphics.g3d.shapeBuilder.BoxShapeBuilder;
import com.monstrous.graphics.g3d.shapeBuilder.SphereShapeBuilder;
import com.monstrous.graphics.lights.DirectionalLight;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.math.Vector2;
import com.monstrous.math.Vector3;
import com.monstrous.utils.ScreenUtils;
import com.monstrous.webgpu.WGPUPrimitiveTopology;
import com.monstrous.webgpu.WGPUVertexFormat;

import java.util.ArrayList;

/** Test building a model from scratch, rather than reading a file
 * This shows some different approaches:
 * - Pushing data straight into the vertex buffer,
 * - Calling MeshBuilder directly.
 * - Or calling BoxShapeBuilder/SphereShapeBuilder.
 *
 */

public class TestModelBuild extends ApplicationAdapter {

    private ModelBatch modelBatch;
    private Camera camera;
    private Model modelBox, modelPyramid, modelSphere, modelBox2;
    private Mesh mesh;
    private ArrayList<ModelInstance> modelInstances;
    private Environment environment;
    private CameraController camController;
    private Texture texture;
    private SpriteBatch batch;
    private BitmapFont font;

    public void create() {
        modelInstances = new ArrayList<>();

        texture = new Texture("textures/jackRussel.png");

        MeshBuilder mb = new MeshBuilder();
        // vertex attributes are fixed per mesh
        VertexAttributes vertexAttributes = new VertexAttributes();
        vertexAttributes.add(VertexAttribute.Usage.POSITION, "position", WGPUVertexFormat.Float32x4, 0);
        vertexAttributes.add(VertexAttribute.Usage.TEXTURE_COORDINATE, "uv", WGPUVertexFormat.Float32x2, 1);
        vertexAttributes.add(VertexAttribute.Usage.NORMAL, "normal", WGPUVertexFormat.Float32x3, 2);
        // beware: the shaderLocation values have to match the shader
        vertexAttributes.end();

        // Note that the shapes we build will become meshParts of a single mesh.
        // This means we don't need to swap vertex buffer and index buffer between rendering these shapes.
        //
        // Also note that topology (triangles, lines) is defined per mesh part, not per mesh.
        //
        mb.begin(vertexAttributes, 16384, 16384);

        modelBox = buildCubeShape(mb);
        modelInstances.add( new ModelInstance(modelBox, 0,0,0) );

        modelPyramid = buildPyramid();
        modelInstances.add( new ModelInstance(modelPyramid, 3,0,0) );

        modelSphere = buildSphere(mb);
        modelInstances.add( new ModelInstance(modelSphere, 0,2,0) );

        modelBox2 = buildWireFrameCube(mb);
        modelInstances.add( new ModelInstance(modelBox2, 0,0,0) );
        mesh = mb.end();

        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(6, 4, -6);
        camera.direction.set(camera.position).scl(-1).nor();
        camera.update();

        camController = new CameraController(camera);
        LibGPU.input.setInputProcessor(camController);

        environment = new Environment();
        environment.add( new DirectionalLight( new Color(1,1,1,1), new Vector3(1f,-1,0)));
        environment.ambientLightLevel = 0.8f;

        camController = new CameraController(camera);
        LibGPU.input.setInputProcessor( camController );

        modelBatch = new ModelBatch();

        batch = new SpriteBatch();
        font = new BitmapFont();
    }

    /** demonstration of building a mesh without mesh builder or shape builder */
    private Model buildRawModel(){

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

        Material material = new Material( texture );

        return new Model(mesh, material);   // defaults to triangle list
    }

    /** demonstration of building a mesh without shape builder */
    private Model buildCube(){
        Vector3[] corners = {
                new Vector3(-1, 1, -1), new Vector3(1, 1, -1), new Vector3(1,-1,-1), new Vector3(-1, -1, -1),// front
                new Vector3(-1, 1,  1), new Vector3(1, 1,  1), new Vector3(1,-1, 1), new Vector3(-1, -1,  1),// back
        };
        Vector2[] texCoords = { new Vector2(0,0), new Vector2(1,0), new Vector2(1,1), new Vector2(0,1) };

        VertexAttributes vertexAttributes = new VertexAttributes();
        vertexAttributes.add(VertexAttribute.Usage.POSITION, "position", WGPUVertexFormat.Float32x4, 0);
        vertexAttributes.add(VertexAttribute.Usage.TEXTURE_COORDINATE,"uv", WGPUVertexFormat.Float32x2, 1);
        // beware: the shaderLocation values have to match the shader
        vertexAttributes.end();

        MeshBuilder mb = new MeshBuilder();
        mb.begin(vertexAttributes, 6*4, 8*6);
        MeshPart part = mb.part("box", WGPUPrimitiveTopology.TriangleList);

        mb.setNormal(0,0,-1);
        mb.addRect(corners[0], corners[3], corners[2], corners[1], texCoords[0], texCoords[3], texCoords[2], texCoords[1]); // front

        mb.setNormal(0,0,1);
        mb.addRect(corners[4], corners[5], corners[6], corners[7], texCoords[0], texCoords[1], texCoords[2], texCoords[3]); // back

        mb.setNormal(0,1,0);
        mb.addRect(corners[0], corners[1], corners[5], corners[4], texCoords[0], texCoords[1], texCoords[2], texCoords[3]); // top

        mb.setNormal(0,-1,0);
        mb.addRect(corners[3], corners[7], corners[6], corners[2], texCoords[0], texCoords[1], texCoords[2], texCoords[3]); // bottom

        mb.setNormal(-1,0,0);
        mb.addRect(corners[0], corners[4], corners[7], corners[3], texCoords[0], texCoords[1], texCoords[2], texCoords[3]); // left

        mb.setNormal(1,0,0);
        mb.addRect(corners[1], corners[2], corners[6], corners[5], texCoords[0], texCoords[1], texCoords[2], texCoords[3]); // right

        mb.end();

        Material material = new Material( texture );

        return new Model(part, material);
    }

    private Model buildPyramid(){
        Vector3[] corners = {
                new Vector3(-1, 0, 1), new Vector3(1, 0, 1), new Vector3(1,0,-1), new Vector3(-1, 0, -1),// base
                new Vector3(0, 2,  0) // top
        };
        //Color[] colors = { Color.BLUE, Color.GREEN, Color.YELLOW, Color.ORANGE, Color.RED };

        VertexAttributes vertexAttributes = new VertexAttributes();
        vertexAttributes.add(VertexAttribute.Usage.POSITION, "position", WGPUVertexFormat.Float32x4, 0);
        vertexAttributes.add(VertexAttribute.Usage.NORMAL, "normal", WGPUVertexFormat.Float32x3, 2);
        vertexAttributes.add(VertexAttribute.Usage.COLOR_PACKED,"color", WGPUVertexFormat.Unorm8x4, 5);
        // beware: the shaderLocation values have to match the shader
        vertexAttributes.end();

        MeshBuilder mb = new MeshBuilder();
        mb.begin(vertexAttributes, 18, 18);
        MeshPart part = mb.part("pyramid", WGPUPrimitiveTopology.TriangleList);

        mb.setNormal(0,-1,0);
        mb.setColor(Color.BLUE);
        mb.addRect(corners[0], corners[1], corners[2], corners[3]); // base, CCW from below

        // sides
        mb.setColor(Color.RED);
        mb.setNormal(0,1,1);
        mb.addTriangle(corners[0], corners[1], corners[4]);
        mb.setColor(Color.ORANGE);
        mb.setNormal(1,1,0);
        mb.addTriangle(corners[1], corners[2], corners[4]);
        mb.setColor(Color.YELLOW);
        mb.setNormal(0,1,-1);
        mb.addTriangle(corners[2], corners[3], corners[4]);
        mb.setNormal(-1,1,0);
        mb.addTriangle(corners[3], corners[0], corners[4]);

        mb.end();

        Material material = new Material( Color.WHITE );

        return new Model(part, material);
    }

    private Model buildCubeShape(MeshBuilder mb){

        MeshPart meshPart = BoxShapeBuilder.build(mb, 2, 2, 2,  WGPUPrimitiveTopology.TriangleList);
        Material material = new Material( texture );

        return new Model(meshPart, material);
    }

    private Model buildSphere(MeshBuilder mb){
        MeshPart meshPart = SphereShapeBuilder.build(mb, 1, 16,  WGPUPrimitiveTopology.TriangleStrip);
        Material material = new Material( texture );

        return new Model(meshPart, material);
    }

    private Model buildWireFrameCube(MeshBuilder mb){
        MeshPart meshPart = BoxShapeBuilder.build(mb, 4, 4, 4, WGPUPrimitiveTopology.LineList);
        Material material = new Material( Color.BLUE );

        return new Model(meshPart, material);
    }


    public void render( ){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){
            LibGPU.app.exit();
            return;
        }

        camController.update();

        ScreenUtils.clear(Color.TEAL);

        modelBatch.begin(camera, environment);
        modelBatch.render(modelInstances);
        modelBatch.end();

        batch.begin();
        font.draw(batch, "Demonstration of models built in code, e.g. using MeshBuilder rather than loaded from a file.", 10, 50);
        font.draw(batch, "draw calls "+modelBatch.drawCalls+" emitted: "+modelBatch.numEmitted+" pipelines: "+modelBatch.numPipelines, 10, 20);
        batch.end();
    }

    public void dispose(){
        // cleanup
        modelBox.dispose();
        modelSphere.dispose();
        modelBox2.dispose();
        modelPyramid.dispose();
        modelBatch.dispose();
        mesh.dispose();
        texture.dispose();
        batch.dispose();
        font.dispose();
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
        batch.getProjectionMatrix().setToOrtho2D(0,0,width, height);
    }


}

