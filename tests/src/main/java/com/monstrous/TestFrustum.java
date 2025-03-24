package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.g3d.Mesh;
import com.monstrous.graphics.g3d.Model;
import com.monstrous.graphics.g3d.ModelBatch;
import com.monstrous.graphics.g3d.ModelInstance;
import com.monstrous.graphics.g3d.shapeBuilder.BoxShapeBuilder;
import com.monstrous.graphics.g3d.shapeBuilder.FrustumShapeBuilder;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;
import com.monstrous.utils.ScreenUtils;

import java.util.ArrayList;

/** Visualizes frustum culling using bounding boxes per model instance.
 *  The demo shows a green wireframe camera view (a truncated pyramid) rotating around.
 *  Boxes that fall within the view change to green, boxes out of view are shown in grey.
 */

public class TestFrustum extends ApplicationAdapter {

    private ModelBatch modelBatch;
    private Camera camera;
    private PerspectiveCamera subCam;
    private Matrix4 subCamMatrix;
    private Model frustumModel;
    private Model invisibleBlockModel;
    private Model visibleBlockModel;
    private Model bigBlockModel;
    private ModelInstance frustum;
    private ModelInstance bigBlock;
    private ArrayList<ModelInstance> instances;
    private Environment environment;
    private CameraController camController;
    private float time;
    private SpriteBatch spriteBatch;
    private BitmapFont font;
    private int visibleCount;

    public void create() {

        Vector3 worldUp = new Vector3(0,1,0);

        // camera used to demonstrate frustum
        subCam = new PerspectiveCamera(50, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        subCam.position.set(0,0,0);
        subCam.direction.set(0,0,1);
        Vector3 tmpRight = new Vector3().set(subCam.direction).crs(worldUp);
        subCam.up.set(tmpRight).crs(subCam.direction).nor();
        subCam.near = 2;
        subCam.far = 10;
        subCam.update();
        time = 0;

        frustumModel = buildFrustumModel(subCam);
        instances = new ArrayList<>();
        subCamMatrix = new Matrix4();


        // "outside" camera
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

        invisibleBlockModel = new Model(BoxShapeBuilder.build(0.1f, 0.1f, 0.1f), new Material(Color.BLUE));
        visibleBlockModel = new Model(BoxShapeBuilder.build(0.1f, 0.1f, 0.1f), new Material(Color.GREEN));
        bigBlockModel = new Model(BoxShapeBuilder.build(1f, 1f, 1f), new Material(Color.ORANGE));

        populate();

        spriteBatch = new SpriteBatch();
        font = new BitmapFont();

    }

    private void populate(){
        instances.clear();
        frustum = new ModelInstance(frustumModel, subCamMatrix);
        bigBlock = new ModelInstance(bigBlockModel, subCamMatrix);

        for(int x = -20; x <= 20; x+=1){
            for(int z = -20; z <= 20; z += 1){
                Matrix4 transform = new Matrix4().idt().setToTranslation(x, 0, z);
                ModelInstance instance = new ModelInstance(invisibleBlockModel, transform);
                instances.add( instance );
            }
        }
        for(int y = -10; y <= 10; y+=1){
            for(int z = -15; z <= 15; z += 1){
                Matrix4 transform = new Matrix4().idt().setToTranslation(0, y, z);
                instances.add( new ModelInstance(invisibleBlockModel, transform));
            }
        }
    }

    private void cull(){
        visibleCount = 0;

        for(ModelInstance instance : instances ){
            // swap out the model between a blue and a green block to show which are "visible"
            if(subCam.frustum.boundsInFrustum(instance.boundingBox)) {
                instance.model = visibleBlockModel;
                visibleCount++;
            }
            else
                instance.model = invisibleBlockModel;
        }

    }



    private Model buildFrustumModel(PerspectiveCamera cam) {
        Mesh mesh = FrustumShapeBuilder.build(cam.frustum);
        Material material = new Material( Color.GREEN );
        return new Model(mesh, material);
    }


    private Model buildModel(float size){
        Mesh mesh = BoxShapeBuilder.build(size, size, size);
        Material material = new Material( Color.BLUE );
        return new Model(mesh, material);
    }

    public void render( ){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE))
            LibGPU.app.exit();

        time += 10*LibGPU.graphics.getDeltaTime();
        float angle = time*(float)Math.PI/180f;
        float yangle = 0;
        subCam.direction.set((float)Math.sin(angle)*(float)Math.cos(yangle),(float)Math.sin(yangle),(float)Math.cos(angle)*(float)Math.cos(yangle));
        subCam.update();
        subCamMatrix.setToYRotation(-angle);

        camController.update();
        cull();

        ScreenUtils.clear(Color.TEAL);

        modelBatch.begin(camera, environment);
        modelBatch.render(bigBlock);
        modelBatch.render(frustum);
        modelBatch.render(instances);
        modelBatch.end();

        spriteBatch.begin();
        font.draw(spriteBatch, "visible/modelInstances: "+visibleCount+"/"+instances.size(), 10, 50);
        spriteBatch.end();
    }

    public void dispose(){
        // cleanup
        frustumModel.dispose();
        modelBatch.dispose();
        spriteBatch.dispose();
        font.dispose();
        invisibleBlockModel.dispose();
        bigBlockModel.dispose();
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }


}

