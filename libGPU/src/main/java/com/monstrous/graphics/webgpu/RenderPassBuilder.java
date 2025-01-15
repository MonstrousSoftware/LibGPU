package com.monstrous.graphics.webgpu;

import com.monstrous.LibGPU;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.Texture;
import com.monstrous.utils.viewports.Viewport;
import com.monstrous.wgpu.*;
import com.monstrous.wgpuUtils.WgpuJava;
import jnr.ffi.Pointer;

import static com.monstrous.LibGPU.wgpu;

// Factory class to create RenderPass objects.
//
// use setCommandEncoder() before creating passes.
// use create() to create a pass (at least once per frame)
// use setClearColor() to set the background color of future passes. (see ScreenUtils.clear() )
// use setViewport() to apply a viewport on future passes created.


public class RenderPassBuilder {

    private static Pointer encoder;
    private static final Color clearColor = new Color(Color.BLACK);
    private static Texture outputTexture;
    private static Texture outputDepthTexture;
    private static Viewport viewport = null;
    private static WGPURenderPassColorAttachment renderPassColorAttachment;
    private static WGPURenderPassDepthStencilAttachment depthStencilAttachment;
    private static WGPURenderPassDescriptor renderPassDescriptor;

    public static void setCommandEncoder(Pointer commandEncoder) {
        encoder = commandEncoder;
    }

    public static RenderPass create() {
        return create(null, null);
    }

    public static RenderPass create(Texture outTexture, Texture outDepthTexture) {
        if(encoder == null)
            throw new RuntimeException("Encoder must be set before calling RenderPass.create()");

        outputTexture = outTexture;
        outputDepthTexture = outDepthTexture;
        WGPUTextureFormat colorFormat;
        WGPUTextureFormat depthFormat;

        if(renderPassColorAttachment == null) {
            renderPassColorAttachment = WGPURenderPassColorAttachment.createDirect();
            renderPassColorAttachment.setNextInChain();
            renderPassColorAttachment.setResolveTarget(WgpuJava.createNullPointer());
            renderPassColorAttachment.setLoadOp(WGPULoadOp.Clear);
            renderPassColorAttachment.setStoreOp(WGPUStoreOp.Store);

            renderPassColorAttachment.setDepthSlice(WGPU.WGPU_DEPTH_SLICE_UNDEFINED);
        }
        renderPassColorAttachment.getClearValue().setR(clearColor.r);
        renderPassColorAttachment.getClearValue().setG(clearColor.g);
        renderPassColorAttachment.getClearValue().setB(clearColor.b);
        renderPassColorAttachment.getClearValue().setA(clearColor.a);

        if(outputTexture == null) {
            renderPassColorAttachment.setView(LibGPU.app.targetView);
            colorFormat = LibGPU.surfaceFormat;
            depthFormat = WGPUTextureFormat.Depth24Plus;    // todo
        }
        else {
            renderPassColorAttachment.setView(outputTexture.getTextureView());
            colorFormat = outputTexture.getFormat();
        }



        if(depthStencilAttachment == null) {
            depthStencilAttachment = WGPURenderPassDepthStencilAttachment.createDirect();
            depthStencilAttachment.setDepthClearValue(1.0f);
            depthStencilAttachment.setDepthLoadOp(WGPULoadOp.Clear);
            depthStencilAttachment.setDepthStoreOp(WGPUStoreOp.Store);
            depthStencilAttachment.setDepthReadOnly(0L);
            depthStencilAttachment.setStencilClearValue(0);
            depthStencilAttachment.setStencilLoadOp(WGPULoadOp.Undefined);
            depthStencilAttachment.setStencilStoreOp(WGPUStoreOp.Undefined);
            depthStencilAttachment.setStencilReadOnly(1L);
        }
        if(outDepthTexture == null) {
            depthFormat = WGPUTextureFormat.Depth24Plus;    // todo
            depthStencilAttachment.setView(LibGPU.app.depthTextureView);
        }
        else {
            depthFormat = outDepthTexture.getFormat();
            depthStencilAttachment.setView(outputDepthTexture.getTextureView());
        }

        if(renderPassDescriptor == null) {
            renderPassDescriptor = WGPURenderPassDescriptor.createDirect();
            renderPassDescriptor.setNextInChain();

            renderPassDescriptor.setLabel("Render Pass");
            renderPassDescriptor.setOcclusionQuerySet(WgpuJava.createNullPointer());
        }
        renderPassDescriptor.setDepthStencilAttachment(depthStencilAttachment);
        renderPassDescriptor.setColorAttachmentCount(1);
        renderPassDescriptor.setColorAttachments(renderPassColorAttachment);


        //gpuTiming.configureRenderPassDescriptor(renderPassDescriptor);

        Pointer renderPassPtr = wgpu.CommandEncoderBeginRenderPass(encoder, renderPassDescriptor);
        RenderPass pass = new RenderPass(renderPassPtr, colorFormat, depthFormat);
        if(viewport != null)
            viewport.apply(pass);
        return pass;
    }


    public static Texture getOutputTexture(){
        return outputTexture;
    }

    public static void setClearColor(Color color) {
        clearColor.set(color);
    }

    public static void setClearColor(float r, float g, float b, float a) {
        clearColor.set(r, g, b, a);
    }

    // set viewport on future render passes created, set to null to not apply a viewport.
    public static void setViewport(Viewport vp){
        viewport = vp;
    }

}
