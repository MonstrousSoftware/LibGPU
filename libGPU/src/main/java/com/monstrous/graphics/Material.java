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
    public boolean ownsDiffuseTexture = true;
    public Texture metallicRoughnessTexture;
    public Texture normalTexture;
    public Texture emissiveTexture;
    public boolean hasNormalMap;
    public float metallicFactor = 0.0f;
    public float roughnessFactor = 0.5f;

    private static Texture whitePixel;  // fallback texture
    private static Texture blackPixel;  // fallback texture

    private static BindGroupLayout materialBindGroupLayout;
    private static BindGroupLayout materialHDRBindGroupLayout;
    private UniformBuffer materialUniformBuffer;
    private BindGroup materialBindGroup;


    public Material(MaterialData materialData) {
        boolean useMipMapping = true;  // for performance testing
        System.out.println("Loading material: "+materialData.name);

        baseColor = new Color(materialData.diffuse);
        if(materialData.diffuseMapData == null)
            this.diffuseTexture = getDefaultWhiteTexture();
        else
            this.diffuseTexture = new Texture(materialData.diffuseMapData, materialData.name+".diffuse", useMipMapping);

        if( materialData.normalMapData != null) {
            this.normalTexture = new Texture(materialData.normalMapData, materialData.name+".normal", useMipMapping);
            hasNormalMap = true;
        } else {
            this.normalTexture = getDefaultBlackTexture();  // will not be used anyway
            hasNormalMap = false;
        }

        if( materialData.emissiveMapData != null)
            this.emissiveTexture = new Texture(materialData.emissiveMapData, materialData.name+".emissive", useMipMapping);
        else
            this.emissiveTexture = getDefaultBlackTexture();    // no emissive colour

        roughnessFactor = materialData.roughnessFactor;
        if(roughnessFactor < 0) // not provided
            roughnessFactor = 1f; // default
        metallicFactor = materialData.metallicFactor;
        if(metallicFactor < 0)  // not provided
            metallicFactor = 1f; // default

        if( materialData.metallicRoughnessMapData != null)
            this.metallicRoughnessTexture = new Texture(materialData.metallicRoughnessMapData, materialData.name+".MR", useMipMapping);
        else
            this.metallicRoughnessTexture = getDefaultWhiteTexture();

        createBindGroupLayout();
    }

    // todo merge constructors
    public Material(Texture texture) {
        this.baseColor = new Color(Color.WHITE);
        this.diffuseTexture = texture;
        this.ownsDiffuseTexture = false;    // caller has to dispose of texture, because it may be reused
        this.normalTexture = getDefaultBlackTexture();
        hasNormalMap = false;
        this.emissiveTexture = getDefaultBlackTexture();
        this.metallicRoughnessTexture = getDefaultWhiteTexture();
        createBindGroupLayout();
    }

    public Material(Color baseColor) {
        this.baseColor = new Color(baseColor);
        this.diffuseTexture = getDefaultWhiteTexture();
        this.normalTexture = getDefaultBlackTexture();
        hasNormalMap = false;
        roughnessFactor = 1f;
        metallicFactor = 1f; // default
        this.emissiveTexture = getDefaultBlackTexture();
        this.metallicRoughnessTexture = getDefaultWhiteTexture();
        createBindGroupLayout();
    }

    // for sorting materials, put emphasis on having or not a normal map, because this implies a pipeline switch, not just a material switch
    // todo: should have same value for materials with same colours and same texture file names
    public int sortCode(){
        return (normalTexture != null ? 10000 : 0) + (hashCode() % 10000);
    }



    @Override
    public void dispose() {
        if(materialBindGroup != null) {
            materialBindGroup.dispose();
            materialBindGroup = null;           // ensure we don't dispose multiple time if the same material is reused.
        }
        if(materialUniformBuffer != null) {
            materialUniformBuffer.dispose();
            materialUniformBuffer = null;
        }

        if(ownsDiffuseTexture)
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

    private void createBindGroupLayout(){
        materialBindGroupLayout = getBindGroupLayout();

        // create a uniform buffer
        materialUniformBuffer = new UniformBuffer( MATERIAL_UB_SIZE, WGPUBufferUsage.CopyDst | WGPUBufferUsage.Uniform);
    }

    public static BindGroupLayout getBindGroupLayout(){
        // make a bind group layout (static member shared by all materials)
        // to do : this is never released
        if(materialBindGroupLayout == null)
            materialBindGroupLayout = createMaterialBindGroupLayout();
        if(materialHDRBindGroupLayout == null)
            materialHDRBindGroupLayout = createHDRMaterialBindGroupLayout();
        return materialBindGroupLayout;
    }

    // bind material to the render pass
    public void bindGroup(RenderPass renderPass, int groupId ){
        if(diffuseTexture != null && diffuseTexture.getFormat() == WGPUTextureFormat.RGBA32Float){
            // fill the uniform buffer
            writeMaterialUniforms(materialUniformBuffer);
            // create a bind group
            materialBindGroup = createHDRMaterialBindGroup(this, materialHDRBindGroupLayout, materialUniformBuffer);   // bind group for textures and uniforms
        }
        else if(materialBindGroup == null){  // lazy init, in case some material properties are set after the Material constructor
            // fill the uniform buffer
            writeMaterialUniforms(materialUniformBuffer);

            // create a bind group
            materialBindGroup = createMaterialBindGroup(this, materialBindGroupLayout, materialUniformBuffer);   // bind group for textures and uniforms
        }
        renderPass.setBindGroup(groupId, materialBindGroup.getHandle(), 0, null);
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

    private static BindGroupLayout createHDRMaterialBindGroupLayout(){
        int location = 0;
        BindGroupLayout layout = new BindGroupLayout("ModelBatch Bind Group Layout (Material - HDR albedo)");
        layout.begin();
        layout.addBuffer(location++, WGPUShaderStage.Fragment, WGPUBufferBindingType.Uniform, MATERIAL_UB_SIZE, false);
        //layout.addTexture(location++, WGPUShaderStage.Fragment,WGPUTextureSampleType.Float, WGPUTextureViewDimension._2D, false);
        layout.addTexture(location++, WGPUShaderStage.Fragment,WGPUTextureSampleType.UnfilterableFloat, WGPUTextureViewDimension._2D, false);
        //layout.addSampler(location++, WGPUShaderStage.Fragment,WGPUSamplerBindingType.Filtering);
        layout.addSampler(location++, WGPUShaderStage.Fragment,WGPUSamplerBindingType.NonFiltering);
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

    // per material bind group
    private BindGroup createHDRMaterialBindGroup(Material material, BindGroupLayout bindGroupLayout, Buffer materialUniformBuffer) {
        BindGroup bg = new BindGroup(bindGroupLayout);
        bg.begin();
        bg.addBuffer(0, materialUniformBuffer);
        bg.addTexture(1, material.diffuseTexture.getTextureView());
        bg.addSampler(2, material.diffuseTexture.getHDRSampler());
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
