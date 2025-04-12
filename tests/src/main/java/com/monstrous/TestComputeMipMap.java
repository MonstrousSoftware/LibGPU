package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.webgpu.*;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.webgpu.*;
import jnr.ffi.Pointer;


public class TestComputeMipMap extends ApplicationAdapter {

    private SpriteBatch batch;
    private BitmapFont font;
    private ShaderProgram computeShader;
    private Texture texture;
    private int mipLevels;
    private TextureView[] textureViews;
    private Pointer pipeline;
    private WebGPU_JNI webGPU;

    @Override
    public void create() {
        webGPU = LibGPU.webGPU;
        batch = new SpriteBatch();
        font = new BitmapFont();

        int textureUsage = WGPUTextureUsage.TextureBinding | WGPUTextureUsage.StorageBinding|WGPUTextureUsage.CopyDst | WGPUTextureUsage.CopySrc;
        mipLevels = 8;
        texture = new Texture(256, 256, mipLevels, textureUsage, WGPUTextureFormat.RGBA8Unorm, 1);


        // load image file into first mip level
        FileHandle file = Files.internal("textures/jackRussel256.png");
        byte[] byteArray = file.readAllBytes();
        Pointer data = JavaWebGPU.createByteArrayPointer(byteArray);
        Pointer image = JavaWebGPU.getUtils().gdx2d_load(data, byteArray.length);        // use native function to parse image file
        PixmapInfo info = PixmapInfo.createAt(image);
        Pointer pixelPtr = info.pixels.get();
        texture.load(pixelPtr, 0);

        // create separate texture views per mip level
        textureViews = new TextureView[mipLevels];
        for(int mip = 0; mip < mipLevels; mip++)
            textureViews[mip] =  new TextureView(texture, WGPUTextureAspect.All, WGPUTextureViewDimension._2D, texture.getFormat(), mip, 1, 0, 1 );

        onCompute();
    }

    @Override
    public void render(){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){
            LibGPU.app.exit();
            return;
        }


        batch.begin(Color.TEAL);
        batch.draw(texture, 0, 0, 256, 256);
        batch.draw(texture, 256, 0, 128, 128);
        batch.draw(texture, 256+128, 0, 64, 64);
        batch.draw(texture, 256+128+64, 0, 32, 32);
        batch.draw(texture, 256+128+64+32, 0, 16, 16);
        batch.draw(texture, 256+128+64+32+16, 0, 8, 8);
        batch.draw(texture, 256+128+64+32+16+8, 0, 4, 4);
        batch.draw(texture, 256+128+64+32+16+8+4, 0, 2, 2);

        font.draw(batch, "Mip maps were computed by a compute shader", 10, LibGPU.graphics.getHeight()-50);
        batch.end();
    }

    @Override
    public void dispose(){
        // cleanup
        for(int mip = 0; mip < mipLevels; mip++)
            textureViews[mip].dispose();
        texture.dispose();
        batch.dispose();
        font.dispose();
    }

    @Override
    public void resize(int width, int height) {

        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }

    private void onCompute() {


        // compute shader
        computeShader = new ShaderProgram(Files.internal("shaders/compute-mipmap.wgsl"));

        // make a pipeline
        BindGroupLayout bindGroupLayout = makeBindGroupLayout();
        PipelineLayout pipelineLayout = new PipelineLayout("compute pipeline layout", bindGroupLayout);
        pipeline = makeComputePipeline(computeShader, pipelineLayout);

        compute2(bindGroupLayout);

        // cleanup
        webGPU.wgpuComputePipelineRelease(pipeline);
        pipelineLayout.dispose();
        computeShader.dispose();
        bindGroupLayout.dispose();

        for(int mip = 1; mip < mipLevels; mip++) {
            ImageSave.saveToPNG("mip-"+mip+".png", texture, mip);
        }
    }


    // @group(0) @binding(0) var previousMipLevel: texture_2d<f32>;
    // @group(0) @binding(1) var nextMipLevel: texture_storage_2d<rgba8unorm,write>;
    //
    private BindGroupLayout makeBindGroupLayout(){
        // create bind group layout
        BindGroupLayout bindGroupLayout = new BindGroupLayout();
        bindGroupLayout.begin();
        bindGroupLayout.addTexture(0, WGPUShaderStage.Compute, WGPUTextureSampleType.Float, WGPUTextureViewDimension._2D, false);
        bindGroupLayout.addStorageTexture(1, WGPUShaderStage.Compute, WGPUStorageTextureAccess.WriteOnly, WGPUTextureFormat.RGBA8Unorm, WGPUTextureViewDimension._2D);
        bindGroupLayout.end();

        return bindGroupLayout;
    }

    private BindGroup makeBindGroup(BindGroupLayout bindGroupLayout, int nextMipLevel){
        // create bind group
        BindGroup bindGroup = new BindGroup(bindGroupLayout);
        bindGroup.addTexture(0, textureViews[nextMipLevel-1]);
        bindGroup.addTexture(1, textureViews[nextMipLevel]);
        bindGroup.end();
        return bindGroup;
    }

    private Pointer makeComputePipeline(ShaderProgram shader, PipelineLayout pipelineLayout){

        WGPUComputePipelineDescriptor pipelineDescriptor = WGPUComputePipelineDescriptor.createDirect();
        pipelineDescriptor.setNextInChain();
        pipelineDescriptor.getCompute().setConstantCount(0);
        pipelineDescriptor.getCompute().setConstants();
        pipelineDescriptor.getCompute().setEntryPoint("computeMipMap");
        pipelineDescriptor.getCompute().setModule(shader.getHandle());
        pipelineDescriptor.setLayout(pipelineLayout.getHandle());

        return LibGPU.webGPU.wgpuDeviceCreateComputePipeline(LibGPU.device.getHandle(), pipelineDescriptor);
    }

//    private void compute(BindGroupLayout bindGroupLayout) {
//
//        // create a command encoder
//        WGPUCommandEncoderDescriptor encoderDesc = WGPUCommandEncoderDescriptor.createDirect();
//        encoderDesc.setNextInChain();
//        Pointer encoder = webGPU.wgpuDeviceCreateCommandEncoder(LibGPU.device, encoderDesc);
//
//        // Create a compute pass
//        WGPUComputePassDescriptor passDesc = WGPUComputePassDescriptor.createDirect();
//        passDesc.setNextInChain();
//        passDesc.setTimestampWrites();
//        Pointer computePass = webGPU.wgpuCommandEncoderBeginComputePass(encoder, passDesc);
//
//        // set pipeline
//        webGPU.wgpuComputePassEncoderSetPipeline(computePass, pipeline);
//        int width = texture.getWidth();
//        int height = texture.getHeight();
//        for(int mip = 1; mip < mipLevels; mip++) {
//
//            BindGroup bindGroup = makeBindGroup(bindGroupLayout, mip);
//            // set bind group
//            webGPU.wgpuComputePassEncoderSetBindGroup(computePass, 0, bindGroup.getHandle(), 0, JavaWebGPU.createNullPointer());
//
//            width /= 2;
//            height /= 2;
//            // use one thread per texel of the output, i.e. half the size of the input texture
//            int invocationCountX = width;
//            int invocationCountY = height;
//
//            int workgroupSizePerDim = 8;
//            // This ceils invocationCountX / workgroupSizePerDim
//            int workgroupCountX = (invocationCountX + workgroupSizePerDim - 1) / workgroupSizePerDim;
//            int workgroupCountY = (invocationCountY + workgroupSizePerDim - 1) / workgroupSizePerDim;
//
//            // dispatch workgroups
//            webGPU.wgpuComputePassEncoderDispatchWorkgroups(computePass, workgroupCountX, workgroupCountY, 1);
//            bindGroup.dispose();
//        }
//        webGPU.wgpuComputePassEncoderEnd(computePass);
//
//        // finish the encoder to give use command buffer
//        WGPUCommandBufferDescriptor bufferDescriptor = WGPUCommandBufferDescriptor.createDirect();
//        bufferDescriptor.setNextInChain();
//        Pointer commandBuffer = webGPU.wgpuCommandEncoderFinish(encoder, bufferDescriptor);
//        webGPU.wgpuCommandEncoderRelease(encoder);
//
//        // feed the command buffer to the queue
//
//        long[] buffers = new long[1];
//        buffers[0] = commandBuffer.address();
//        Pointer bufferPtr = JavaWebGPU.createLongArrayPointer(buffers);
//        webGPU.wgpuQueueSubmit(LibGPU.queue, 1, bufferPtr);
//
//        webGPU.wgpuCommandBufferRelease(commandBuffer);
//        //webGPU.wgpuComputePassEncoderRelease(computePass); // causes crash
//    }

    private void compute2(BindGroupLayout bindGroupLayout) {

        Queue queue = new Queue(LibGPU.device);

        // create a command encoder
        CommandEncoder encoder = new CommandEncoder(LibGPU.device);

        ComputePass pass = encoder.beginComputePass();

        // set pipeline
        pass.setPipeline(pipeline);

        int width = texture.getWidth();
        int height = texture.getHeight();
        for(int mip = 1; mip < mipLevels; mip++) {

            BindGroup bindGroup = makeBindGroup(bindGroupLayout, mip);
            // set bind group
            pass.setBindGroup(0, bindGroup);

            width /= 2;
            height /= 2;
            // use one thread per texel of the output, i.e. half the size of the input texture
            int invocationCountX = width;
            int invocationCountY = height;

            int workgroupSizePerDim = 8;
            // This ceils invocationCountX / workgroupSizePerDim
            int workgroupCountX = (invocationCountX + workgroupSizePerDim - 1) / workgroupSizePerDim;
            int workgroupCountY = (invocationCountY + workgroupSizePerDim - 1) / workgroupSizePerDim;

            // dispatch workgroups
            pass.dispatchWorkGroups(workgroupCountX, workgroupCountY, 1);
            bindGroup.dispose();
        }
        pass.end();

        CommandBuffer commandBuffer = encoder.finish();
        encoder.dispose();

        // feed the command buffer to the queue
        queue.submit(commandBuffer);
        commandBuffer.dispose();

        queue.dispose();
    }

}
