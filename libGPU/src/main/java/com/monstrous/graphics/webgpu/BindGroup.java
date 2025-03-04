package com.monstrous.graphics.webgpu;

import com.monstrous.LibGPU;
import com.monstrous.utils.Disposable;
import com.monstrous.webgpu.*;
import jnr.ffi.Pointer;

import java.util.ArrayList;

/**
 * Encapsulated bind group.  Use begin(), addXXX(), end() to define a layout.
 */
public class BindGroup implements Disposable {

    private Pointer handle = null;

    private final BindGroupLayout layout;
    private final ArrayList<WGPUBindGroupEntry> entries;

    public BindGroup(BindGroupLayout layout) {
        this.layout = layout;
        entries = new ArrayList<>();
    }

    public void begin() {
        entries.clear();
        handle = null;
    }

    /**
     * Add binding for a buffer.
     *
     * @param bindingId         integer as in the shader, 0, 1, 2, ...
     */
    public void addBuffer(int bindingId, Buffer buffer, int offset, long size) {
        WGPUBindGroupEntry entry = WGPUBindGroupEntry.createDirect();
        entry.setBinding(bindingId);
        entry.setBuffer(buffer.getHandle());
        entry.setOffset(offset);
        entry.setSize(size);
        entries.add(entry);
    }

    // shorthand to add whole buffer with no offset
    public void addBuffer(int bindingId, Buffer buffer) {
        addBuffer(bindingId, buffer, 0, buffer.getSize());
    }

    public void addTexture(int bindingId, Pointer textureView) {
        WGPUBindGroupEntry entry = WGPUBindGroupEntry.createDirect();
        entry.setBinding(bindingId);
        entry.setTextureView(textureView);
        entries.add(entry);
    }

    public void addSampler(int bindingId, Pointer sampler) {
        WGPUBindGroupEntry entry = WGPUBindGroupEntry.createDirect();
        entry.setBinding(bindingId);
        entry.setSampler(sampler);
        entries.add(entry);
    }


    // fallback option
    public void addBindGroupEntry(int bindingId, WGPUBindGroupEntry entry) {
        entry.setBinding(bindingId);
        entries.add(entry);
    }

    // todo other types

    public void end() {
        // Create a bind group
        WGPUBindGroupDescriptor bindGroupDescriptor = WGPUBindGroupDescriptor.createDirect();
        bindGroupDescriptor.setNextInChain()
                .setLayout(layout.getHandle())
                .setEntryCount(entries.size());

        WGPUBindGroupEntry[] entryArray = new WGPUBindGroupEntry[entries.size()];
        for (int i = 0; i < entries.size(); i++)
            entryArray[i] = entries.get(i);
        bindGroupDescriptor.setEntries(entryArray);

        handle = LibGPU.webGPU.wgpuDeviceCreateBindGroup(LibGPU.device, bindGroupDescriptor);
    }

    public Pointer getHandle() {
        if (handle == null)
            throw new RuntimeException("BindGroup not defined, did you forget to call end()?");
        return handle;
    }

    @Override
    public void dispose() {
        LibGPU.webGPU.wgpuBindGroupRelease(handle);
    }

}



