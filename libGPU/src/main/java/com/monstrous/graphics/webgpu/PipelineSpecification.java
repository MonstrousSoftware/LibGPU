/*******************************************************************************
 * Copyright 2025 Monstrous Software.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.monstrous.graphics.webgpu;

import com.monstrous.LibGPU;
import com.monstrous.graphics.ShaderProgram;
import com.monstrous.graphics.VertexAttributes;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.webgpu.*;

import java.util.Objects;

public class PipelineSpecification  {
    public String name;
    public VertexAttributes vertexAttributes;
    public WGPUIndexFormat indexFormat;
    public WGPUPrimitiveTopology topology;
    public Environment environment;
    public String shaderFilePath;
    public ShaderProgram shader;
    public boolean hasDepth;
    public boolean isSkyBox;
    public boolean isDepthPass;
    public boolean afterDepthPrepass;
    public int numSamples;

    public WGPUBlendFactor blendSrcColor;
    public WGPUBlendFactor blendDstColor;
    public WGPUBlendOperation blendOpColor;
    public WGPUBlendFactor blendSrcAlpha;
    public WGPUBlendFactor blendDstAlpha;
    public WGPUBlendOperation blendOpAlpha;
    public WGPUCullMode cullMode;

    public WGPUTextureFormat colorFormat;
    public WGPUTextureFormat depthFormat;
    private int hash;


    public PipelineSpecification() {
        this.name = "pipeline";
        enableDepth();
        disableBlending();
        setCullMode(WGPUCullMode.None);
        indexFormat = WGPUIndexFormat.Uint16;
        topology =  WGPUPrimitiveTopology.TriangleList;
        isDepthPass = false;
        colorFormat = LibGPU.surfaceFormat;
        depthFormat = WGPUTextureFormat.Depth24Plus;       // todo get from adapter?
        numSamples = 1;
        isSkyBox = false;
        afterDepthPrepass = false;
        recalcHash();
    }

    public PipelineSpecification(VertexAttributes vertexAttributes, String shaderFilePath) {
        this();
        this.vertexAttributes = vertexAttributes;
        this.shaderFilePath = shaderFilePath;
        recalcHash();
    }

    public PipelineSpecification(VertexAttributes vertexAttributes, ShaderProgram shader) {
        this();
        this.vertexAttributes = vertexAttributes;
        this.shader = shader;
        recalcHash();
    }

    public PipelineSpecification(PipelineSpecification spec) {
        this.name  = spec.name;
        this.vertexAttributes = spec.vertexAttributes;       // should be deep copy
        this.environment = spec.environment;
        this.shaderFilePath = spec.shaderFilePath;
        this.shader = spec.shader;
        this.hasDepth = spec.hasDepth;
        this.isDepthPass= spec.isDepthPass;
        this.blendSrcColor = spec.blendSrcColor;
        this.blendDstColor = spec.blendDstColor;
        this.blendOpColor = spec.blendOpColor;
        this.blendSrcAlpha = spec.blendSrcAlpha;
        this.blendDstAlpha = spec.blendDstAlpha;
        this.blendOpAlpha = spec.blendOpAlpha;
        this.cullMode = spec.cullMode;
        this.topology = spec.topology;
        this.indexFormat = spec.indexFormat;
        this.isSkyBox = spec.isSkyBox;
        this.afterDepthPrepass = spec.afterDepthPrepass;

        this.colorFormat = spec.colorFormat;
        this.depthFormat = spec.depthFormat;
        this.numSamples = spec.numSamples;
        recalcHash();
    }

    public void enableDepth(){
        hasDepth = true;
        recalcHash();
    }

    public void disableDepth(){
        hasDepth = false;
        recalcHash();
    }

    public void setCullMode(WGPUCullMode cullMode){
        this.cullMode = cullMode;
        recalcHash();
    }

    public void enableBlending(){
        blendSrcColor = WGPUBlendFactor.SrcAlpha;
        blendDstColor = WGPUBlendFactor.OneMinusSrcAlpha;
        blendOpColor = WGPUBlendOperation.Add;
        blendSrcAlpha = WGPUBlendFactor.Zero;
        blendDstAlpha = WGPUBlendFactor.One;
        blendOpAlpha = WGPUBlendOperation.Add;
        recalcHash();
    }

    public void disableBlending(){
        blendSrcColor = WGPUBlendFactor.One;
        blendDstColor = WGPUBlendFactor.Zero;
        blendOpColor = WGPUBlendOperation.Add;
        blendSrcAlpha = WGPUBlendFactor.One;
        blendDstAlpha = WGPUBlendFactor.Zero;
        blendOpAlpha = WGPUBlendOperation.Add;
        recalcHash();
    }

    // used?
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PipelineSpecification that = (PipelineSpecification) o;
        return hasDepth == that.hasDepth && Objects.equals(vertexAttributes, that.vertexAttributes) && blendSrcColor == that.blendSrcColor && blendDstColor == that.blendDstColor
                && blendOpColor == that.blendOpColor && blendSrcAlpha == that.blendSrcAlpha && blendDstAlpha == that.blendDstAlpha && blendOpAlpha == that.blendOpAlpha &&
                numSamples == that.numSamples;
        // todo compare shader
    }

    @Override
    public int hashCode() {
        return hash;
    }

    /** to be called whenever relevant content changes (to avoid doing this in hashCode which is called a lot) */
    public void recalcHash() {
        hash = Objects.hash(vertexAttributes != null ? vertexAttributes.getUsageFlags() : 0,
                shaderFilePath,
                shader != null ? shader.getName(): "",
                isDepthPass, afterDepthPrepass,
                topology, indexFormat,
                hasDepth, blendSrcColor, blendDstColor, blendOpColor, blendSrcAlpha, blendDstAlpha, blendOpAlpha, numSamples, cullMode, isSkyBox, depthFormat, numSamples);
    }

}
