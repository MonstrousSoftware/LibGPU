package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.math.Matrix4;
import com.monstrous.wgpu.*;
import com.monstrous.wgpuUtils.WgpuJava;
import jnr.ffi.Pointer;


public class Demo implements ApplicationListener {
    private WGPU wgpu;

    private Mesh mesh;
    private ModelBatch modelBatch;

    private Camera camera;

    private Texture texture;
    private Texture texture2;
    private Matrix4 modelMatrix;
    private Texture textureFont;
    private float currentTime;
    private SpriteBatch batch;
    private long startTime;
    private int frames;

    public void create() {

        startTime = System.nanoTime();
        frames = 0;

        wgpu = LibGPU.wgpu;

        mesh = new Mesh("pyramid.txt");

        texture = new Texture("monstrous.png", false);
        texture2 = new Texture("jackRussel.png", false);
        textureFont = new Texture("lsans-15.png", false);

        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(0, 1, -3);
        camera.direction.set(0,0f, 1f);
        camera.update();

        LibGPU.input.setInputProcessor(new CameraController(camera));


        modelMatrix = new Matrix4();

        batch = new SpriteBatch();

        modelBatch = new ModelBatch();

    }




    private void updateModelMatrix(float currentTime){
        Matrix4 RT = new Matrix4().setToXRotation((float) ( -0.5f*Math.PI ));
        Matrix4 R1 = new Matrix4().setToYRotation(currentTime*0.6f);
        Matrix4 T = new Matrix4().translate(0.8f, 0f, 0f);
        modelMatrix.idt().mul(R1).mul(T).mul(RT);
    }





    public void render( float deltaTime ){
        currentTime += deltaTime;

        WGPUCommandEncoderDescriptor encoderDescriptor = WGPUCommandEncoderDescriptor.createDirect();
        encoderDescriptor.setNextInChain();
        encoderDescriptor.setLabel("My Encoder");

        Pointer encoder = wgpu.DeviceCreateCommandEncoder(LibGPU.device, encoderDescriptor);

        WGPURenderPassColorAttachment renderPassColorAttachment = WGPURenderPassColorAttachment.createDirect();
        renderPassColorAttachment.setNextInChain();
        renderPassColorAttachment.setView(LibGPU.application.targetView);
        renderPassColorAttachment.setResolveTarget(WgpuJava.createNullPointer());
        renderPassColorAttachment.setLoadOp(WGPULoadOp.Clear);
        renderPassColorAttachment.setStoreOp(WGPUStoreOp.Store);

        renderPassColorAttachment.getClearValue().setR(0.25);
        renderPassColorAttachment.getClearValue().setG(0.25);
        renderPassColorAttachment.getClearValue().setB(0.25);
        renderPassColorAttachment.getClearValue().setA(1.0);

        renderPassColorAttachment.setDepthSlice(wgpu.WGPU_DEPTH_SLICE_UNDEFINED);


        WGPURenderPassDepthStencilAttachment depthStencilAttachment = WGPURenderPassDepthStencilAttachment.createDirect();
        depthStencilAttachment.setView( LibGPU.application.depthTextureView );
        depthStencilAttachment.setDepthClearValue(1.0f);
        depthStencilAttachment.setDepthLoadOp(WGPULoadOp.Clear);
        depthStencilAttachment.setDepthStoreOp(WGPUStoreOp.Store);
        depthStencilAttachment.setDepthReadOnly(0L);
        depthStencilAttachment.setStencilClearValue(0);
        depthStencilAttachment.setStencilLoadOp(WGPULoadOp.Undefined);
        depthStencilAttachment.setStencilStoreOp(WGPUStoreOp.Undefined);
        depthStencilAttachment.setStencilReadOnly(1L);



        WGPURenderPassDescriptor renderPassDescriptor = WGPURenderPassDescriptor.createDirect();
        renderPassDescriptor.setNextInChain();

        renderPassDescriptor.setLabel("Main Render Pass");

        renderPassDescriptor.setColorAttachmentCount(1);
        renderPassDescriptor.setColorAttachments( renderPassColorAttachment );
        renderPassDescriptor.setOcclusionQuerySet(WgpuJava.createNullPointer());
        renderPassDescriptor.setDepthStencilAttachment( depthStencilAttachment );
        renderPassDescriptor.setTimestampWrites();


        Pointer renderPass = wgpu.CommandEncoderBeginRenderPass(encoder, renderPassDescriptor);
// [...] Use Render Pass

        boolean testSprites = false;
        if(testSprites) {


            // SpriteBatch testing
            batch.begin(renderPass);    // todo param for now
//char id=65 x=80 y=33 width=11 height=13 xoffset=-1 yoffset=2 xadvance=9 page=0 chnl=0

//        TextureRegion letterA = new TextureRegion(textureFont, 80f/256f, (33f+13f)/128f, (80+11f)/256f, 33f/128f);
//        batch.draw(letterA, 100, 100);

            batch.setColor(1, 0, 0, 0.1f);
            batch.draw(texture, 0, 0, 100, 100);

            batch.draw(texture, 0, 0, 300, 300, 0.5f, 0.5f, 0.9f, 0.1f);
            batch.draw(texture, 300, 300, 50, 50);
            batch.setColor(1, 1, 1, 1);

            batch.draw(texture2, 400, 100, 100, 100);

            TextureRegion region = new TextureRegion(texture2, 0, 0, 512, 512);
            batch.draw(region, 200, 300, 64, 64);

            TextureRegion region2 = new TextureRegion(texture2, 0f, 1f, .5f, 0.5f);
            batch.draw(region2, 400, 300, 64, 64);

            int W = LibGPU.graphics.getWidth();
            int H = LibGPU.graphics.getHeight();
            batch.setColor(0, 1, 0, 1);
            for (int i = 0; i < 8000; i++) {
                batch.draw(texture2, (int) (Math.random() * W), (int) (Math.random() * H), 32, 32);
            }
            batch.end();
        }
        else {

            updateModelMatrix(currentTime);

            modelBatch.begin(camera, renderPass);

            modelBatch.render(mesh, texture2, modelMatrix);

            modelBatch.end();
        }
        wgpu.RenderPassEncoderEnd(renderPass);
        wgpu.RenderPassEncoderRelease(renderPass);

        WGPUCommandBufferDescriptor bufferDescriptor =  WGPUCommandBufferDescriptor.createDirect();
        bufferDescriptor.setNextInChain();
        bufferDescriptor.setLabel("Command Buffer");
        Pointer commandBuffer = wgpu.CommandEncoderFinish(encoder, bufferDescriptor);
        wgpu.CommandEncoderRelease(encoder);


        long[] buffers = new long[1];
        buffers[0] = commandBuffer.address();
        Pointer bufferPtr = WgpuJava.createLongArrayPointer(buffers);
        //System.out.println("Pointer: "+bufferPtr.toString());
        //System.out.println("Submitting command...");
        wgpu.QueueSubmit(LibGPU.queue, 1, bufferPtr);

        wgpu.CommandBufferRelease(commandBuffer);
        //System.out.println("Command submitted...");


        // At the end of the frame


        if (System.nanoTime() - startTime > 1000000000) {
            System.out.println("SpriteBatch : fps: " + frames  );
            frames = 0;
            startTime = System.nanoTime();
        }
        frames++;

    }

    public void dispose(){
        // cleanup
        System.out.println("demo exit");
        texture.dispose();
        texture2.dispose();
        batch.dispose();
        mesh.dispose();
        modelBatch.dispose();
    }

    @Override
    public void resize(int width, int height) {
        System.out.println("demo got resize");
    }


}
