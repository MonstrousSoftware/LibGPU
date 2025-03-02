package com.monstrous.graphics.webgpu;

import com.monstrous.LibGPU;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.Texture;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.utils.viewports.Viewport;
import com.monstrous.webgpu.*;
import jnr.ffi.Pointer;
import org.jetbrains.annotations.Nullable;

import static com.monstrous.LibGPU.webGPU;

/** Factory class to create RenderPass objects.
 *  use setCommandEncoder() before creating passes.
 *  use create() to create a pass (at least once per frame)
 *  use setClearColor() to set the background color of future passes. (see ScreenUtils.clear() )
 *  use setViewport() to apply a viewport on the next render pass.
 */
public class RenderPassBuilder {

    private static Pointer encoder;
    private static final Color defaultClearColor = new Color(Color.BLACK);
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
        return create(null, null, null, 1);
    }

    public static RenderPass create(Color clearColor) {
        return create(clearColor, null, null, 1);
    }


    // clearColor: can be null the default clear color will be used or if none is set (see ScreenUtils.clear) the screen
    // is not cleared.
    public static RenderPass create(Color clearColor, Texture outTexture, Texture outDepthTexture, int sampleCount) {
        if(encoder == null)
            throw new RuntimeException("Encoder must be set before calling RenderPass.create()");

        outputTexture = outTexture;
        outputDepthTexture = outDepthTexture;
        WGPUTextureFormat colorFormat;
        WGPUTextureFormat depthFormat;

        renderPassColorAttachment = WGPURenderPassColorAttachment.createDirect();
        renderPassColorAttachment.setNextInChain();

        renderPassColorAttachment.setStoreOp(WGPUStoreOp.Store);

        renderPassColorAttachment.setDepthSlice(-1L);

        renderPassColorAttachment.setLoadOp((clearColor != null) ? WGPULoadOp.Clear : WGPULoadOp.Load);

//        if(clearColor == null && mustClear){
//            clearColor = defaultClearColor;
//        }

        if(clearColor != null) {
            renderPassColorAttachment.getClearValue().setR(clearColor.r);
            renderPassColorAttachment.getClearValue().setG(clearColor.g);
            renderPassColorAttachment.getClearValue().setB(clearColor.b);
            renderPassColorAttachment.getClearValue().setA(clearColor.a);
        }

        if(outputTexture == null) {
            if(sampleCount > 1){
                renderPassColorAttachment.setView(LibGPU.app.multiSamplingTexture.getTextureView());
                renderPassColorAttachment.setResolveTarget(LibGPU.app.targetView);
            } else {
                renderPassColorAttachment.setView(LibGPU.app.targetView);
                renderPassColorAttachment.setResolveTarget(JavaWebGPU.createNullPointer());
            }
            colorFormat = LibGPU.surfaceFormat;
            //depthFormat = WGPUTextureFormat.Depth24Plus;    // todo
        }
        else {
            renderPassColorAttachment.setView(outputTexture.getTextureView());
            renderPassColorAttachment.setResolveTarget(JavaWebGPU.createNullPointer());
            colorFormat = outputTexture.getFormat();
            sampleCount = 1;
        }
        depthStencilAttachment = WGPURenderPassDepthStencilAttachment.createDirect();
        depthStencilAttachment.setDepthClearValue(1.0f);
        depthStencilAttachment.setDepthLoadOp(WGPULoadOp.Clear);
        depthStencilAttachment.setDepthStoreOp(WGPUStoreOp.Store);
        depthStencilAttachment.setDepthReadOnly(0L);
        depthStencilAttachment.setStencilClearValue(0);
        depthStencilAttachment.setStencilLoadOp(WGPULoadOp.Undefined);
        depthStencilAttachment.setStencilStoreOp(WGPUStoreOp.Undefined);
        depthStencilAttachment.setStencilReadOnly(1L);

        if(outDepthTexture == null) {
            depthFormat = WGPUTextureFormat.Depth24Plus;    // todo
            depthStencilAttachment.setView(LibGPU.app.depthTextureView);
        }
        else {
            depthFormat = outDepthTexture.getFormat();
            depthStencilAttachment.setView(outputDepthTexture.getTextureView());
        }

        renderPassDescriptor = WGPURenderPassDescriptor.createDirect();
        renderPassDescriptor.setNextInChain();

        renderPassDescriptor.setLabel("Render Pass");
        renderPassDescriptor.setOcclusionQuerySet(JavaWebGPU.createNullPointer());

        renderPassDescriptor.setDepthStencilAttachment(depthStencilAttachment);
        renderPassDescriptor.setColorAttachmentCount(1);
        renderPassDescriptor.setColorAttachments(renderPassColorAttachment);


        LibGPU.app.gpuTiming.configureRenderPassDescriptor(renderPassDescriptor);

        Pointer renderPassPtr = webGPU.wgpuCommandEncoderBeginRenderPass(encoder, renderPassDescriptor);
        RenderPass pass = new RenderPass(renderPassPtr, colorFormat, depthFormat, sampleCount,
                outputTexture == null ? LibGPU.graphics.getWidth() : outputTexture.getWidth(),
                outputTexture == null ? LibGPU.graphics.getHeight() : outputTexture.getHeight());
        if(viewport != null) {
            viewport.apply(pass);
            viewport = null;        // apply only once after setViewport() is called
        }
        return pass;
    }


    public static Texture getOutputTexture(){
        return outputTexture;
    }

    // color null to not clear screen
//    public static void setClearColor(Color color) {
//        if(color == null){
//            mustClear = false;
//        }
//        else {
//            defaultClearColor.set(color);
//            mustClear = true;
//        }
//    }

//    public static void setClearColor(float r, float g, float b, float a) {
//        defaultClearColor.set(r, g, b, a);
//    }

    // set viewport on future render passes created, set to null to not apply a viewport.
    public static void setViewport(Viewport vp){
        viewport = vp;
    }

}
