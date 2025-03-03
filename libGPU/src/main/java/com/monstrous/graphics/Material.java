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

import com.monstrous.LibGPU;
import com.monstrous.graphics.loaders.MaterialData;
import com.monstrous.graphics.webgpu.RenderPass;
import com.monstrous.graphics.webgpu.UniformBuffer;
import com.monstrous.utils.Disposable;
import com.monstrous.webgpu.*;
import jnr.ffi.Pointer;

import static com.monstrous.LibGPU.webGPU;


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

    private static Pointer materialBindGroupLayout;
    private UniformBuffer materialUniformBuffer;
    private Pointer materialBindGroup;


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
        disposeUnlessStatic(diffuseTexture);
        disposeUnlessStatic(normalTexture);
        disposeUnlessStatic(emissiveTexture);
        disposeUnlessStatic(metallicRoughnessTexture);

        webGPU.wgpuBindGroupRelease(materialBindGroup);
        materialBindGroup = null;

        // cannot be released as it is shared by all materials
        //wgpu.BindGroupLayoutRelease(materialBindGroupLayout);

        materialUniformBuffer.dispose();
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

        materialBindGroup = createMaterialBindGroup(this, materialBindGroupLayout, materialUniformBuffer.getHandle());   // bind group for textures and uniforms

    }

    // bind material to the render pass
    public void bindGroup(RenderPass renderPass, int groupId ){
        renderPass.setBindGroup(groupId, materialBindGroup, 0, null);
    }



    public static Pointer getBindGroupLayout(){
        // make a bind group layout (shared by all materials)
        if(materialBindGroupLayout == null)
            materialBindGroupLayout = createMaterialBindGroupLayout();
        return materialBindGroupLayout;
    }

    private static Pointer createMaterialBindGroupLayout(){
        int location = 0;

        // Define binding layout
        WGPUBindGroupLayoutEntry uniformBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(uniformBindingLayout);
        uniformBindingLayout.setBinding(location++);
        uniformBindingLayout.setVisibility(WGPUShaderStage.Fragment);
        uniformBindingLayout.getBuffer().setType(WGPUBufferBindingType.Uniform);
        uniformBindingLayout.getBuffer().setMinBindingSize(MATERIAL_UB_SIZE);
        uniformBindingLayout.getBuffer().setHasDynamicOffset(0L);

        WGPUBindGroupLayoutEntry texBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(texBindingLayout);
        texBindingLayout.setBinding(location++);
        texBindingLayout.setVisibility(WGPUShaderStage.Fragment);
        texBindingLayout.getTexture().setSampleType(WGPUTextureSampleType.Float);
        texBindingLayout.getTexture().setViewDimension(WGPUTextureViewDimension._2D);

        WGPUBindGroupLayoutEntry samplerBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(samplerBindingLayout);
        samplerBindingLayout.setBinding(location++);
        samplerBindingLayout.setVisibility(WGPUShaderStage.Fragment);
        samplerBindingLayout.getSampler().setType(WGPUSamplerBindingType.Filtering);

        // emissive texture binding is included even if it is not used
        WGPUBindGroupLayoutEntry emissiveTexBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(emissiveTexBindingLayout);
        emissiveTexBindingLayout.setBinding(location++);
        emissiveTexBindingLayout.setVisibility(WGPUShaderStage.Fragment);
        emissiveTexBindingLayout.getTexture().setSampleType(WGPUTextureSampleType.Float);
        emissiveTexBindingLayout.getTexture().setViewDimension(WGPUTextureViewDimension._2D);

        // normal texture binding is included even if it is not used
        WGPUBindGroupLayoutEntry normalTexBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(normalTexBindingLayout);
        normalTexBindingLayout.setBinding(location++);
        normalTexBindingLayout.setVisibility(WGPUShaderStage.Fragment);
        normalTexBindingLayout.getTexture().setSampleType(WGPUTextureSampleType.Float);
        normalTexBindingLayout.getTexture().setViewDimension(WGPUTextureViewDimension._2D);

        // metallic roughness texture binding is included even if it is not used
        WGPUBindGroupLayoutEntry mrTexBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(mrTexBindingLayout);
        mrTexBindingLayout.setBinding(location++);
        mrTexBindingLayout.setVisibility(WGPUShaderStage.Fragment);
        mrTexBindingLayout.getTexture().setSampleType(WGPUTextureSampleType.Float);
        mrTexBindingLayout.getTexture().setViewDimension(WGPUTextureViewDimension._2D);

        // Create a bind group layout
        WGPUBindGroupLayoutDescriptor bindGroupLayoutDesc = WGPUBindGroupLayoutDescriptor.createDirect();
        bindGroupLayoutDesc.setNextInChain();
        bindGroupLayoutDesc.setLabel("ModelBatch Bind Group Layout (Material)");
        bindGroupLayoutDesc.setEntryCount(location);

        bindGroupLayoutDesc.setEntries(uniformBindingLayout, texBindingLayout, samplerBindingLayout, emissiveTexBindingLayout, normalTexBindingLayout, mrTexBindingLayout );
        return webGPU.wgpuDeviceCreateBindGroupLayout(LibGPU.device, bindGroupLayoutDesc);
    }

    // per material bind group
    private Pointer createMaterialBindGroup(Material material, Pointer bindGroupLayout, Pointer materialUniformBuffer) {
        // Create a binding
        WGPUBindGroupEntry uniformBinding = WGPUBindGroupEntry.createDirect();
        uniformBinding.setNextInChain();
        uniformBinding.setBinding(0);  // binding index
        uniformBinding.setBuffer(materialUniformBuffer);
        uniformBinding.setOffset(0);
        uniformBinding.setSize(MATERIAL_UB_SIZE);

        Texture diffuse = material.diffuseTexture;


        // A bind group contains one or multiple bindings
        WGPUBindGroupDescriptor bindGroupDesc = WGPUBindGroupDescriptor.createDirect();
        bindGroupDesc.setNextInChain();
        bindGroupDesc.setLayout(bindGroupLayout);

        // There must be as many bindings as declared in the layout!
        bindGroupDesc.setEntryCount(6);
        bindGroupDesc.setEntries(
                uniformBinding,
                diffuse.getBinding(1),
                diffuse.getSamplerBinding(2),
                material.emissiveTexture.getBinding(3),
                material.normalTexture.getBinding(4),
                material.metallicRoughnessTexture.getBinding(5));

        return webGPU.wgpuDeviceCreateBindGroup(LibGPU.device, bindGroupDesc);
    }

    private void writeMaterialUniforms( UniformBuffer uniformBuffer){

        uniformBuffer.beginFill();
        uniformBuffer.append(metallicFactor);
        uniformBuffer.append(roughnessFactor);
        uniformBuffer.pad(2*4); // padding (important)
        uniformBuffer.append(baseColor);

        uniformBuffer.endFill(); // write to GPU
    }

    // default binding layout values
    private static void setDefault(WGPUBindGroupLayoutEntry bindingLayout) {

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
