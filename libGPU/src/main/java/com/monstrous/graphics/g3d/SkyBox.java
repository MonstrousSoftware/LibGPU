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

package com.monstrous.graphics.g3d;

import com.monstrous.LibGPU;
import com.monstrous.graphics.Camera;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.webgpu.*;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;
import com.monstrous.utils.Disposable;
import com.monstrous.webgpu.*;

/**
 * SkyBox
 * Following the approach from https://webgpufundamentals.org/webgpu/lessons/webgpu-skybox.html
 * This uses a dedicated shader that renders one screen filling triangle using a cube map.
 *  The camera projection view matrix is inverted and use to look up screen pixels in the cube map.
 *  The sky box should be rendered after all opaque renderables.
  */


public class SkyBox implements Disposable {

    private final int FRAME_UB_SIZE = 16*Float.BYTES;   // to hold one 4x4 matrix

    private final Texture cubeMap;
    private final UniformBuffer uniformBuffer;
    private final BindGroupLayout bindGroupLayout;
    private final BindGroup bindGroup;

    private final Pipeline pipeline;
    private final PipelineSpecification pipelineSpec;
    private final Matrix4 invertedProjectionView;


    public SkyBox( Texture cubeMap ){
        this.cubeMap = cubeMap;

        uniformBuffer = new UniformBuffer(FRAME_UB_SIZE, WGPUBufferUsage.CopyDst | WGPUBufferUsage.Uniform);

        bindGroupLayout = createBindGroupLayout();

        PipelineLayout pipelineLayout = new PipelineLayout("SkyBox Pipeline Layout", bindGroupLayout);

        pipelineSpec = new PipelineSpecification();
        pipelineSpec.name = "skybox pipeline";
        pipelineSpec.vertexAttributes = null;
        pipelineSpec.environment = null;
        pipelineSpec.shader = null;
        pipelineSpec.shaderFilePath =  "shaders/skybox.wgsl"; //Files.classpath("shaders/skybox.wgsl");
        pipelineSpec.enableDepthTest();
        pipelineSpec.setCullMode(WGPUCullMode.Back);
        pipelineSpec.colorFormat = LibGPU.surfaceFormat;
        pipelineSpec.depthFormat = WGPUTextureFormat.Depth24Plus;
        pipelineSpec.numSamples = LibGPU.app.configuration.numSamples;
        pipelineSpec.isSkyBox = true;

        pipeline = new Pipeline(pipelineLayout.getHandle(), pipelineSpec);

        bindGroup = makeBindGroup(bindGroupLayout, uniformBuffer);  // move to constructor along with BG release?

        invertedProjectionView = new Matrix4();
    }

    public void render(Camera camera, RenderPass pass){
        writeUniforms(uniformBuffer, camera);

        pass.setPipeline(pipeline.getPipeline());
        pass.setBindGroup(0, bindGroup.getHandle());
        pass.draw(3);
    }


    @Override
    public void dispose() {
        pipeline.dispose();
        bindGroup.dispose();
        bindGroupLayout.dispose();
        uniformBuffer.dispose();
    }


    // Bind Group Layout:
    //  0 uniforms,
    //  1 cube map texture,
    //  2 sampler

    private BindGroupLayout createBindGroupLayout(){

        // Define binding layout
        BindGroupLayout layout = new BindGroupLayout();
        int location = 0;
        layout.begin();
        layout.addBuffer(location++, WGPUShaderStage.Fragment, WGPUBufferBindingType.Uniform, FRAME_UB_SIZE, false);    // uniform buffer
        layout.addTexture(location++, WGPUShaderStage.Fragment, WGPUTextureSampleType.Float,WGPUTextureViewDimension.Cube, false );  // cube map texture
        layout.addSampler(location, WGPUShaderStage.Fragment, WGPUSamplerBindingType.Filtering);  // cube map sampler
        layout.end();
        return layout;
    }

    //  bind group
    private BindGroup makeBindGroup(BindGroupLayout bindGroupLayout, Buffer uniformBuffer) {
        BindGroup bg = new BindGroup(bindGroupLayout);
        bg.begin();
        bg.addBuffer(0, uniformBuffer);
        bg.addTexture(1, cubeMap.getTextureView());
        bg.addSampler(2, cubeMap.getSampler());
        bg.end();
        return bg;
    }


    private void writeUniforms( UniformBuffer uniformBuffer, Camera camera ){
        invertedProjectionView.set(camera.combined);
        invertedProjectionView.setTranslation(Vector3.Zero);
        invertedProjectionView.inv();

        uniformBuffer.beginFill();
        uniformBuffer.append(invertedProjectionView);
        uniformBuffer.endFill();   // write to GPU buffer
    }
}
