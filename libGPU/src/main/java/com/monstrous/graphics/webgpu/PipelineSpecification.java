package com.monstrous.graphics.webgpu;

import com.monstrous.LibGPU;
import com.monstrous.graphics.ShaderProgram;
import com.monstrous.graphics.VertexAttributes;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.utils.Disposable;
import com.monstrous.wgpu.WGPUBlendFactor;
import com.monstrous.wgpu.WGPUBlendOperation;
import com.monstrous.wgpu.WGPUCullMode;
import com.monstrous.wgpu.WGPUTextureFormat;

import java.util.Objects;

public class PipelineSpecification implements Disposable {
    public String name;
    public VertexAttributes vertexAttributes;
    public Environment environment;
    public String shaderSourceFile;
    public ShaderProgram shader;
    public boolean ownsShader;
    public boolean hasDepth;

    public WGPUBlendFactor blendSrcColor;
    public WGPUBlendFactor blendDstColor;
    public WGPUBlendOperation blendOpColor;
    public WGPUBlendFactor blendSrcAlpha;
    public WGPUBlendFactor blendDstAlpha;
    public WGPUBlendOperation blendOpAlpha;
    public WGPUCullMode cullMode;

    public WGPUTextureFormat colorFormat;
    public WGPUTextureFormat depthFormat;


    public PipelineSpecification() {
        this.name = "pipeline";
        ownsShader = false;
        enableDepth();
        enableBlending();
        setCullMode(WGPUCullMode.None);
        colorFormat = LibGPU.surfaceFormat;
        depthFormat = WGPUTextureFormat.Depth24Plus;       // todo get from adapter?
    }

    public PipelineSpecification(VertexAttributes vertexAttributes, String shaderSourceFile) {
        this();
        this.vertexAttributes = vertexAttributes;
        this.shaderSourceFile = shaderSourceFile;
    }

    public PipelineSpecification(VertexAttributes vertexAttributes, ShaderProgram shader) {
        this();
        this.vertexAttributes = vertexAttributes;
        this.shader = shader;
    }

    public PipelineSpecification(PipelineSpecification spec) {
        this.name  = spec.name;
        this.vertexAttributes = spec.vertexAttributes;       // should be deep copy
        this.shaderSourceFile = spec.shaderSourceFile;
        this.shader = spec.shader;
        this.hasDepth = spec.hasDepth;
        this.blendSrcColor = spec.blendSrcColor;
        this.blendDstColor = spec.blendDstColor;
        this.blendOpColor = spec.blendOpColor;
        this.blendSrcAlpha = spec.blendSrcAlpha;
        this.blendDstAlpha = spec.blendDstAlpha;
        this.blendOpAlpha = spec.blendOpAlpha;

        this.colorFormat = spec.colorFormat;
        this.depthFormat = spec.depthFormat;
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
        return hasDepth == that.hasDepth && Objects.equals(vertexAttributes, that.vertexAttributes) && shaderSourceFile.contentEquals(that.shaderSourceFile) && blendSrcColor == that.blendSrcColor && blendDstColor == that.blendDstColor && blendOpColor == that.blendOpColor && blendSrcAlpha == that.blendSrcAlpha && blendDstAlpha == that.blendDstAlpha && blendOpAlpha == that.blendOpAlpha;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vertexAttributes.getUsageFlags(), shaderSourceFile, hasDepth, blendSrcColor, blendDstColor, blendOpColor, blendSrcAlpha, blendDstAlpha, blendOpAlpha);
    }

    @Override
    public void dispose() {
        if(ownsShader)
            shader.dispose();
    }
}
