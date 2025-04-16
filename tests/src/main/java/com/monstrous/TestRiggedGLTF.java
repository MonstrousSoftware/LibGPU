package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.g3d.*;
import com.monstrous.graphics.g3d.shapeBuilder.BoxShapeBuilder;
import com.monstrous.graphics.lights.DirectionalLight;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;
import com.monstrous.webgpu.WGPUPrimitiveTopology;

import java.util.ArrayList;

// Demonstration of skeletal animation from a GLTF file.

// To test on more examples models, they don't all work.


public class TestRiggedGLTF extends ApplicationAdapter {

    private ModelBatch modelBatch;
    private Camera camera;
    private Environment environment;
    private Model model;
    private Model modelFloor;
    private Model modelBlock;
    private ModelInstance modelInstance1;
    private ModelInstance modelInstance2;
    private ArrayList<ModelInstance> instances;
    private ArrayList<ModelInstance> bones;
    private SpriteBatch batch;
    private BitmapFont font;
    private CameraController camController;
    private AnimationController animController, animController2;
    private boolean showBones = true;

    public void create() {
        instances = new ArrayList<>();
        bones = new ArrayList<>();

        model = new Model("models/RiggedFigure/RiggedFigure.gltf");
        //model = new Model("models/RiggedSimple/RiggedSimple.gltf");

        modelInstance1 = new ModelInstance(model);
        instances.add(modelInstance1);

//        modelInstance2 = new ModelInstance(model, 3, 1, 0);
//        instances.add(modelInstance2);


//        modelFloor = buildFloor();
//        instances.add(new ModelInstance(modelFloor, 0, -0.5f, 0));

        // reproduce bind pose by inverting the inverse transform
        modelBlock = buildBlock();
        Matrix4 tmpMat = new Matrix4();
        for(Matrix4 mat : model.inverseBoneTransforms){
            tmpMat.set(mat);
            tmpMat.inv();
            bones.add(new ModelInstance(modelBlock, tmpMat));
        }

        // show current transform of bones
        for(Node node : model.joints){
            bones.add(new ModelInstance(modelBlock, node.globalTransform));
        }

        animController = new AnimationController(modelInstance1);
        animController.setAnimation(-1, 1.0f);
//
//        animController2 = new AnimationController(modelInstance2);
//        animController2.setAnimation(-1, 1.0f);


        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(3, 1f, -3);
        camera.direction.set(camera.position).scl(-1);
        camera.far = 200f;
        camera.near = 0.001f;
        camera.update();

        environment = new Environment();

        DirectionalLight sun = new DirectionalLight( Color.WHITE, new Vector3(.6f,-1,.3f).nor());
        environment.add( sun );
        environment.ambientLightLevel = 0.7f;

        camController = new CameraController(camera);
        camController.setPivotPoint(new Vector3(0,1,0));
        LibGPU.input.setInputProcessor( camController );

        modelBatch = new ModelBatch();

        batch = new SpriteBatch();
        font = new BitmapFont();
    }


    public void render(){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){
            LibGPU.app.exit();
        }
        float deltaTime =  LibGPU.graphics.getDeltaTime();
        if(LibGPU.input.isKeyPressed(Input.Keys.SPACE)){
            deltaTime =  0.f;
        }
        if(LibGPU.input.isKeyPressed(Input.Keys.NUM_1)){
            showBones = true;
        }
        if(LibGPU.input.isKeyPressed(Input.Keys.NUM_2)){
            showBones = false;
        }

        AnimationController.AnimationDesc desc = animController.update(deltaTime);
//        animController2.update(deltaTime);
        camController.update();
        modelInstance1.update();


        modelBatch.begin(camera, environment, Color.TEAL);
        modelBatch.render(instances);
        modelBatch.end();

        // overlay to show bones (not depth masked)
        if(showBones) {
            modelBatch.begin(camera, environment, null);
            modelBatch.render(bones);
            modelBatch.end();
        }

        batch.begin();
        font.draw(batch, "Skeletal Animation of GLTF file" , 10, 180);
        font.draw(batch, "1 to show bones, 2 to hide, SPACE to freeze animation " , 10, 150);
//        if(desc != null) {
//            font.draw(batch, "animation time: " + desc.time, 10, 120);
//        }
        batch.end();
    }

    public void dispose(){
        // cleanup
        model.dispose();
        //modelFloor.dispose();
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


    private Model buildFloor(){
        MeshBuilder mb = new MeshBuilder();

        VertexAttributes vertexAttributes = new VertexAttributes(VertexAttribute.Usage.POSITION|VertexAttribute.Usage.TEXTURE_COORDINATE|VertexAttribute.Usage.NORMAL);

        mb.begin(vertexAttributes, 100, 100);
        MeshPart meshPart = BoxShapeBuilder.build(mb, 10f, 1f, 10f,  WGPUPrimitiveTopology.TriangleList);
        Material material = new Material( Color.GREEN );
        mb.end();

        return new Model(meshPart, material);
    }

    private Model buildBlock(){
        MeshBuilder mb = new MeshBuilder();

        VertexAttributes vertexAttributes = new VertexAttributes(VertexAttribute.Usage.POSITION|VertexAttribute.Usage.TEXTURE_COORDINATE|VertexAttribute.Usage.NORMAL);

        mb.begin(vertexAttributes, 100, 100);
        float size = 0.05f;
        MeshPart meshPart = BoxShapeBuilder.build(mb, size, size, size,  WGPUPrimitiveTopology.TriangleList);
        Material material = new Material( Color.BLUE );
        mb.end();

        return new Model(meshPart, material);
    }

}

