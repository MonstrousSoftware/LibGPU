package com.monstrous.graphics.webgpu;

import com.monstrous.wgpu.WGPUIndexFormat;
import com.monstrous.wgpu.WGPUTextureFormat;
import jnr.ffi.Pointer;

import static com.monstrous.LibGPU.webGPU;

public class RenderPass {

    private final Pointer renderPass;                   // handle used by WebGPU
    private final WGPUTextureFormat textureFormat;
    private final WGPUTextureFormat depthFormat;
    public int targetWidth, targetHeight;
    private int sampleCount;

    // don't call this directly, use RenderPassBuilder.create()
    RenderPass(Pointer renderPass, WGPUTextureFormat textureFormat, WGPUTextureFormat depthFormat, int sampleCount, int targetWidth, int targetHeight) {
        this.renderPass = renderPass;
        this.textureFormat = textureFormat;
        this.depthFormat = depthFormat;
        this.sampleCount = sampleCount;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
    }

    public void end() {
        webGPU.RenderPassEncoderEnd(renderPass);
        webGPU.RenderPassEncoderRelease(renderPass);
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

    public void setSampleCount(int n){
        sampleCount = n;
    }

    public int getSampleCount(){
        return sampleCount;
    }

    public void setPipeline(Pointer pipeline) {
        webGPU.RenderPassEncoderSetPipeline(renderPass, pipeline);
    }

    public void setBindGroup(int groupIndex, Pointer bindGroup) {
        setBindGroup(groupIndex, bindGroup, 0, null);
    }

    public void setBindGroup(int groupIndex, Pointer bindGroup, int dynamicOffsetCount, Pointer dynamicOffsets) {
        webGPU.RenderPassEncoderSetBindGroup(renderPass, groupIndex, bindGroup, dynamicOffsetCount, dynamicOffsets);
    }

    public void setVertexBuffer(int slot, Pointer vertexBuffer, long offset, long size) {
        webGPU.RenderPassEncoderSetVertexBuffer(renderPass,slot ,vertexBuffer, offset, size);
    }

    public void setIndexBuffer(Pointer indexBuffer, WGPUIndexFormat wgpuIndexFormat, int offset, long size) {
        webGPU.RenderPassEncoderSetIndexBuffer(renderPass, indexBuffer, wgpuIndexFormat, offset, size);
    }

    public void setViewport(float x, float y, float width, float height, float minDepth, float maxDepth){
        webGPU.RenderPassEncoderSetViewport(renderPass, x, y, width, height, minDepth, maxDepth);
    }

    public void setScissorRect(int x, int y, int width, int height){
        webGPU.RenderPassEncoderSetScissorRect(renderPass,  x,  y,  width,  height);
    }

    public void drawIndexed(int indexCount, int numInstances, int firstIndex, int baseVertex, int firstInstance) {
        webGPU.RenderPassEncoderDrawIndexed (renderPass, indexCount,  numInstances,  firstIndex,  baseVertex,  firstInstance);
    }

    public void draw(int numVertices, int numInstances, int firstVertex, int firstInstance){
        webGPU.RenderPassEncoderDraw(renderPass, numVertices, numInstances, firstVertex, firstInstance);
    }
}
