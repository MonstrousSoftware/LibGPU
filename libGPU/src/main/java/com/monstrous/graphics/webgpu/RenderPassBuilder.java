/*******************************************************************************
 * Copyright 2025 Monstrous Software.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.monstrous.graphics.webgpu;

import com.monstrous.LibGPU;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.Texture;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.utils.viewports.Viewport;
import com.monstrous.webgpu.*;
import jnr.ffi.Pointer;

import static com.monstrous.LibGPU.webGPU;

/** Factory class to create RenderPass objects.
 *  use setCommandEncoder() before creating passes.
 *  use create() to create a pass (at least once per frame)
 *  use setClearColor() to set the background color of future passes. (see ScreenUtils.clear() )
 *  use setViewport() to apply a viewport on the next render pass.
 */
public class RenderPassBuilder {

    private static Viewport viewport = null;

    public static RenderPass create() {
        return create(null);
    }

    public static RenderPass create(Color clearColor) {
        return create(clearColor,  1);
    }

    public static RenderPass create(Color clearColor, int sampleCount) {
        return create(clearColor, null, LibGPU.app.depthTextureFormat, LibGPU.app.depthTextureView, sampleCount);
    }


    public static RenderPass create(Color clearColor, Texture outTexture, WGPUTextureFormat depthFormat, Pointer depthTextureView, int sampleCount){
        return create("color pass", clearColor, outTexture, depthFormat, depthTextureView, sampleCount, RenderPassType.COLOR_PASS);
    }


    /**
     * Create a render pass
     *
     * @param clearColor    background color, null to not clear the screen, e.g. for a UI
     * @param outTexture    output texture, null to render to the screen
     * @param depthFormat/depthTextureView   output depth texture, can be null
     * @param sampleCount       samples per pixel: 1 or 4
     * @param passType
     * @return
     */
    public static RenderPass create(String name, Color clearColor, Texture outTexture,  WGPUTextureFormat depthFormat, Pointer depthTextureView, int sampleCount, RenderPassType passType) {
        if(LibGPU.commandEncoder == null)
            throw new RuntimeException("Encoder must be set before calling RenderPass.create()");

        WGPUTextureFormat colorFormat = WGPUTextureFormat.Undefined;

        WGPURenderPassDescriptor renderPassDescriptor = WGPURenderPassDescriptor.createDirect();
        renderPassDescriptor.setNextInChain().setLabel(name).setOcclusionQuerySet(JavaWebGPU.createNullPointer());


        if(  passType == RenderPassType.COLOR_PASS || passType == RenderPassType.COLOR_PASS_AFTER_DEPTH_PREPASS ||passType == RenderPassType.SHADOW_PASS ||passType == RenderPassType.NO_DEPTH){  // todo TEMP SHADOW FOR DEBUG

            WGPURenderPassColorAttachment renderPassColorAttachment = WGPURenderPassColorAttachment.createDirect();
            renderPassColorAttachment.setNextInChain();

            renderPassColorAttachment.setStoreOp(WGPUStoreOp.Store);

            renderPassColorAttachment.setDepthSlice(-1L);

            renderPassColorAttachment.setLoadOp((clearColor != null) ? WGPULoadOp.Clear : WGPULoadOp.Load);

            if (clearColor != null) {
                renderPassColorAttachment.getClearValue().setR(clearColor.r);
                renderPassColorAttachment.getClearValue().setG(clearColor.g);
                renderPassColorAttachment.getClearValue().setB(clearColor.b);
                renderPassColorAttachment.getClearValue().setA(clearColor.a);
            }

            if (outTexture == null) {
                if ( sampleCount > 1) {
                    renderPassColorAttachment.setView(LibGPU.app.multiSamplingTexture.getTextureView());
                    renderPassColorAttachment.setResolveTarget(LibGPU.app.targetView);
                } else {
                    renderPassColorAttachment.setView(LibGPU.app.targetView);
                    renderPassColorAttachment.setResolveTarget(JavaWebGPU.createNullPointer());
                }
                colorFormat = LibGPU.surfaceFormat;

            } else {
                renderPassColorAttachment.setView(outTexture.getTextureView());
                renderPassColorAttachment.setResolveTarget(JavaWebGPU.createNullPointer());
                colorFormat = outTexture.getFormat();
                sampleCount = 1;
            }

            renderPassDescriptor.setColorAttachmentCount(1);
            renderPassDescriptor.setColorAttachments(renderPassColorAttachment);
        } else {
            sampleCount = 1;
            renderPassDescriptor.setColorAttachmentCount(0);
        }

        if(passType != RenderPassType.NO_DEPTH) {
            WGPURenderPassDepthStencilAttachment depthStencilAttachment = WGPURenderPassDepthStencilAttachment.createDirect();
            depthStencilAttachment.setDepthClearValue(1.0f);
            // if we just did a depth prepass, don't clear the depth buffer
            depthStencilAttachment.setDepthLoadOp(passType == RenderPassType.COLOR_PASS_AFTER_DEPTH_PREPASS ? WGPULoadOp.Load : WGPULoadOp.Clear);
            depthStencilAttachment.setDepthStoreOp(WGPUStoreOp.Store);
            depthStencilAttachment.setDepthReadOnly(0L);
            depthStencilAttachment.setStencilClearValue(0);
            depthStencilAttachment.setStencilLoadOp(WGPULoadOp.Undefined);
            depthStencilAttachment.setStencilStoreOp(WGPUStoreOp.Undefined);
            depthStencilAttachment.setStencilReadOnly(1L);

            depthStencilAttachment.setView(depthTextureView);

            renderPassDescriptor.setDepthStencilAttachment(depthStencilAttachment);
        }

        LibGPU.app.gpuTiming.configureRenderPassDescriptor(renderPassDescriptor);



        Pointer renderPassPtr = webGPU.wgpuCommandEncoderBeginRenderPass(LibGPU.commandEncoder, renderPassDescriptor);
        RenderPass pass = new RenderPass(renderPassPtr, passType, colorFormat, depthFormat, sampleCount,
                outTexture == null ? LibGPU.graphics.getWidth() : outTexture.getWidth(),
                outTexture == null ? LibGPU.graphics.getHeight() : outTexture.getHeight());


        if(viewport != null) {
            viewport.apply(pass);
            viewport = null;        // apply only once after setViewport() is called
        }
        return pass;
    }


    // set viewport on future render passes created, set to null to not apply a viewport.
    public static void setViewport(Viewport vp){
        viewport = vp;
    }

}
