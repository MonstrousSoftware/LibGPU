package com.monstrous.graphics.webgpu;

import com.monstrous.LibGPU;
import com.monstrous.utils.Disposable;
import com.monstrous.webgpu.*;
import jnr.ffi.Pointer;

import java.util.ArrayList;

/**
 * Encapsulated bind group layout.  Use begin(), addXXX(), end() to define a layout.
 */
public class BindGroupLayout implements Disposable {
    private Pointer handle = null;
    private final String label;
    private final ArrayList<WGPUBindGroupLayoutEntry> entries;


    public BindGroupLayout() {
        this("bind group layout");
    }

    public BindGroupLayout( String label ) {
        this.label = label;
        entries = new ArrayList<>();
    }

    public void begin(){
        entries.clear();
        handle = null;
    }

    /**
     * Add binding layout for a buffer.
     *
     * @param bindingId integer as in the shader, 0, 1, 2, ...
     * @param visibility e.g. WGPUShaderStage.Fragment (or combination using OR operator)
     * @param bufferBindingType e.g. WGPUBufferBindingType.ReadOnlyStorage
     */
    public void addBuffer(int bindingId, long visibility, WGPUBufferBindingType bufferBindingType, long minBindingSize, boolean hasDynamicOffset ){
        WGPUBindGroupLayoutEntry bindingLayout = addBinding(bindingId, visibility);
        bindingLayout.getBuffer().setType(bufferBindingType);
        bindingLayout.getBuffer().setMinBindingSize(minBindingSize);
        bindingLayout.getBuffer().setHasDynamicOffset(hasDynamicOffset? 1L : 0L);

        entries.add(bindingLayout);
    }

    public void addTexture(int bindingId, long visibility, WGPUTextureSampleType sampleType, WGPUTextureViewDimension viewDimension, boolean multiSampled ){
        WGPUBindGroupLayoutEntry bindingLayout = addBinding(bindingId, visibility);
        bindingLayout.getTexture().setMultisampled(multiSampled? 1L : 0L);
        bindingLayout.getTexture().setSampleType(sampleType);
        bindingLayout.getTexture().setViewDimension(viewDimension);
        entries.add(bindingLayout);
    }
    public void addStorageTexture(int bindingId, long visibility, WGPUStorageTextureAccess access, WGPUTextureFormat format, WGPUTextureViewDimension viewDimension ){
        WGPUBindGroupLayoutEntry bindingLayout = addBinding(bindingId, visibility);
        bindingLayout.getStorageTexture().setAccess(access);
        bindingLayout.getStorageTexture().setFormat(format);
        bindingLayout.getStorageTexture().setViewDimension(viewDimension);
        entries.add(bindingLayout);
    }

    public void addSampler(int bindingId, long visibility, WGPUSamplerBindingType samplerType ){
        WGPUBindGroupLayoutEntry bindingLayout = addBinding(bindingId, visibility);
        bindingLayout.getSampler().setType(samplerType);
        entries.add(bindingLayout);
    }


    /** addBinding
     *  common part of binding layouts
     */
    private WGPUBindGroupLayoutEntry addBinding(int bindingId, long visibility ){
        WGPUBindGroupLayoutEntry bindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefaultLayout(bindingLayout);
        bindingLayout.setBinding(bindingId);
        bindingLayout.setVisibility(visibility);
        return bindingLayout;
    }

    public void end(){
        // Create a bind group layout
        WGPUBindGroupLayoutDescriptor bindGroupLayoutDesc = WGPUBindGroupLayoutDescriptor.createDirect();
        bindGroupLayoutDesc.setNextInChain();
        bindGroupLayoutDesc.setLabel(label);
        bindGroupLayoutDesc.setEntryCount(entries.size());
        WGPUBindGroupLayoutEntry[] entryArray = new WGPUBindGroupLayoutEntry[entries.size()];
        for(int i = 0; i < entries.size(); i++)
            entryArray[i] = entries.get(i);
        bindGroupLayoutDesc.setEntries( entryArray );

        handle = LibGPU.webGPU.wgpuDeviceCreateBindGroupLayout(LibGPU.device, bindGroupLayoutDesc);
    }

    public Pointer getHandle(){
        if(handle == null)
            throw new RuntimeException("BindGroupLayout not defined, did you forget to call end()?");
        return handle;
    }


    private void setDefaultLayout(WGPUBindGroupLayoutEntry bindingLayout) {

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

    @Override
    public void dispose() {
        LibGPU.webGPU.wgpuBindGroupLayoutRelease(handle);
    }
}



