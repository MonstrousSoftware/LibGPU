package com.monstrous.graphics.webgpu;

import com.monstrous.LibGPU;
import com.monstrous.graphics.Color;
import com.monstrous.utils.viewports.Viewport;
import com.monstrous.wgpu.*;
import com.monstrous.wgpuUtils.WgpuJava;
import jnr.ffi.Pointer;

import static com.monstrous.LibGPU.wgpu;

public class RenderPass {

    private static Pointer encoder;
    private static final Color clearColor = new Color(Color.BLACK);
    private static Viewport viewport = null;

    private final Pointer renderPass;


    private RenderPass(Pointer renderPass) {
        this.renderPass = renderPass;
    }

    public static void setEncoder(Pointer commandEncoder){
        encoder = commandEncoder;
    }

    public static RenderPass create() {
        if(encoder == null)
            throw new RuntimeException("Encoder must be set before calling RenderPass.create()");

        WGPURenderPassColorAttachment renderPassColorAttachment = WGPURenderPassColorAttachment.createDirect();
        renderPassColorAttachment.setNextInChain();
        renderPassColorAttachment.setView(LibGPU.app.targetView);
        renderPassColorAttachment.setResolveTarget(WgpuJava.createNullPointer());
        renderPassColorAttachment.setLoadOp(WGPULoadOp.Clear);
        renderPassColorAttachment.setStoreOp(WGPUStoreOp.Store);

        renderPassColorAttachment.getClearValue().setR(clearColor.r);
        renderPassColorAttachment.getClearValue().setG(clearColor.g);
        renderPassColorAttachment.getClearValue().setB(clearColor.b);
        renderPassColorAttachment.getClearValue().setA(clearColor.a);

        renderPassColorAttachment.setDepthSlice(WGPU.WGPU_DEPTH_SLICE_UNDEFINED);


        WGPURenderPassDepthStencilAttachment depthStencilAttachment = WGPURenderPassDepthStencilAttachment.createDirect();
        depthStencilAttachment.setView(LibGPU.app.depthTextureView);
        depthStencilAttachment.setDepthClearValue(1.0f);
        depthStencilAttachment.setDepthLoadOp(WGPULoadOp.Clear);
        depthStencilAttachment.setDepthStoreOp(WGPUStoreOp.Store);
        depthStencilAttachment.setDepthReadOnly(0L);
        depthStencilAttachment.setStencilClearValue(0);
        depthStencilAttachment.setStencilLoadOp(WGPULoadOp.Undefined);
        depthStencilAttachment.setStencilStoreOp(WGPUStoreOp.Undefined);
        depthStencilAttachment.setStencilReadOnly(1L);

        WGPURenderPassDescriptor renderPassDescriptor = WGPURenderPassDescriptor.createDirect();
        renderPassDescriptor.setNextInChain();

        renderPassDescriptor.setLabel("Render Pass");

        renderPassDescriptor.setColorAttachmentCount(1);
        renderPassDescriptor.setColorAttachments(renderPassColorAttachment);
        renderPassDescriptor.setOcclusionQuerySet(WgpuJava.createNullPointer());
        renderPassDescriptor.setDepthStencilAttachment(depthStencilAttachment);

        //gpuTiming.configureRenderPassDescriptor(renderPassDescriptor);

        RenderPass pass = new RenderPass(wgpu.CommandEncoderBeginRenderPass(encoder, renderPassDescriptor));
        if(viewport != null)
            viewport.apply(pass);
        return pass;
    }

    public void end() {
        wgpu.RenderPassEncoderEnd(renderPass);
        wgpu.RenderPassEncoderRelease(renderPass);
    }

    public Pointer getPointer() {
        return renderPass;
    }


    public static void setClearColor(Color color) {
        clearColor.set(color);
    }

    public static void setClearColor(float r, float g, float b, float a) {
        clearColor.set(r, g, b, a);
    }

    public static void setViewport(Viewport vp){
        viewport = vp;
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
