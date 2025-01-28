package com.monstrous.graphics;

import com.monstrous.LibGPU;
import com.monstrous.graphics.loaders.MaterialData;
import com.monstrous.graphics.webgpu.RenderPass;
import com.monstrous.utils.Disposable;
import com.monstrous.wgpu.*;
import com.monstrous.wgpuUtils.WgpuJava;
import jnr.ffi.Pointer;

import static com.monstrous.LibGPU.wgpu;


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
    private Pointer materialUniformBuffer;
    private Pointer materialBindGroup;
    private static Pointer uniformData;

    public Material(MaterialData materialData) {
        baseColor = new Color(materialData.diffuse);
        String fileName;
        if(materialData.diffuseMapFilePath == null)
            this.diffuseTexture = getDefaultWhiteTexture();
        else
            this.diffuseTexture = new Texture(materialData.diffuseMapFilePath, true);


        if( materialData.normalMapFilePath != null) {
            this.normalTexture = new Texture(materialData.normalMapFilePath, true);
            hasNormalMap = true;
        } else {
            this.normalTexture = getDefaultBlackTexture();  // will not be used anyway
            hasNormalMap = false;
        }

        if( materialData.emissiveMapFilePath != null)
            this.emissiveTexture = new Texture(materialData.emissiveMapFilePath, true);
        else
            this.emissiveTexture = getDefaultBlackTexture();    // no emissive colour

        roughnessFactor = materialData.roughnessFactor;
        if(roughnessFactor < 0) // not provided
            roughnessFactor = 1f; // default
        metallicFactor = materialData.metallicFactor;
        if(metallicFactor < 0)  // not provided
            metallicFactor = 1f; // default

        if( materialData.metallicRoughnessMapFilePath != null)
            this.metallicRoughnessTexture = new Texture(materialData.metallicRoughnessMapFilePath, true);
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


    private Texture getDefaultWhiteTexture(){
        if(whitePixel == null){
            whitePixel = new Texture(1,1);
            whitePixel.fill(Color.WHITE);
        }
        return whitePixel;
    }

    private Texture getDefaultBlackTexture(){
        if(blackPixel == null){
            blackPixel = new Texture(2,2);
            blackPixel.fill(Color.BLACK);
        }
        return blackPixel;
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
