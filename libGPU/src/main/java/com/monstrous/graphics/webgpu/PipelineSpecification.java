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

import com.monstrous.FileHandle;
import com.monstrous.LibGPU;
import com.monstrous.graphics.ShaderProgram;
import com.monstrous.graphics.VertexAttributes;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.utils.Disposable;
import com.monstrous.webgpu.WGPUBlendFactor;
import com.monstrous.webgpu.WGPUBlendOperation;
import com.monstrous.webgpu.WGPUCullMode;
import com.monstrous.webgpu.WGPUTextureFormat;

import java.util.Objects;

public class PipelineSpecification implements Disposable {
    public String name;
    public VertexAttributes vertexAttributes;
    public Environment environment;
    public FileHandle shaderSourceFile;
    public ShaderProgram shader;
    public boolean ownsShader;
    public boolean hasDepth;
    public boolean isSkyBox;
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


    public PipelineSpecification() {
        this.name = "pipeline";
        ownsShader = false;
        enableDepth();
        enableBlending();
        setCullMode(WGPUCullMode.None);
        colorFormat = LibGPU.surfaceFormat;
        depthFormat = WGPUTextureFormat.Depth24Plus;       // todo get from adapter?
        numSamples = 1;
        isSkyBox = false;
    }

    public PipelineSpecification(VertexAttributes vertexAttributes, FileHandle shaderSourceFile) {
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
        this.numSamples = spec.numSamples;
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
        return hasDepth == that.hasDepth && Objects.equals(vertexAttributes, that.vertexAttributes) && blendSrcColor == that.blendSrcColor && blendDstColor == that.blendDstColor
                && blendOpColor == that.blendOpColor && blendSrcAlpha == that.blendSrcAlpha && blendDstAlpha == that.blendDstAlpha && blendOpAlpha == that.blendOpAlpha &&
                numSamples == that.numSamples;
        // todo compare shader
    }

    @Override
    public int hashCode() {
        return Objects.hash(vertexAttributes.getUsageFlags(), shaderSourceFile, shader, hasDepth, blendSrcColor, blendDstColor, blendOpColor, blendSrcAlpha, blendDstAlpha, blendOpAlpha, numSamples);
    }

    @Override
    public void dispose() {
        if(ownsShader)
            shader.dispose();
    }
}
