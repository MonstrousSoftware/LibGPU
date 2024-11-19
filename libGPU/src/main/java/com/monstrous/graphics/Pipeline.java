package com.monstrous.graphics;

import com.monstrous.LibGPU;
import com.monstrous.utils.Disposable;
import com.monstrous.wgpu.*;
import com.monstrous.wgpuUtils.WgpuJava;
import jnr.ffi.Pointer;

public class Pipeline implements Disposable {

    private VertexAttributes vertexAttributes;
    private Pointer bindGroupLayout;
    private ShaderProgram shader;
    private Pointer pipelineLayout;
    private Pointer pipeline;

    // assuming 1 bind group
    public Pipeline(VertexAttributes vertexAttributes, Pointer bindGroupLayout, ShaderProgram shader) {
        this.vertexAttributes = vertexAttributes;
        this.bindGroupLayout = bindGroupLayout;
        this.shader = shader;

        Pointer shaderModule = shader.getShaderModule();
        WGPUVertexBufferLayout vertexBufferLayout = vertexAttributes.getVertexBufferLayout();

        WGPURenderPipelineDescriptor pipelineDesc = WGPURenderPipelineDescriptor.createDirect();
        pipelineDesc.setNextInChain();
        pipelineDesc.setLabel("pipeline");

        pipelineDesc.getVertex().setBufferCount(1);
        pipelineDesc.getVertex().setBuffers(vertexBufferLayout);

        pipelineDesc.getVertex().setModule(shaderModule);
        pipelineDesc.getVertex().setEntryPoint("vs_main");
        pipelineDesc.getVertex().setConstantCount(0);
        pipelineDesc.getVertex().setConstants();

        pipelineDesc.getPrimitive().setTopology(WGPUPrimitiveTopology.TriangleList);
        pipelineDesc.getPrimitive().setStripIndexFormat(WGPUIndexFormat.Undefined);
        pipelineDesc.getPrimitive().setFrontFace(WGPUFrontFace.CCW);
        pipelineDesc.getPrimitive().setCullMode(WGPUCullMode.None);

        WGPUFragmentState fragmentState = WGPUFragmentState.createDirect();
        fragmentState.setNextInChain();
        fragmentState.setModule(shaderModule);
        fragmentState.setEntryPoint("fs_main");
        fragmentState.setConstantCount(0);
        fragmentState.setConstants();

        // blend
        WGPUBlendState blendState = WGPUBlendState.createDirect();
        blendState.getColor().setSrcFactor(WGPUBlendFactor.SrcAlpha);
        blendState.getColor().setDstFactor(WGPUBlendFactor.OneMinusSrcAlpha);
        blendState.getColor().setOperation(WGPUBlendOperation.Add);
        blendState.getAlpha().setSrcFactor(WGPUBlendFactor.Zero);
        blendState.getAlpha().setDstFactor(WGPUBlendFactor.One);
        blendState.getAlpha().setOperation(WGPUBlendOperation.Add);

        WGPUColorTargetState colorTarget = WGPUColorTargetState.createDirect();
        colorTarget.setFormat(LibGPU.surfaceFormat);
        colorTarget.setBlend(blendState);
        colorTarget.setWriteMask(WGPUColorWriteMask.All);

        fragmentState.setTargetCount(1);
        fragmentState.setTargets(colorTarget);

        pipelineDesc.setFragment(fragmentState);

        WGPUDepthStencilState depthStencilState = WGPUDepthStencilState.createDirect();
        setDefault(depthStencilState);
        depthStencilState.setDepthCompare(WGPUCompareFunction.Less);
        depthStencilState.setDepthWriteEnabled(1L);

        //
        WGPUTextureFormat depthTextureFormat = WGPUTextureFormat.Depth24Plus;       // todo
        depthStencilState.setFormat(depthTextureFormat);
        // deactivate stencil
        depthStencilState.setStencilReadMask(0L);
        depthStencilState.setStencilWriteMask(0L);

        pipelineDesc.setDepthStencil(depthStencilState);


        pipelineDesc.getMultisample().setCount(1);
        pipelineDesc.getMultisample().setMask( 0xFFFFFFFF);
        pipelineDesc.getMultisample().setAlphaToCoverageEnabled(0);



        long[] layouts = new long[1];
        layouts[0] = bindGroupLayout.address();
        Pointer layoutPtr = WgpuJava.createLongArrayPointer(layouts);

        // Create the pipeline layout to define the bind groups needed : 1 bind group
        WGPUPipelineLayoutDescriptor layoutDesc = WGPUPipelineLayoutDescriptor.createDirect();
        layoutDesc.setNextInChain();
        layoutDesc.setLabel("Pipeline Layout");
        layoutDesc.setBindGroupLayoutCount(1);
        layoutDesc.setBindGroupLayouts(layoutPtr);
        pipelineLayout = LibGPU.wgpu.DeviceCreatePipelineLayout(LibGPU.device, layoutDesc);

        pipelineDesc.setLayout(pipelineLayout);
        pipeline = LibGPU.wgpu.DeviceCreateRenderPipeline(LibGPU.device, pipelineDesc);
    }

    public boolean canRender(VertexAttributes vertexAttributes){
        // crude check, to be refined
        return (vertexAttributes.attributes.size() == this.vertexAttributes.attributes.size() &&
                vertexAttributes.hasNormalMap == this.vertexAttributes.hasNormalMap);
    }

    public Pointer getPipeline(){
        return pipeline;
    }

    @Override
    public void dispose() {
        LibGPU.wgpu.PipelineLayoutRelease(pipelineLayout);
        LibGPU.wgpu.RenderPipelineRelease(pipeline);
    }


    private void setDefault(WGPUStencilFaceState stencilFaceState) {
        stencilFaceState.setCompare( WGPUCompareFunction.Always);
        stencilFaceState.setFailOp( WGPUStencilOperation.Keep);
        stencilFaceState.setDepthFailOp( WGPUStencilOperation.Keep);
        stencilFaceState.setPassOp( WGPUStencilOperation.Keep);
    }


    private void setDefault(WGPUDepthStencilState  depthStencilState ) {
        depthStencilState.setFormat(WGPUTextureFormat.Undefined);
        depthStencilState.setDepthWriteEnabled(0L);
        depthStencilState.setDepthCompare(WGPUCompareFunction.Always);
        depthStencilState.setStencilReadMask(0xFFFFFFFF);
        depthStencilState.setStencilWriteMask(0xFFFFFFFF);
        depthStencilState.setDepthBias(0);
        depthStencilState.setDepthBiasSlopeScale(0);
        depthStencilState.setDepthBiasClamp(0);
        setDefault(depthStencilState.getStencilFront());
        setDefault(depthStencilState.getStencilBack());
    }


}
