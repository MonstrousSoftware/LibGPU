package com.monstrous;

import com.monstrous.graphics.BitmapFont;
import com.monstrous.graphics.Camera;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.PerspectiveCamera;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.g3d.Model;
import com.monstrous.graphics.g3d.ModelBatch;
import com.monstrous.graphics.g3d.ModelInstance;
import com.monstrous.graphics.lights.DirectionalLight;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;
import com.monstrous.scene2d.*;

import java.util.ArrayList;

// Test instancing
// For comparison to DuckField demo but is lacking shadows, PBR shading,etc.

public class TestDuckField extends ApplicationAdapter {

    private ModelBatch modelBatch;
    private Camera camera;
    private CameraController camController;
    private Model model;
    private ArrayList<ModelInstance> modelInstances;
    private Environment environment;
    private ArrayList<Matrix4> transforms;
    private BitmapFont font;
    private SpriteBatch batch;
    private String info;
//    private Stage stage;
    private int fps;
    private long startTime;
    private int frames;
    private final Vector3 up = new Vector3(0,1,0);

    public void create() {
        startTime = System.nanoTime();
        frames = 0;

        model = new Model("models/Ducky/ducky.glb");
        modelInstances = new ArrayList<>();

        transforms = makeTransforms();
        ModelInstance modelInstance = new ModelInstance(model, transforms);
        modelInstances.add(modelInstance);

        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(0, 1, -3);
        camera.direction.set(0,0f, 1f);
        camera.far = 1000f;
        camera.update();

        environment = new Environment();
        DirectionalLight light = new DirectionalLight( Color.WHITE, new Vector3(.4f,-1,.2f));
        light.setIntensity(5f);
        environment.add( light );
        environment.ambientLightLevel = 0.4f;

        modelBatch = new ModelBatch();

        batch = new SpriteBatch();
        font = new BitmapFont();
        info = "Number of instances: "+transforms.size();

//        stage = new Stage();
//        Table sliderTable = new Table();
//        WrappedFloat side = new WrappedFloat(15);
//        Slider slider = new Slider(side, 0, 360, 5f);
//
//        Label.Style style = new Label.Style();
//        style.font = font;
//        style.fontColor = Color.WHITE;
//        FloatLabel value = new FloatLabel( side, "count:", style );
//        sliderTable.add(slider);
//        sliderTable.row();
//        sliderTable.add(value);
//        stage.add(sliderTable).setAlign(Align.topRight);


        camController = new CameraController(camera);
//        InputMultiplexer im= new InputMultiplexer();
//        im.addProcessor(stage);
//        im.addProcessor(camController);
        LibGPU.input.setInputProcessor(camController);
    }

    private ArrayList<Matrix4> makeTransforms(){
        ArrayList<Matrix4> transforms = new ArrayList<>();
        float N = 20;
        float x = -N;

        for(int i = 0; i < N; i++, x += 2f) {
            float z = -N;
            for (int j = 0; j < N; j++, z += 2f) {
                transforms.add(new Matrix4().translate(x, 0, z).scale(new Vector3( 1f,1f+.3f*(float)Math.sin(x-z), 1f)).rotate(up, 30f * x + 20f * z));
            }
        }
        System.out.println("Instances: "+transforms.size());
        return transforms;
    }



    private void rotate(ArrayList<Matrix4> transforms, float deltaTime){
        for(Matrix4 transform : transforms)
            transform.rotate(up, 30f*deltaTime);
    }



    public void render( ){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE))
            LibGPU.app.exit();
        camController.update();

        rotate(transforms, LibGPU.graphics.getDeltaTime());



        modelBatch.begin(camera, environment, Color.GRAY);
        modelBatch.render(modelInstances);
        modelBatch.end();

        batch.begin(null);
        font.draw(batch, info, 10, 70);
        font.draw(batch, "frames per second: "+fps, 10, 50);
        batch.end();

//        stage.draw();

        // At the end of the frame
        if (System.nanoTime() - startTime > 1000000000) {
            System.out.println("SpriteBatch : fps: " + frames +" instances: "+ transforms.size() );
            fps = frames;
            frames = 0;
            startTime = System.nanoTime();
        }
        frames++;
    }

    public void dispose(){
        // cleanup
        model.dispose();
        modelBatch.dispose();
        batch.dispose();
        font.dispose();
//        stage.dispose();
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
        //stage.resize(width, height);
    }


}

