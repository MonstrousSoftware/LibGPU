package com.monstrous.graphics.webgpu;

import com.monstrous.LibGPU;
import com.monstrous.graphics.ShaderProgram;
import com.monstrous.graphics.VertexAttributes;
import com.monstrous.wgpu.WGPUBlendFactor;
import com.monstrous.wgpu.WGPUBlendOperation;
import com.monstrous.wgpu.WGPUCullMode;
import com.monstrous.wgpu.WGPUTextureFormat;

import java.util.Objects;

public class PipelineSpecification {
    public String name;
    public VertexAttributes vertexAttributes;
    public ShaderProgram shader;
    public boolean hasDepth;

    public WGPUBlendFactor blendSrcColor;
    public WGPUBlendFactor blendDstColor;
    public WGPUBlendOperation blendOpColor;
    public WGPUBlendFactor blendSrcAlpha;
    public WGPUBlendFactor blendDstAlpha;
    public WGPUBlendOperation blendOpAlpha;
    public WGPUCullMode cullMode;

    public WGPUTextureFormat colorFormat;


    public PipelineSpecification() {
        this.name = "pipeline";
        enableDepth();
        enableBlending();
        setCullMode(WGPUCullMode.None);
        colorFormat = LibGPU.surfaceFormat;
    }

    public PipelineSpecification(VertexAttributes vertexAttributes, ShaderProgram shader) {
        this();
        this.vertexAttributes = vertexAttributes;
        this.shader = shader;
    }

    public PipelineSpecification(PipelineSpecification spec) {
        this.name  = spec.name;
        this.vertexAttributes = spec.vertexAttributes;       // should be deep copy
        this.shader = spec.shader;
        this.hasDepth = spec.hasDepth;
        this.blendSrcColor = spec.blendSrcColor;
        this.blendDstColor = spec.blendDstColor;
        this.blendOpColor = spec.blendOpColor;
        this.blendSrcAlpha = spec.blendSrcAlpha;
        this.blendDstAlpha = spec.blendDstAlpha;
        this.blendOpAlpha = spec.blendOpAlpha;

        this.colorFormat = spec.colorFormat;
    }

    public void enableDepth(){
        hasDepth = true;
    }

    public void disableDepth(){
        hasDepth = false;
    }

    public void setCullMode(WGPUCullMode cullMode){
        this.cullMode = cullMode;
    }

    public void enableBlending(){
        blendSrcColor = WGPUBlendFactor.SrcAlpha;
        blendDstColor = WGPUBlendFactor.OneMinusSrcAlpha;
        blendOpColor = WGPUBlendOperation.Add;
        blendSrcAlpha = WGPUBlendFactor.Zero;
        blendDstAlpha = WGPUBlendFactor.One;
        blendOpAlpha = WGPUBlendOperation.Add;
    }

    public void disableBlending(){
        blendSrcColor = WGPUBlendFactor.One;
        blendDstColor = WGPUBlendFactor.Zero;
        blendOpColor = WGPUBlendOperation.Add;
        blendSrcAlpha = WGPUBlendFactor.One;
        blendDstAlpha = WGPUBlendFactor.Zero;
        blendOpAlpha = WGPUBlendOperation.Add;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PipelineSpecification that = (PipelineSpecification) o;
        return hasDepth == that.hasDepth && Objects.equals(vertexAttributes, that.vertexAttributes) && Objects.equals(shader, that.shader) && blendSrcColor == that.blendSrcColor && blendDstColor == that.blendDstColor && blendOpColor == that.blendOpColor && blendSrcAlpha == that.blendSrcAlpha && blendDstAlpha == that.blendDstAlpha && blendOpAlpha == that.blendOpAlpha;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vertexAttributes, shader, hasDepth, blendSrcColor, blendDstColor, blendOpColor, blendSrcAlpha, blendDstAlpha, blendOpAlpha);
    }
}
