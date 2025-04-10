package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.webgpu.*;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.webgpu.*;
import jnr.ffi.Pointer;


public class TestComputeMipMap extends ApplicationAdapter {

    private SpriteBatch batch;
    private Texture sourceTexture;
    private ShaderProgram computeShader;
    private Texture texture;
    private Texture destTexture;
    private TextureView inputTextureView;
    private TextureView outputTextureView;
    private Pointer pipeline;
    private WebGPU_JNI webGPU;


    @Override
    public void create() {
        webGPU = LibGPU.webGPU;
        batch = new SpriteBatch();

        sourceTexture = new Texture("textures/jackRussel256.png", true);

        int textureUsage = WGPUTextureUsage.TextureBinding | WGPUTextureUsage.StorageBinding|WGPUTextureUsage.CopyDst | WGPUTextureUsage.CopySrc;
        destTexture = new Texture(128, 128, 1, textureUsage, WGPUTextureFormat.RGBA8Unorm, 1);

        // create a texture with 2 mip levels
        // level 0 to be filled from source file
        //textureUsage = WGPUTextureUsage.TextureBinding | WGPUTextureUsage.StorageBinding|WGPUTextureUsage.CopyDst | WGPUTextureUsage.CopySrc;
        int mipLevels = 8;
        texture = new Texture(256, 256, mipLevels, textureUsage, WGPUTextureFormat.RGBA8Unorm, 1);


        // load image file into first mip level
        FileHandle file = Files.internal("textures/jackRussel256.png");
        byte[] byteArray = file.readAllBytes();
        Pointer data = JavaWebGPU.createByteArrayPointer(byteArray);
        Pointer image = JavaWebGPU.getUtils().gdx2d_load(data, byteArray.length);        // use native function to parse image file
        PixmapInfo info = PixmapInfo.createAt(image);
        Pointer pixelPtr = info.pixels.get();
        texture.load(pixelPtr, 0);

        int ok = ImageSave.saveToPNG("saved.png", pixelPtr, 256, 256, 4);
        System.out.println("save png: "+ok);

        // note: the textures need to be different otherwise it doesn't work. Error: includes writable usage and another usage in the same synchronization scope
        inputTextureView =  new TextureView(texture, WGPUTextureAspect.All, WGPUTextureViewDimension._2D, texture.getFormat(), 0, 1, 0, 1 );
        outputTextureView = new TextureView(destTexture, WGPUTextureAspect.All, WGPUTextureViewDimension._2D, texture.getFormat(), 1, 1, 0, 1 );

        onCompute();
    }

    @Override
    public void render(){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){
            LibGPU.app.exit();
            return;
        }

        // SpriteBatch testing
        batch.begin(Color.TEAL);
        batch.draw(sourceTexture, 0,0, 512, 512);        // normal texture
        batch.draw(destTexture, 512,0, 512, 512);        // normal texture
        batch.end();
    }

    @Override
    public void dispose(){
        // cleanup
        texture.dispose();
        batch.dispose();
    }

    @Override
    public void resize(int width, int height) {

        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }

    private void onCompute() {

        // make a pipeline

        BindGroupLayout bindGroupLayout = makeBindGroupLayout();
        BindGroup bindGroup = makeBindGroup(bindGroupLayout);

        // compute shader
        computeShader = new ShaderProgram(Files.internal("shaders/compute-mipmap.wgsl"));

        PipelineLayout pipelineLayout = new PipelineLayout("compute pipeline layout", bindGroupLayout);
        pipeline = makeComputePipeline(computeShader, pipelineLayout);

        compute(bindGroup);

        // cleanup
        webGPU.wgpuComputePipelineRelease(pipeline);
        pipelineLayout.dispose();
        computeShader.dispose();
        bindGroup.dispose();
        bindGroupLayout.dispose();

        ImageSave.saveToPNG("mip-1.png", destTexture,0);
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

    private BindGroup makeBindGroup(BindGroupLayout bindGroupLayout){
        // create bind group
        BindGroup bindGroup = new BindGroup(bindGroupLayout);
        bindGroup.addTexture(0, inputTextureView);
        bindGroup.addTexture(1, outputTextureView);
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

        return LibGPU.webGPU.wgpuDeviceCreateComputePipeline(LibGPU.device, pipelineDescriptor);
    }

    private void compute(BindGroup bindGroup) {

        // create a command encoder
        WGPUCommandEncoderDescriptor encoderDesc = WGPUCommandEncoderDescriptor.createDirect();
        encoderDesc.setNextInChain();
        Pointer encoder = webGPU.wgpuDeviceCreateCommandEncoder(LibGPU.device, encoderDesc);

        // Create a compute pass
        WGPUComputePassDescriptor passDesc = WGPUComputePassDescriptor.createDirect();
        passDesc.setNextInChain();
        passDesc.setTimestampWrites();
        Pointer computePass = webGPU.wgpuCommandEncoderBeginComputePass(encoder, passDesc);

        // set pipeline
        webGPU.wgpuComputePassEncoderSetPipeline(computePass, pipeline);
        // set bind group
        webGPU.wgpuComputePassEncoderSetBindGroup(computePass, 0, bindGroup.getHandle(), 0, JavaWebGPU.createNullPointer());

        // use one thread per texel of the output, i.e. half the size of the input texture
        int invocationCountX = texture.getWidth() / 2;
        int invocationCountY = texture.getHeight() / 2;

        int workgroupSizePerDim = 8;
        // This ceils invocationCountX / workgroupSizePerDim
        int workgroupCountX = (invocationCountX + workgroupSizePerDim - 1) / workgroupSizePerDim;
        int workgroupCountY = (invocationCountY + workgroupSizePerDim - 1) / workgroupSizePerDim;

        // dispatch workgroups
        webGPU.wgpuComputePassEncoderDispatchWorkgroups(computePass, workgroupCountX,workgroupCountY, 1);

        webGPU.wgpuComputePassEncoderEnd(computePass);

        // finish the encoder to give use command buffer
        WGPUCommandBufferDescriptor bufferDescriptor = WGPUCommandBufferDescriptor.createDirect();
        bufferDescriptor.setNextInChain();
        Pointer commandBuffer = webGPU.wgpuCommandEncoderFinish(encoder, bufferDescriptor);
        webGPU.wgpuCommandEncoderRelease(encoder);

        // feed the command buffer to the queue
        long[] buffers = new long[1];
        buffers[0] = commandBuffer.address();
        Pointer bufferPtr = JavaWebGPU.createLongArrayPointer(buffers);
        webGPU.wgpuQueueSubmit(LibGPU.queue, 1, bufferPtr);


        webGPU.wgpuDeviceTick(LibGPU.device);   // Dawn

        webGPU.wgpuCommandBufferRelease(commandBuffer);
        webGPU.wgpuCommandEncoderRelease(encoder);
        //webGPU.wgpuComputePassEncoderRelease(computePass); // causes crash
    }




}
