package com.monstrous.graphics;

import com.monstrous.LibGPU;
import com.monstrous.graphics.loaders.MaterialData;
import com.monstrous.graphics.webgpu.RenderPass;
import com.monstrous.utils.Disposable;
import com.monstrous.wgpu.*;
import com.monstrous.wgpuUtils.WgpuJava;
import jnr.ffi.Pointer;

import static com.monstrous.LibGPU.wgpu;

// todo make 1 pixel default textures and reuse...

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


    private static Pointer materialBindGroupLayout;
    private Pointer materialUniformBuffer;
    private Pointer materialBindGroup;
    private static Pointer uniformData;

    public Material(MaterialData materialData) {
        baseColor = new Color(materialData.diffuse);
        String fileName;
        if(materialData.diffuseMapFilePath == null)
            fileName = "textures\\white.png";
        else
            fileName = materialData.diffuseMapFilePath;
        this.diffuseTexture = new Texture(fileName, true);
        // todo use caching of textures in case we reuse the same texture in different materials

        fileName = "textures\\black.png";               // placeholder
        if( materialData.normalMapFilePath != null)
            fileName = materialData.normalMapFilePath;
        this.normalTexture = new Texture(fileName, true);

        fileName = "textures\\black.png";
        if( materialData.emissiveMapFilePath != null)
            fileName = materialData.emissiveMapFilePath;
        this.emissiveTexture = new Texture(fileName, true);

        roughnessFactor = materialData.roughnessFactor;
        if(roughnessFactor < 0)
            roughnessFactor = 1f; // default
        metallicFactor = materialData.metallicFactor;
        if(metallicFactor < 0)
            metallicFactor = 1f; // default

        fileName = "textures\\white.png";
        if( materialData.metallicRoughnessMapFilePath != null)
            fileName = materialData.metallicRoughnessMapFilePath;
        this.metallicRoughnessTexture = new Texture(fileName, true);
        createBindGroup();
    }

    // todo merge constructors
    public Material(Texture texture) {
        baseColor = new Color(Color.WHITE);
        this.diffuseTexture = texture;
        this.emissiveTexture = new Texture("textures\\black.png", false);
        createBindGroup();
    }

    public Material(Color baseColor) {
        this.baseColor = new Color(baseColor);
        this.diffuseTexture = new Texture("textures\\white.png", false);
        this.emissiveTexture = new Texture("textures\\black.png", false);
        this.normalTexture = new Texture("textures\\black.png", false);
        this.metallicRoughnessTexture = new Texture("textures\\black.png", true);
        createBindGroup();
    }

    // for sorting materials, put emphasis on having or not a normal map, because this implies a pipeline switch, not just a material switch
    // todo: should have same value for materials with same colours and same texture file names
    public int sortCode(){
        return (normalTexture != null ? 10000 : 0) + (hashCode() % 10000);
    }



    @Override
    public void dispose() {
        diffuseTexture.dispose();
        if(normalTexture != null)
            normalTexture.dispose();
        if(materialBindGroup != null) {
            wgpu.BindGroupRelease(materialBindGroup);
            materialBindGroup = null;
        }

        // cannot be released as it is shared by all materials
        //wgpu.BindGroupLayoutRelease(materialBindGroupLayout);

        wgpu.BufferRelease(materialUniformBuffer);
    }

    private void createBindGroup(){
        // make a bind group layout (shared by all materials)
        materialBindGroupLayout = getBindGroupLayout();

        // create a uniform buffer
        materialUniformBuffer = createUniformBuffer( MATERIAL_UB_SIZE );

        // fill the uniform buffer
        writeMaterialUniforms(materialUniformBuffer);

        // create a bind group
        materialBindGroup = createMaterialBindGroup(this, materialBindGroupLayout, materialUniformBuffer);   // bind group for textures and uniforms
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
        return wgpu.DeviceCreateBindGroupLayout(LibGPU.device, bindGroupLayoutDesc);
    }


    private Pointer createUniformBuffer(int bufferSize ) {
        // Create uniform buffer
        WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
        bufferDesc.setLabel("Uniform object buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.Uniform );
        bufferDesc.setSize(bufferSize);
        bufferDesc.setMappedAtCreation(0L);
        return wgpu.DeviceCreateBuffer(LibGPU.device, bufferDesc);
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
        bindGroupDesc.setEntries(uniformBinding, diffuse.getBinding(1), diffuse.getSamplerBinding(2), material.emissiveTexture.getBinding(3), material.normalTexture.getBinding(4),
                    material.metallicRoughnessTexture.getBinding(5));

        return wgpu.DeviceCreateBindGroup(LibGPU.device, bindGroupDesc);
    }

    private int setUniformFloat(Pointer data, int offset, float value ){
        data.putFloat(offset, value);
        return Float.BYTES;
    }

    private int setUniformColor(Pointer data, int offset, Color color ){
        data.putFloat(offset+0*Float.BYTES, color.r);
        data.putFloat(offset+1*Float.BYTES, color.g);
        data.putFloat(offset+2*Float.BYTES, color.b);
        data.putFloat(offset+3*Float.BYTES, color.a);
        return 4*Float.BYTES;
    }

    private void writeMaterialUniforms( Pointer uniformBuffer){

        // working native buffer to fill uniform buffer
        // static so it can be reused
        if(uniformData == null) {
            float[] uniforms = new float[MATERIAL_UB_SIZE / Float.BYTES];
            uniformData = WgpuJava.createFloatArrayPointer(uniforms);       // native memory buffer for one instance to aid write buffer
        }

        int offset = 0;
        offset += setUniformFloat(uniformData, offset, metallicFactor);
        offset += setUniformFloat(uniformData, offset, roughnessFactor);
        offset += 2*4; // padding (important)
        offset += setUniformColor(uniformData, offset, baseColor);

        wgpu.QueueWriteBuffer(LibGPU.queue, uniformBuffer, 0, uniformData, offset);
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
