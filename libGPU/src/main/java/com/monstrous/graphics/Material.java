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

package com.monstrous.graphics;

import com.monstrous.graphics.loaders.MaterialData;
import com.monstrous.graphics.webgpu.*;
import com.monstrous.utils.Disposable;
import com.monstrous.webgpu.*;


public class Material implements Disposable {
    private static final int MATERIAL_UB_SIZE = 8 * Float.BYTES;

    public Color baseColor;
    public Texture diffuseTexture;
    public Texture metallicRoughnessTexture;
    public Texture normalTexture;
    public Texture emissiveTexture;
    public boolean hasNormalMap;
    public float metallicFactor = 0.0f;
    public float roughnessFactor = 0.5f;

    private static Texture whitePixel;  // fallback texture
    private static Texture blackPixel;  // fallback texture

    private static BindGroupLayout materialBindGroupLayout;
    private UniformBuffer materialUniformBuffer;
    private BindGroup materialBindGroup;


    public Material(MaterialData materialData) {
        baseColor = new Color(materialData.diffuse);
        if(materialData.diffuseMapData == null)
            this.diffuseTexture = getDefaultWhiteTexture();
        else
            this.diffuseTexture = new Texture(materialData.diffuseMapData, true);

        if( materialData.normalMapData != null) {
            this.normalTexture = new Texture(materialData.normalMapData, true);
            hasNormalMap = true;
        } else {
            this.normalTexture = getDefaultBlackTexture();  // will not be used anyway
            hasNormalMap = false;
        }

        if( materialData.emissiveMapData != null)
            this.emissiveTexture = new Texture(materialData.emissiveMapData, true);
        else
            this.emissiveTexture = getDefaultBlackTexture();    // no emissive colour

        roughnessFactor = materialData.roughnessFactor;
        if(roughnessFactor < 0) // not provided
            roughnessFactor = 1f; // default
        metallicFactor = materialData.metallicFactor;
        if(metallicFactor < 0)  // not provided
            metallicFactor = 1f; // default

        if( materialData.metallicRoughnessMapData != null)
            this.metallicRoughnessTexture = new Texture(materialData.metallicRoughnessMapData, true);
        else
            this.metallicRoughnessTexture = getDefaultWhiteTexture();

        createBindGroup();
    }

    // todo merge constructors
    public Material(Texture texture) {
        this.baseColor = new Color(Color.WHITE);
        this.diffuseTexture = texture;
        this.emissiveTexture = getDefaultBlackTexture();
        this.normalTexture = getDefaultBlackTexture();
        this.metallicRoughnessTexture = getDefaultBlackTexture();
        createBindGroup();
    }

    public Material(Color baseColor) {
        this.baseColor = new Color(baseColor);
        this.diffuseTexture = getDefaultWhiteTexture();
        this.emissiveTexture = getDefaultBlackTexture();
        this.normalTexture = getDefaultBlackTexture();
        this.metallicRoughnessTexture = getDefaultBlackTexture();
        createBindGroup();
    }

    // for sorting materials, put emphasis on having or not a normal map, because this implies a pipeline switch, not just a material switch
    // todo: should have same value for materials with same colours and same texture file names
    public int sortCode(){
        return (normalTexture != null ? 10000 : 0) + (hashCode() % 10000);
    }



    @Override
    public void dispose() {
        materialBindGroup.dispose();
        materialUniformBuffer.dispose();

        disposeUnlessStatic(diffuseTexture);
        disposeUnlessStatic(normalTexture);
        disposeUnlessStatic(emissiveTexture);
        disposeUnlessStatic(metallicRoughnessTexture);

        // cannot be released as it is shared by all materials
        //wgpu.BindGroupLayoutRelease(materialBindGroupLayout);
    }

    // avoid disposing shared static placeholder textures
    private void disposeUnlessStatic(Texture texture){
        if(texture != null && texture != whitePixel && texture != blackPixel)
            texture.dispose();
    }


    private Texture getDefaultWhiteTexture(){
        if(whitePixel == null){
            whitePixel = new Texture(1,1);
            whitePixel.fill(Color.WHITE);
        }
        return whitePixel;
    }

    private Texture getDefaultBlackTexture(){
        if(blackPixel == null){
            blackPixel = new Texture(1,1);
            blackPixel.fill(Color.BLACK);
        }
        return blackPixel;
    }

    private void createBindGroup(){
        // make a bind group layout (shared by all materials)
        materialBindGroupLayout = getBindGroupLayout();

        // create a uniform buffer
        materialUniformBuffer = new UniformBuffer( MATERIAL_UB_SIZE, WGPUBufferUsage.CopyDst | WGPUBufferUsage.Uniform);
        //materialUniformBuffer = createUniformBuffer( MATERIAL_UB_SIZE );

        // fill the uniform buffer
        writeMaterialUniforms(materialUniformBuffer);

        // create a bind group
        materialBindGroup = createMaterialBindGroup(this, materialBindGroupLayout, materialUniformBuffer.getBuffer());   // bind group for textures and uniforms
    }

    // bind material to the render pass
    public void bindGroup(RenderPass renderPass, int groupId ){
        renderPass.setBindGroup(groupId, materialBindGroup.getHandle(), 0, null);
    }



    public static BindGroupLayout getBindGroupLayout(){
        // make a bind group layout (shared by all materials)
        if(materialBindGroupLayout == null)
            materialBindGroupLayout = createMaterialBindGroupLayout();
        return materialBindGroupLayout;
    }

    private static BindGroupLayout createMaterialBindGroupLayout(){
        int location = 0;
        BindGroupLayout layout = new BindGroupLayout("ModelBatch Bind Group Layout (Material)");
        layout.begin();
        layout.addBuffer(location++, WGPUShaderStage.Fragment, WGPUBufferBindingType.Uniform, MATERIAL_UB_SIZE, false);
        layout.addTexture(location++, WGPUShaderStage.Fragment,WGPUTextureSampleType.Float, WGPUTextureViewDimension._2D, false);
        layout.addSampler(location++, WGPUShaderStage.Fragment,WGPUSamplerBindingType.Filtering);
        layout.addTexture(location++, WGPUShaderStage.Fragment,WGPUTextureSampleType.Float, WGPUTextureViewDimension._2D, false);// emissive texture
        layout.addTexture(location++, WGPUShaderStage.Fragment,WGPUTextureSampleType.Float, WGPUTextureViewDimension._2D, false);// normal texture
        layout.addTexture(location, WGPUShaderStage.Fragment,WGPUTextureSampleType.Float, WGPUTextureViewDimension._2D, false);// metallic roughness texture

        layout.end();
        return layout;
    }

    // per material bind group
    private BindGroup createMaterialBindGroup(Material material, BindGroupLayout bindGroupLayout, Buffer materialUniformBuffer) {
        BindGroup bg = new BindGroup(bindGroupLayout);
        bg.begin();
        bg.addBuffer(0, materialUniformBuffer);
        bg.addTexture(1, material.diffuseTexture.getTextureView());
        bg.addSampler(2, material.diffuseTexture.getSampler());
        bg.addTexture(3, material.emissiveTexture.getTextureView());
        bg.addTexture(4, material.normalTexture.getTextureView());
        bg.addTexture(5, material.metallicRoughnessTexture.getTextureView());
        bg.end();
        return bg;
    }

    private void writeMaterialUniforms( UniformBuffer uniformBuffer){

        uniformBuffer.beginFill();
        uniformBuffer.append(metallicFactor);
        uniformBuffer.append(roughnessFactor);
        uniformBuffer.pad(2*4); // padding (important)
        uniformBuffer.append(baseColor);

        uniformBuffer.endFill(); // write to GPU
    }

}
