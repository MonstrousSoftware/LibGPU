package com.monstrous.graphics.webgpu;

import com.monstrous.wgpu.WGPUIndexFormat;
import com.monstrous.wgpu.WGPUTextureFormat;
import jnr.ffi.Pointer;

import static com.monstrous.LibGPU.wgpu;

public class RenderPass {

    private final Pointer renderPass;                   // handle used by WebGPU
    private final WGPUTextureFormat textureFormat;
    private final WGPUTextureFormat depthFormat;

    // don't call this directly, use RenderPassBuilder.create()
    RenderPass(Pointer renderPass, WGPUTextureFormat textureFormat, WGPUTextureFormat depthFormat) {
        this.renderPass = renderPass;
        this.textureFormat = textureFormat;
        this.depthFormat = depthFormat;
    }

    public void end() {
        wgpu.RenderPassEncoderEnd(renderPass);
        wgpu.RenderPassEncoderRelease(renderPass);
    }

    public Pointer getPointer() {
        return renderPass;
    }

    public WGPUTextureFormat getColorFormat(){
        return textureFormat;
    }

    public WGPUTextureFormat getDepthFormat(){
        return depthFormat;
    }

    public void setPipeline(Pointer pipeline) {
        wgpu.RenderPassEncoderSetPipeline(renderPass, pipeline);
    }

    public void setBindGroup(int groupIndex, Pointer bindGroup) {
        setBindGroup(groupIndex, bindGroup, 0, null);
    }

    public void setBindGroup(int groupIndex, Pointer bindGroup, int dynamicOffsetCount, Pointer dynamicOffsets) {
        wgpu.RenderPassEncoderSetBindGroup(renderPass, groupIndex, bindGroup, dynamicOffsetCount, dynamicOffsets);
    }

    public void setVertexBuffer(int slot, Pointer vertexBuffer, long offset, long size) {
        wgpu.RenderPassEncoderSetVertexBuffer(renderPass,slot ,vertexBuffer, offset, size);
    }

    public void setIndexBuffer(Pointer indexBuffer, WGPUIndexFormat wgpuIndexFormat, int offset, long size) {
        wgpu.RenderPassEncoderSetIndexBuffer(renderPass, indexBuffer, wgpuIndexFormat, offset, size);
    }

    public void setViewport(float x, float y, float width, float height, float minDepth, float maxDepth){
        wgpu.RenderPassEncoderSetViewport(renderPass, x, y, width, height, minDepth, maxDepth);
    }

    public void setScissorRect(int x, int y, int width, int height){
        wgpu.RenderPassEncoderSetScissorRect(renderPass,  x,  y,  width,  height);
    }

    public void drawIndexed(int indexCount, int numInstances, int firstIndex, int baseVertex, int firstInstance) {
        wgpu.RenderPassEncoderDrawIndexed (renderPass, indexCount,  numInstances,  firstIndex,  baseVertex,  firstInstance);
    }

    public void draw(int numVertices, int numInstances, int firstVertex, int firstInstance){
        wgpu.RenderPassEncoderDraw(renderPass, numVertices, numInstances, firstVertex, firstInstance);
    }
}
