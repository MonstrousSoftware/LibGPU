package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.g3d.*;
import com.monstrous.graphics.g3d.shapeBuilder.BoxShapeBuilder;
import com.monstrous.graphics.lights.DirectionalLight;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.graphics.webgpu.RenderPassType;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;
import com.monstrous.webgpu.WGPUTextureFormat;
import com.monstrous.webgpu.WGPUVertexFormat;

import java.util.ArrayList;

public class TestSponza extends ApplicationAdapter {

    private static boolean WITH_SHADOWS = true;
    private static boolean WITH_Z_PREPASS = false;

    private static Color bgColor = new Color(178f/255f, 204f/255f, 1f, 1);
    private static int SHADOW_MAP_SIZE = 4096;      // size (in pixels) of depth map
    private static int SHADOW_VIEWPORT_SIZE = 25;   // area (in world units) covered by shadow


    private ModelBatch modelBatch;
    private Camera camera;
    private Environment environment;
    private Matrix4 modelMatrix;
    private Model model;
    private ModelInstance modelInstance1;
    private ArrayList<ModelInstance> instances;
    private ArrayList<ModelInstance> boxes;
    private long startTime;
    private int frames;
    private SpriteBatch batch;
    private BitmapFont font;
    private String status;
    private CameraController camController;
    private OrthographicCamera shadowCam;
    private Texture colorMap, depthMap;
    private boolean userControl = true;
    private int frame;

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        instances = new ArrayList<>();

        long startLoad = System.currentTimeMillis();

        model = new Model("models/Sponza/Sponza.gltf");
       // model = new Model("models/Ducky/ducky.gltf");

        long endLoad = System.currentTimeMillis();
        System.out.println("Model loading time (ms): "+(endLoad - startLoad));

        modelMatrix = new Matrix4();
        modelInstance1 = new ModelInstance(model, modelMatrix);
        instances.add(modelInstance1);

        buildBoxes();


        camera = new PerspectiveCamera(120, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 800f;
        camera.position.set(6.7f, 5, -1.6f);

        camera.direction.set(-0.99f,0f, 0.1f).nor();
        camera.update();

        DirectionalLight sun = new DirectionalLight(Color.WHITE, new Vector3(0,-1f,0));
        sun.direction.set(-1, -3, 0.5f).nor();
        sun.intensity = 6;
        environment = new Environment();
        environment.add(sun);
        environment.ambientLightLevel = 0.4f;

        shadowCam = new OrthographicCamera(SHADOW_VIEWPORT_SIZE, SHADOW_VIEWPORT_SIZE); // in world units

        shadowCam.direction.set(sun.direction);
        shadowCam.position.set(shadowCam.direction).scl(-50);
        shadowCam.up.set(0,0,1);
        shadowCam.near = 0f;
        shadowCam.far = 100f;
        shadowCam.zoom = 1f;
        shadowCam.update();

        colorMap = new Texture(SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, false, true, WGPUTextureFormat.RGBA8Unorm, 1);
        depthMap = new Texture(SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, false, true, WGPUTextureFormat.Depth32Float, 1);

        camController = new CameraController(camera, new Vector3(0,5,0));
        //camController.setPivotPoint(new Vector3(0,0,0));
        LibGPU.input.setInputProcessor( camController );
        userControl = true;

        modelBatch = new ModelBatch();

        batch = new SpriteBatch();
        font = new BitmapFont();
        status = "...";

        frame = 0;
    }

    public void render(){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){
            LibGPU.app.exit();
        }
        if(LibGPU.input.isKeyPressed(Input.Keys.NUM_1)){
            userControl = false;
            camera.position.set(6.7f, 5, -1.6f);
            camera.direction.set(-0.99f,0f, 0.1f).nor();
            camera.up.set(0,1,0);
            camera.update();
        } else  if(LibGPU.input.isKeyPressed(Input.Keys.NUM_2)){
            userControl = true;
        }

        if(userControl)
            camController.update();
        camera.update();

        if(WITH_SHADOWS) {
            // pass #1 : depth map
            environment.depthPass = true;
            environment.renderShadows = false;
            environment.setShadowMap(shadowCam, null);

            modelBatch.begin(shadowCam, environment, Color.WHITE, colorMap, depthMap, RenderPassType.SHADOW_PASS);
            modelBatch.render(instances);
            modelBatch.end();
            environment.setShadowMap(shadowCam, depthMap);
        }

        if(WITH_Z_PREPASS) {
            // pass #2a : depth pre-pass
            environment.depthPass = true;
            environment.renderShadows = false;

            modelBatch.begin(camera, environment, null, null, null, RenderPassType.DEPTH_PREPASS);
            modelBatch.render(instances);
            modelBatch.end();
        }


        if(WITH_SHADOWS) {
            // pass #2 : render colours
            environment.renderShadows = true;
        }

        environment.depthPass = false;

        modelBatch.begin(camera, environment, bgColor, null, null, WITH_Z_PREPASS ? RenderPassType.COLOR_PASS_AFTER_DEPTH_PREPASS : RenderPassType.COLOR_PASS);
        modelBatch.render(instances);
        //modelBatch.render(boxes);
        modelBatch.end();

        // text
        batch.begin();
        font.draw(batch, status, 10, 50);
        //font.draw(batch, "cam at :"+camera.position.toString()+" dir:"+camera.direction.toString(), 10, 100);
        batch.end();


        // At the end of the frame

        if (System.nanoTime() - startTime > 1000000000) {
            status = "fps: " + frames + " GPU: "+(int)LibGPU.app.getAverageGPUtime()+" microseconds pipelines: "+modelBatch.numPipelines+
                    " pipe switches: "+modelBatch.numPipelineSwitches +
                    " material switches: "+modelBatch.materialSwitches +
                    "draw calls: "+modelBatch.drawCalls +
                    " emitted: "+modelBatch.numEmitted+" instance-joined: "+modelBatch.instancingJoins;
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
        batch.getProjectionMatrix().setToOrtho2D(0,0,width, height);
    }

    private void buildBoxes(){
        boxes = new ArrayList<>();

        MeshBuilder mb = new MeshBuilder();
        VertexAttributes vertexAttributes = new VertexAttributes();
        vertexAttributes.add(VertexAttribute.Usage.POSITION, "position", WGPUVertexFormat.Float32x4, 0);
        vertexAttributes.add(VertexAttribute.Usage.TEXTURE_COORDINATE, "uv", WGPUVertexFormat.Float32x2, 1);
        vertexAttributes.add(VertexAttribute.Usage.NORMAL, "normal", WGPUVertexFormat.Float32x3, 2);
        // beware: the shaderLocation values have to match the shader
        vertexAttributes.end();

        mb.begin(vertexAttributes, 32000, 32000);

        for(ModelInstance instance : instances){
            Node rootNode = instance.model.rootNodes.get(0);
            for(NodePart part : rootNode.nodeParts){

                BoundingBox bb = new BoundingBox(part.meshPart.getMesh().boundingBox);
                bb.transform(rootNode.globalTransform);
                MeshPart meshPart = BoxShapeBuilder.build(mb, bb);
                Model model = new Model(meshPart, new Material(Color.GREEN));   // todo dispose
                boxes.add(new ModelInstance(model, instance.transform));
            }
        }
    }


}

