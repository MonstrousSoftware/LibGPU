package com.monstrous.graphics.webgpu;

import com.monstrous.LibGPU;
import com.monstrous.graphics.Color;
import com.monstrous.wgpu.*;
import com.monstrous.wgpuUtils.WgpuJava;
import jnr.ffi.Pointer;

import static com.monstrous.LibGPU.wgpu;

public class RenderPass {


    private static final Color clearColor = new Color(Color.BLACK);
    private final Pointer renderPass;

    private RenderPass(Pointer renderPass) {
        this.renderPass = renderPass;
    }

    public static RenderPass create(Pointer encoder){

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

        renderPassColorAttachment.setDepthSlice(wgpu.WGPU_DEPTH_SLICE_UNDEFINED);


        WGPURenderPassDepthStencilAttachment depthStencilAttachment = WGPURenderPassDepthStencilAttachment.createDirect();
        depthStencilAttachment.setView( LibGPU.app.depthTextureView );
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
        renderPassDescriptor.setColorAttachments( renderPassColorAttachment );
        renderPassDescriptor.setOcclusionQuerySet(WgpuJava.createNullPointer());
        renderPassDescriptor.setDepthStencilAttachment( depthStencilAttachment );

        //gpuTiming.configureRenderPassDescriptor(renderPassDescriptor);

        return new RenderPass(wgpu.CommandEncoderBeginRenderPass(encoder, renderPassDescriptor));
    }

    public void end() {
        wgpu.RenderPassEncoderEnd(renderPass);
        wgpu.RenderPassEncoderRelease(renderPass);
    }

    public Pointer getPointer(){
        return renderPass;
    }


    public static void setClearColor(Color color){
        clearColor.set(color);
    }
}
