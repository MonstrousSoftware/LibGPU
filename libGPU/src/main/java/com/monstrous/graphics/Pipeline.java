package com.monstrous.graphics;

import com.monstrous.LibGPU;
import com.monstrous.utils.Disposable;
import com.monstrous.wgpu.*;
import jnr.ffi.Pointer;

public class Pipeline implements Disposable {

    private VertexAttributes vertexAttributes;
    private ShaderProgram shader;
    private boolean hasDepth;
    private Pointer pipelineLayout;
    private Pointer pipeline;


    public Pipeline(VertexAttributes vertexAttributes, Pointer pipelineLayout, ShaderProgram shader, boolean depth) {
        this.vertexAttributes = vertexAttributes;
        this.pipelineLayout = pipelineLayout;
        this.shader = shader;
        this.hasDepth = depth;

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

        if(depth) {
            depthStencilState.setDepthCompare(WGPUCompareFunction.Less);
            depthStencilState.setDepthWriteEnabled(1L);
        } else {
            // disable depth testing
            depthStencilState.setDepthCompare(WGPUCompareFunction.Always);
            depthStencilState.setDepthWriteEnabled(0L);
        }

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


        pipelineDesc.setLayout(pipelineLayout);
        pipeline = LibGPU.wgpu.DeviceCreateRenderPipeline(LibGPU.device, pipelineDesc);
    }

    public boolean canRender(VertexAttributes vertexAttributes, boolean depth){    // perhaps we need more params
        // crude check, to be refined
        return (vertexAttributes.attributes.size() == this.vertexAttributes.attributes.size() &&
                vertexAttributes.hasNormalMap == this.vertexAttributes.hasNormalMap &&
                hasDepth == depth);
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
