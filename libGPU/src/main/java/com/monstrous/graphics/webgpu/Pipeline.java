package com.monstrous.graphics.webgpu;

import com.monstrous.LibGPU;
import com.monstrous.ShaderPrefix;
import com.monstrous.graphics.ShaderProgram;
import com.monstrous.utils.Disposable;
import com.monstrous.wgpu.*;
import jnr.ffi.Pointer;

public class Pipeline implements Disposable {

    private Pointer pipelineLayout;
    private Pointer pipeline;
    public PipelineSpecification specification;

    public Pipeline(Pointer pipelineLayout, PipelineSpecification spec) {
        this.specification = new PipelineSpecification(spec);

        this.pipelineLayout = pipelineLayout;

        // if the specification does not already have a shader, create one from the source file, customized to the vertex attributes.
        if(spec.shader == null){
            String prefix = ShaderPrefix.buildPrefix(spec.vertexAttributes, spec.environment);
            System.out.println("Shader Source ["+spec.shaderSourceFile+"] Prefix: ["+prefix+"]");
            spec.shader = new ShaderProgram(spec.shaderSourceFile, prefix);
            spec.ownsShader = true;
        }

        //spec.shader = shader;
        Pointer shaderModule = spec.shader.getShaderModule(); //spec.shader.getShaderModule();
        WGPUVertexBufferLayout vertexBufferLayout = spec.vertexAttributes.getVertexBufferLayout();

        WGPURenderPipelineDescriptor pipelineDesc = WGPURenderPipelineDescriptor.createDirect();
        pipelineDesc.setNextInChain();
        pipelineDesc.setLabel( spec.name );

        pipelineDesc.getVertex().setBufferCount(1);
        pipelineDesc.getVertex().setBuffers(vertexBufferLayout);

        pipelineDesc.getVertex().setModule(shaderModule);
        pipelineDesc.getVertex().setEntryPoint("vs_main");
        pipelineDesc.getVertex().setConstantCount(0);
        pipelineDesc.getVertex().setConstants();

        pipelineDesc.getPrimitive().setTopology(WGPUPrimitiveTopology.TriangleList);
        pipelineDesc.getPrimitive().setStripIndexFormat(WGPUIndexFormat.Undefined);
        pipelineDesc.getPrimitive().setFrontFace(WGPUFrontFace.CCW);
        pipelineDesc.getPrimitive().setCullMode(spec.cullMode);

        WGPUFragmentState fragmentState = WGPUFragmentState.createDirect();
        fragmentState.setNextInChain();
        fragmentState.setModule(shaderModule);
        fragmentState.setEntryPoint("fs_main");
        fragmentState.setConstantCount(0);
        fragmentState.setConstants();

        // blend
        WGPUBlendState blendState = WGPUBlendState.createDirect();
        blendState.getColor().setSrcFactor(spec.blendSrcColor);
        blendState.getColor().setDstFactor(spec.blendDstColor);
        blendState.getColor().setOperation(spec.blendOpColor);
        blendState.getAlpha().setSrcFactor(spec.blendSrcAlpha);
        blendState.getAlpha().setDstFactor(spec.blendDstAlpha);
        blendState.getAlpha().setOperation(spec.blendOpAlpha);

        WGPUColorTargetState colorTarget = WGPUColorTargetState.createDirect();

        colorTarget.setFormat(spec.colorFormat);
        colorTarget.setBlend(blendState);
        colorTarget.setWriteMask(WGPUColorWriteMask.All);

        fragmentState.setTargetCount(1);
        fragmentState.setTargets(colorTarget);

        pipelineDesc.setFragment(fragmentState);

        WGPUDepthStencilState depthStencilState = WGPUDepthStencilState.createDirect();
        setDefault(depthStencilState);

        if(spec.hasDepth) {
            depthStencilState.setDepthCompare(WGPUCompareFunction.Less);
            depthStencilState.setDepthWriteEnabled(1L);
        } else {
            // disable depth testing
            depthStencilState.setDepthCompare(WGPUCompareFunction.Always);
            depthStencilState.setDepthWriteEnabled(0L);
        }

        //
        depthStencilState.setFormat(spec.depthFormat);
        // deactivate stencil
        depthStencilState.setStencilReadMask(0L);
        depthStencilState.setStencilWriteMask(0L);

        pipelineDesc.setDepthStencil(depthStencilState);

        pipelineDesc.getMultisample().setCount(spec.numSamples);
        pipelineDesc.getMultisample().setMask( -1L );
        pipelineDesc.getMultisample().setAlphaToCoverageEnabled(0);

        pipelineDesc.setLayout(pipelineLayout);
        pipeline = LibGPU.wgpu.DeviceCreateRenderPipeline(LibGPU.device, pipelineDesc);
        if(pipeline == null)
            throw new RuntimeException("Pipeline createion failed");
    }

    public boolean canRender(PipelineSpecification spec){    // perhaps we need more params
        // crude check, to be refined
        int h = spec.hashCode();
        int h2 = this.specification.hashCode();
        return h == h2;
        //return spec.hashCode() == this.specification.hashCode();
        // could be too strict, e.g. name changes or different instances of same shader

//        return (spec.vertexAttributes.attributes.size() == this.specification.vertexAttributes.attributes.size() &&
//               spec.vertexAttributes.hasNormalMap == this.specification.vertexAttributes.hasNormalMap &&
//                spec.hasDepth == this.specification.hasDepth);
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
