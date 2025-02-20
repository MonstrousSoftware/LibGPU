package com.monstrous.graphics.g3d;

import com.monstrous.Files;
import com.monstrous.LibGPU;
import com.monstrous.graphics.Camera;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.webgpu.Pipeline;
import com.monstrous.graphics.webgpu.PipelineSpecification;
import com.monstrous.graphics.webgpu.RenderPass;
import com.monstrous.graphics.webgpu.UniformBuffer;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;
import com.monstrous.utils.Disposable;
import com.monstrous.wgpu.*;
import com.monstrous.wgpuUtils.WgpuJava;
import jnr.ffi.Pointer;

// SkyBox
//
// Following the approach from https://webgpufundamentals.org/webgpu/lessons/webgpu-skybox.html
//
// This uses a dedicated shader that renders one screen filling triangle using a cube map.
// The camera projection view matrix is inverted and use to look up screen pixels in the cube map.
// The sky box should be rendered after all opaque renderables.

public class SkyBox implements Disposable {

    private final int FRAME_UB_SIZE = 16*Float.BYTES;   // to hold one 4x4 matrix

    private final WebGPU webGPU;
    private final Pointer device;

    private final Texture cubeMap;
    private final UniformBuffer uniformBuffer;
    private final Pointer bindGroupLayout;
    private final Pointer bindGroup;

    private final Pipeline pipeline;
    private final PipelineSpecification pipelineSpec;
    private final Matrix4 invertedProjectionView;


    public SkyBox( Texture cubeMap ){
        this.cubeMap = cubeMap;
        webGPU = LibGPU.webGPU;
        device = LibGPU.device;

        uniformBuffer = new UniformBuffer(FRAME_UB_SIZE, WGPUBufferUsage.CopyDst | WGPUBufferUsage.Uniform);

        bindGroupLayout = createBindGroupLayout();

        Pointer pipelineLayout = makePipelineLayout(bindGroupLayout);

        pipelineSpec = new PipelineSpecification();
        pipelineSpec.name = "skybox pipeline";
        pipelineSpec.vertexAttributes = null;
        pipelineSpec.environment = null;
        pipelineSpec.shader = null;
        pipelineSpec.shaderSourceFile =  Files.classpath("shaders/skybox.wgsl");
        pipelineSpec.enableDepth();
        pipelineSpec.setCullMode(WGPUCullMode.Back);
        pipelineSpec.colorFormat = LibGPU.surfaceFormat;
        pipelineSpec.depthFormat = WGPUTextureFormat.Depth24Plus;
        pipelineSpec.numSamples = 1;
        pipelineSpec.isSkyBox = true;

        pipeline = new Pipeline(pipelineLayout, pipelineSpec);

        bindGroup = makeBindGroup(bindGroupLayout, uniformBuffer.getHandle());  // move to constructor along with BG release?

        invertedProjectionView = new Matrix4();
    }

    public void render(Camera camera, RenderPass pass){
        writeUniforms(uniformBuffer, camera);

        pass.setPipeline(pipeline.getPipeline());
        pass.setBindGroup(0, bindGroup);
        pass.draw(3);
    }


    @Override
    public void dispose() {
        pipeline.dispose();
        webGPU.BindGroupLayoutRelease(bindGroupLayout);
        uniformBuffer.dispose();
        pipelineSpec.dispose();
        webGPU.BindGroupRelease(bindGroup);
    }


    // Bind Group Layout:
    //  0 uniforms,
    //  1 cube map texture,
    //  2 sampler

    private Pointer createBindGroupLayout(){

        // Define binding layout
        int location = 0;

        WGPUBindGroupLayoutEntry uniformBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(uniformBindingLayout);
        uniformBindingLayout.setBinding(location++);
        uniformBindingLayout.setVisibility( WGPUShaderStage.Fragment);
        uniformBindingLayout.getBuffer().setType(WGPUBufferBindingType.Uniform);
        uniformBindingLayout.getBuffer().setMinBindingSize(FRAME_UB_SIZE);
        uniformBindingLayout.getBuffer().setHasDynamicOffset(0L);

        // cube map texture
        WGPUBindGroupLayoutEntry cubeMapBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(cubeMapBindingLayout);
        cubeMapBindingLayout.setBinding(location++);
        cubeMapBindingLayout.setVisibility(WGPUShaderStage.Fragment);
        cubeMapBindingLayout.getTexture().setSampleType(WGPUTextureSampleType.Float);
        cubeMapBindingLayout.getTexture().setViewDimension(WGPUTextureViewDimension.Cube);

        // cube map sampler
        WGPUBindGroupLayoutEntry cubeMapSamplerBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(cubeMapSamplerBindingLayout);
        cubeMapSamplerBindingLayout.setBinding(location++);
        cubeMapSamplerBindingLayout.setVisibility(WGPUShaderStage.Fragment);
        cubeMapSamplerBindingLayout.getSampler().setType(WGPUSamplerBindingType.Filtering);

        // Create a bind group layout
        WGPUBindGroupLayoutDescriptor bindGroupLayoutDesc = WGPUBindGroupLayoutDescriptor.createDirect();
        bindGroupLayoutDesc.setNextInChain();
        bindGroupLayoutDesc.setLabel("SkyBox Bind Group Layout");
        bindGroupLayoutDesc.setEntryCount(3);
        bindGroupLayoutDesc.setEntries(uniformBindingLayout, cubeMapBindingLayout, cubeMapSamplerBindingLayout);
        return webGPU.DeviceCreateBindGroupLayout(device, bindGroupLayoutDesc);
    }

    //  bind group
    private Pointer makeBindGroup(Pointer bindGroupLayout, Pointer uniformBuffer) {
        // Create a binding
        WGPUBindGroupEntry uniformBinding = WGPUBindGroupEntry.createDirect();
        uniformBinding.setNextInChain();
        uniformBinding.setBinding(0);  // binding index
        uniformBinding.setBuffer(uniformBuffer);
        uniformBinding.setOffset(0);
        uniformBinding.setSize(FRAME_UB_SIZE);

        // A bind group contains one or multiple bindings
        WGPUBindGroupDescriptor bindGroupDesc = WGPUBindGroupDescriptor.createDirect();
        bindGroupDesc.setNextInChain();
        bindGroupDesc.setLayout(bindGroupLayout);

        bindGroupDesc.setEntryCount(3);
        bindGroupDesc.setEntries(uniformBinding, cubeMap.getBinding(1), cubeMap.getSamplerBinding(2));

        return webGPU.DeviceCreateBindGroup(device, bindGroupDesc);
    }



    private Pointer makePipelineLayout(Pointer bindGroupLayout) {

        long[] layouts = new long[1];
        layouts[0] = bindGroupLayout.address();

        Pointer layoutPtr = WgpuJava.createLongArrayPointer(layouts);

        // Create the pipeline layout to define the bind groups needed
        WGPUPipelineLayoutDescriptor layoutDesc = WGPUPipelineLayoutDescriptor.createDirect();
        layoutDesc.setNextInChain();
        layoutDesc.setLabel("SkyBox Pipeline Layout");
        layoutDesc.setBindGroupLayoutCount(1);
        layoutDesc.setBindGroupLayouts(layoutPtr);
        return LibGPU.webGPU.DeviceCreatePipelineLayout(LibGPU.device, layoutDesc);
    }


    private void writeUniforms( UniformBuffer uniformBuffer, Camera camera ){
        invertedProjectionView.set(camera.combined);
        invertedProjectionView.setTranslation(Vector3.Zero);
        invertedProjectionView.inv();

        uniformBuffer.beginFill();
        uniformBuffer.append(invertedProjectionView);
        uniformBuffer.endFill();   // write to GPU buffer
    }



    private void setDefault(WGPUBindGroupLayoutEntry bindingLayout) {

        bindingLayout.getBuffer().setNextInChain();
        bindingLayout.getBuffer().setType(WGPUBufferBindingType.Undefined);
        bindingLayout.getBuffer().setHasDynamicOffset(0L);

        bindingLayout.getSampler().setNextInChain();
        bindingLayout.getSampler().setType(WGPUSamplerBindingType.Undefined);

        bindingLayout.getStorageTexture().setNextInChain();
        bindingLayout.getStorageTexture().setAccess(WGPUStorageTextureAccess.Undefined);
        bindingLayout.getStorageTexture().setFormat(WGPUTextureFormat.Undefined);
        bindingLayout.getStorageTexture().setViewDimension(WGPUTextureViewDimension.Undefined);

        bindingLayout.getTexture().setNextInChain();
        bindingLayout.getTexture().setMultisampled(0L);
        bindingLayout.getTexture().setSampleType(WGPUTextureSampleType.Undefined);
        bindingLayout.getTexture().setViewDimension(WGPUTextureViewDimension.Undefined);
    }

}
