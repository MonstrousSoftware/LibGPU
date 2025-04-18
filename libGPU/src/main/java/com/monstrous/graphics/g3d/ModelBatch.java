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
import com.monstrous.graphics.*;
import com.monstrous.graphics.lights.DirectionalLight;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.graphics.lights.Light;
import com.monstrous.graphics.lights.PointLight;
import com.monstrous.graphics.webgpu.*;
import com.monstrous.math.Matrix4;
import com.monstrous.math.Vector3;
import com.monstrous.utils.Disposable;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.webgpu.*;
import jnr.ffi.Pointer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModelBatch implements Disposable {

    private final int MAX_PASSES = 40;
    private final int MAX_DIR_LIGHTS = 5;
    private final int MAX_POINT_LIGHTS = 5;
    private final int MAX_INSTANCES = 4096;

    private final int FRAME_UB_SIZE = 816;      // check...

    private final WebGPU_JNI webGPU;
    private final Pointer device;

    private RenderPass pass;

    private UniformBuffer frameUniformBuffer;
    private final BindGroupLayout frameBindGroupLayout;
    private final BindGroupLayout instancingBindGroupLayout;
    private final BindGroupLayout skinningBindGroupLayout;
    private PipelineLayout pipelineLayout;
    private Pointer sampler;
    private BindGroup frameBindGroup;
    private BindGroup instancingBindGroup;
    private BindGroup skinningBindGroup;
    private Material prevMaterial;
    private ModelInstance prevModelInstance;
    private Mesh currentMesh;
    private Camera camera;

    private final Pipelines pipelines;
    private Pipeline prevPipeline;
    private RenderPassType passType;
    private PipelineSpecification pipelineSpec;
    private final List<Renderable> renderables;
    private final List<Renderable> visibleRenderables;
    private final RenderablePool pool;

    public Environment environment;
    public int numPipelines;
    public int numPipelineSwitches;
    public int materialSwitches;
    public int drawCalls;
    public int numEmitted;
    public int instancingJoins; // number of renderables that could be combined by instancing

    private DirectionalLight defaultDirectionalLight;
    private UniformBuffer instanceBuffer;
    private CubeMap dummyCubemap;
    private Texture dummyShadowMap;
    private Texture dummy2DTexture;
    private Buffer dummyBuffer;




    public ModelBatch (){
        webGPU = LibGPU.webGPU;
        device = LibGPU.device.getHandle();

        pipelines = new Pipelines();
        renderables = new ArrayList<>();
        visibleRenderables = new ArrayList<>();
        pool = new RenderablePool(1000);
        pipelineSpec = new PipelineSpecification();

        defaultDirectionalLight = new DirectionalLight(new Color(1,0,0,1), new Vector3(0, -1, 0));

        frameUniformBuffer = new UniformBuffer( FRAME_UB_SIZE, WGPUBufferUsage.CopyDst | WGPUBufferUsage.Uniform, MAX_PASSES);

        frameBindGroupLayout = createFrameBindGroupLayout();
        instancingBindGroupLayout = createInstancingBindGroupLayout();


        skinningBindGroupLayout = createSkinningBindGroupLayout();
        // fallback for skinning buffers
        dummyBuffer = new Buffer("dummy buffer", WGPUBufferUsage.CopyDst | WGPUBufferUsage.Storage, 16 * Float.BYTES);

        pipelineLayout = new PipelineLayout("ModelBatch Pipeline Layout", frameBindGroupLayout, Material.getBindGroupLayout(),instancingBindGroupLayout, skinningBindGroupLayout);

        dummyShadowMap =  new Texture(1, 1, false, true, WGPUTextureFormat.Depth32Float, 1);
        dummyCubemap = new CubeMap(1,1);
        dummy2DTexture = new Texture(1,1);

        int instanceSize = 16*Float.BYTES;      // data size per instance
        // todo is this a uniform buffer or a storage buffer?
        instanceBuffer = new UniformBuffer(instanceSize, WGPUBufferUsage.CopyDst | WGPUBufferUsage.Storage, MAX_INSTANCES);

        sampler = makeShadowSampler();
    }

    /** Call this to hot-load shaders as they will all be recompiled. */
    public void invalidatePipelines(){
        pipelines.clear();
    }


    public void begin(Camera camera){
        begin(camera, null, null, null, null);
    }

    public void begin(Camera camera, Environment environment) {
        begin(camera, environment, null, null, null);
    }

    public void begin(Camera camera, Environment environment, Color clearColor) {
        begin(camera, environment, clearColor, null, null);
    }

    public void begin(Camera camera, Environment environment, Color clearColor, Texture outputTexture, Texture depthTexture){
        begin(camera, environment, clearColor, outputTexture, depthTexture, RenderPassType.COLOR_PASS);
    }

    public void begin(Camera camera, Environment environment, Color clearColor, Texture outputTexture, Texture depthTexture, RenderPassType passType){
        this.camera = camera;
        this.environment = environment;
        this.passType = passType;

        // create a new render pass

        int samples = (outputTexture != null) ? outputTexture.getNumSamples() : LibGPU.app.configuration.numSamples;
        if(passType == RenderPassType.SHADOW_PASS || passType == RenderPassType.DEPTH_PREPASS)
            samples = 1;
        if(depthTexture == null)
            pass = RenderPassBuilder.create(passType.name(), clearColor, outputTexture, LibGPU.app.depthTextureFormat, LibGPU.app.depthTextureView, samples, passType );
        else
            pass = RenderPassBuilder.create(passType.name(), clearColor, outputTexture, depthTexture.getFormat(), depthTexture.getTextureView(), samples, passType );

        prevMaterial = null;
        prevPipeline = null;
        prevModelInstance = null;
        numPipelineSwitches = 0;
        materialSwitches = 0;
        drawCalls = 0;
        numEmitted = 0;
        instancingJoins = 0;

        writeFrameUniforms(frameUniformBuffer, camera, environment, LibGPU.graphics.passNumber);
        frameBindGroup = makeFrameBindGroup(frameBindGroupLayout, sampler, frameUniformBuffer);

        // use dynamic offset for the frame uniform buffer slice of this pass
        pass.setBindGroup(0, frameBindGroup.getHandle(), LibGPU.graphics.passNumber * frameUniformBuffer.getUniformStride());
        LibGPU.graphics.passNumber++;

        instancingBindGroup = createInstancingBindGroup(instancingBindGroupLayout, instanceBuffer);
        pass.setBindGroup(2, instancingBindGroup.getHandle());

        // set skinning bind group to use fake non-zero buffer to avoid complaints from webgpu
        skinningBindGroup = createSkinningBindGroup(skinningBindGroupLayout, dummyBuffer, dummyBuffer);
        pass.setBindGroup(3, skinningBindGroup.getHandle());
    }

    public void render(ArrayList<ModelInstance> instances) {
        for(ModelInstance instance : instances)
            render(instance);
    }

    public void render(ModelInstance instance){

        instance.getRenderables((ArrayList<Renderable>) renderables, pool);
    }

    public void render(Renderable renderable) {
        renderables.add( renderable );
    }


//    public void render(MeshPart meshPart, Material material, Matrix4 modelMatrix) {
//        renderables.add( new Renderable(meshPart, material, modelMatrix));
//    }



    public void end(){
        finalizeRenderables();
        emitRenderables();
        close();

    }


    // sort comparator to sort on material to reduce number of material switches.
    //
    private static class RenderableComparator implements Comparator<Renderable> {
        @Override
        public int compare(Renderable r1, Renderable r2) {
            //return r1.meshPart.hashCode() - r2.meshPart.hashCode();
            return r1.material.sortCode() - r2.material.sortCode();
        }
    }

    private final RenderableComparator comparator = new RenderableComparator();

    private MeshPart prevMeshPart;
    private int instanceCount;
    private int renderablesCount;

    /** sort the gathered renderables and perform frustum culling to populate visibleRenderables */
    private void finalizeRenderables() {
        // sort renderables to minimize material switching, to do: depth sorting etc.
        renderables.sort(comparator);

        // culling
        visibleRenderables.clear();
        for(Renderable renderable : renderables) {
            if(isVisible(renderable))
                visibleRenderables.add(renderable);
            else
                pool.free(renderable);
        }
        renderables.clear();
    }

    /** issue draw calls for the visibleRenderables */
    private void emitRenderables(){
        prevMeshPart = null;
        instanceCount = 0;
        renderablesCount = 0;
        currentMesh = null;
        for(Renderable renderable : visibleRenderables) {
            emit(renderable);
        }
        emitMeshPart(prevMeshPart, instanceCount, renderablesCount);
    }

    /** skybox rendering and clean-up */
    private void close(){
        for(Renderable renderable : visibleRenderables) {
            pool.free(renderable);
        }
        visibleRenderables.clear();

        frameBindGroup.dispose();
        instancingBindGroup.dispose();

        if(passType == RenderPassType.COLOR_PASS && environment.skybox != null) // todo move out skybox rendering
            environment.skybox.render(camera, pass);
        pass.end();
        pass = null;

        numPipelines = pipelines.size();        // for statistics
    }

    private BoundingBox bbox = new BoundingBox();

    /** Frustum culling using a transformed mesh bounding box */
    private boolean isVisible(Renderable renderable){
        return true;
//        bbox.set(renderable.meshPart.getMesh().boundingBox);
//        bbox.transform(renderable.modelTransform);
//        return camera.frustum.boundsInFrustum(bbox);
    }


    // this will actually generate the draw calls for the renderable
    private void emit(Renderable renderable) {
        numEmitted++;
        emit(renderable.meshPart, renderable.material, renderable.modelTransform, renderable.modelInstance);
    }

    public void emit(MeshPart meshPart, Material material, Matrix4 modelMatrix, ModelInstance modelInstance){

        // gather identical meshParts to be drawn in one call using instancing
        if(meshPart != prevMeshPart) {
            emitMeshPart(prevMeshPart, instanceCount, renderablesCount);
            instanceCount = 0;
            prevMeshPart = meshPart;
        } else {
            instancingJoins++;
        }
        addInstance(renderablesCount, modelMatrix);
        renderablesCount++; // nr of renderables in buffer
        instanceCount++;    // nr of instances of the same meshPart

        // if we change material, bind new material
        if(material != prevMaterial) {
            prevMaterial = material;
            material.bindGroup(pass, 1);    // group 1 is material bind group
            materialSwitches++;
        }

        // modelInstance switch affects skinning
        if(modelInstance != prevModelInstance){
            prevModelInstance = modelInstance;
            if(modelInstance != null && modelInstance.jointBuffer != null) {
                updateJointBuffer(modelInstance);
                // note: perhaps the bind group should be part of model instance
                skinningBindGroup = createSkinningBindGroup(skinningBindGroupLayout, modelInstance.jointBuffer, modelInstance.model.inverseBoneBuffer);
                pass.setBindGroup(3, skinningBindGroup.getHandle());
            }
        }
    }

    // make a draw call
    private void emitMeshPart(MeshPart meshPart, int instanceCount, int renderablesCount) {
        if(meshPart == null)
            return;

        // switch mesh? bind vertex buffer and index buffer
        if(meshPart.getMesh() != currentMesh){
            currentMesh = meshPart.getMesh();
            Pointer vertexBuffer = currentMesh.getVertexBuffer().getHandle();
            pass.setVertexBuffer(0, vertexBuffer, 0, currentMesh.getVertexBuffer().getSize());
            if (currentMesh.getIndexCount() > 0) { // indexed mesh?
                Pointer indexBuffer = currentMesh.getIndexBuffer().getHandle();
                pass.setIndexBuffer(indexBuffer, meshPart.getMesh().getIndexBuffer().getFormat(), 0, currentMesh.getIndexBuffer().getSize());
            }
        }




        setPipeline(pass,  meshPart, environment);

        if (meshPart.getMesh().getIndexCount() > 0)  // indexed mesh?
            pass.drawIndexed( meshPart.getSize(), instanceCount, meshPart.getOffset(), 0, renderablesCount-instanceCount);
        else
            pass.draw(meshPart.getSize(), instanceCount, meshPart.getOffset(), renderablesCount-instanceCount);

        drawCalls++;
    }

    private String selectShaderSourceFile(RenderPassType passType, Environment environment) {

        if(passType == RenderPassType.SHADOW_PASS)
            return "shaders/modelbatchDepth.wgsl";
        else if (passType == RenderPassType.DEPTH_PREPASS)
            return "shaders/modelbatchDepthPrepass.wgsl";
        if(environment.shaderSourcePath != null)
            return environment.shaderSourcePath;
        return "shaders/modelbatchPBRUber.wgsl";
        //return "shaders/modelbatchEquilateral.wgsl";            /// TODO TEMP!!
    }



    // create or reuse pipeline on demand when we know the model
    private void setPipeline(RenderPass pass, MeshPart meshPart, Environment environment ) {

        pipelineSpec.vertexAttributes = meshPart.getMesh().vertexAttributes;
        pipelineSpec.environment = environment;
        pipelineSpec.shader = null;
        pipelineSpec.shaderFilePath = selectShaderSourceFile(pass.type, environment);
        pipelineSpec.useDepthTest = true;
        pipelineSpec.noDepthAttachment = (pass.type == RenderPassType.NO_DEPTH);
        pipelineSpec.setCullMode(WGPUCullMode.Back);
        pipelineSpec.isDepthPass = (pass.type == RenderPassType.SHADOW_PASS || pass.type == RenderPassType.DEPTH_PREPASS);
        pipelineSpec.afterDepthPrepass = (pass.type == RenderPassType.COLOR_PASS_AFTER_DEPTH_PREPASS);
        pipelineSpec.colorFormat = pass.getColorFormat();    // pixel format of render pass output
        pipelineSpec.depthFormat = pass.getDepthFormat();
        pipelineSpec.numSamples = pipelineSpec.isDepthPass ? 1 : pass.getSampleCount();
        pipelineSpec.topology = meshPart.getTopology();
        pipelineSpec.indexFormat = meshPart.getMesh().getIndexBuffer().getFormat();
        pipelineSpec.recalcHash();

        Pipeline pipeline = pipelines.findPipeline(pipelineLayout.getHandle(), pipelineSpec);
        if (pipeline != prevPipeline) { // avoid unneeded switches
            pass.setPipeline(pipeline.getHandle());
            prevPipeline = pipeline;
            numPipelineSwitches++;
        }
    }


    @Override
    public void dispose() {
        pipelines.dispose();
        pipelineLayout.dispose();

        frameBindGroupLayout.dispose();
        instancingBindGroupLayout.dispose();

        frameUniformBuffer.dispose();
        instanceBuffer.dispose();

        LibGPU.webGPU.wgpuSamplerRelease(sampler);
        // todo check everything is cleaned up
    }



    private Pointer makeShadowSampler(){
        // Create a sampler
        WGPUSamplerDescriptor samplerDesc = WGPUSamplerDescriptor.createDirect();
        samplerDesc.setAddressModeU(WGPUAddressMode.ClampToEdge);
        samplerDesc.setAddressModeV(WGPUAddressMode.ClampToEdge);
        samplerDesc.setAddressModeW(WGPUAddressMode.ClampToEdge);
        samplerDesc.setMagFilter(WGPUFilterMode.Linear);
        samplerDesc.setMinFilter(WGPUFilterMode.Linear);
        samplerDesc.setMipmapFilter(WGPUMipmapFilterMode.Linear);

        samplerDesc.setLodMinClamp(0);
        samplerDesc.setLodMaxClamp(1);
        samplerDesc.setCompare(WGPUCompareFunction.Less);
        samplerDesc.setMaxAnisotropy(1);
        return LibGPU.webGPU.wgpuDeviceCreateSampler(LibGPU.device.getHandle(), samplerDesc);
    }

    // Bind Group Layout:
    //  uniforms

    private BindGroupLayout createFrameBindGroupLayout(){
        BindGroupLayout layout = new BindGroupLayout("ModelBatch Bind Group Layout (Frame)");
        layout.begin();

        layout.addBuffer(0, WGPUShaderStage.Vertex | WGPUShaderStage.Fragment, WGPUBufferBindingType.Uniform, FRAME_UB_SIZE, true);
        layout.addTexture(1, WGPUShaderStage.Fragment , WGPUTextureSampleType.Depth, WGPUTextureViewDimension._2D, false);  // shadow map
        layout.addSampler(2, WGPUShaderStage.Fragment , WGPUSamplerBindingType.Comparison); // shadow sampler
        layout.addTexture(3, WGPUShaderStage.Fragment , WGPUTextureSampleType.Float, WGPUTextureViewDimension.Cube, false); // cube map
        layout.addSampler(4, WGPUShaderStage.Fragment , WGPUSamplerBindingType.Filtering);

        // IBL textures
        layout.addTexture(5, WGPUShaderStage.Fragment , WGPUTextureSampleType.Float, WGPUTextureViewDimension.Cube, false); // irradiance map
        layout.addSampler(6, WGPUShaderStage.Fragment , WGPUSamplerBindingType.Filtering);
        layout.addTexture(7, WGPUShaderStage.Fragment , WGPUTextureSampleType.Float, WGPUTextureViewDimension.Cube, false);
        layout.addTexture(8, WGPUShaderStage.Fragment , WGPUTextureSampleType.Float, WGPUTextureViewDimension._2D, false);
        layout.end();
        return layout;
    }

    // per frame bind group
    private BindGroup makeFrameBindGroup(BindGroupLayout frameBindGroupLayout, Pointer sampler, Buffer uniformBuffer) {

        Texture shadowMap = (environment != null && environment.renderShadows)? environment.shadowMap : dummyShadowMap;
        Texture cubeMap = (environment != null && environment.cubeMap != null) ? environment.cubeMap :  dummyCubemap;
        Texture irradMap = (environment != null && environment.irradianceMap != null) ? environment.irradianceMap :  dummyCubemap;
        Texture radMap = (environment != null && environment.radianceMap != null) ? environment.radianceMap :  dummyCubemap;
        Texture LUT = (environment != null && environment.brdfLUT != null) ? environment.brdfLUT :  dummy2DTexture;

        BindGroup bindGroup = new BindGroup(frameBindGroupLayout);
        bindGroup.begin();
        bindGroup.addBuffer(0, uniformBuffer, 0, FRAME_UB_SIZE);
        bindGroup.addTexture(1, shadowMap.getTextureView());
        bindGroup.addSampler(2, sampler);
        bindGroup.addTexture(3, cubeMap.getTextureView());
        bindGroup.addSampler(4, cubeMap.getSampler());

        bindGroup.addTexture(5, irradMap.getTextureView());
        bindGroup.addSampler(6, radMap.getSampler());
        bindGroup.addTexture(7, radMap.getTextureView());
        bindGroup.addTexture(8, LUT.getTextureView());
//        bindGroup.addSampler(8, radMap.getSampler());

        bindGroup.end();
        return bindGroup;
    }


    private void writeFrameUniforms( UniformBuffer uniformBuffer, Camera camera, Environment environment, int passNumber ){
        uniformBuffer.beginFill();
        uniformBuffer.append(camera.projection);
        uniformBuffer.append(camera.view);
        uniformBuffer.append(camera.combined);
        uniformBuffer.append(camera.position);
        uniformBuffer.append(environment == null ? 0f : environment.ambientLightLevel);
        uniformBuffer.pad(3*4);

        int numDirLights = 0;
        if(environment != null){
            DirectionalLight dirLight;
            for(Light light : environment.lights){
                if(light instanceof DirectionalLight && numDirLights < MAX_DIR_LIGHTS-1) {
                    numDirLights++;
                    dirLight = (DirectionalLight) light;
                    uniformBuffer.append(dirLight.color);
                    uniformBuffer.append(dirLight.direction);
                    uniformBuffer.append(dirLight.intensity);
                    uniformBuffer.pad(3*4); // padding
                }
            }
            // fixed length array, fill with placeholders up to MAX_DIR_LIGHTS
            for(int i = numDirLights; i < MAX_DIR_LIGHTS; i++) {
                dirLight = defaultDirectionalLight; // will be ignored anyway
                uniformBuffer.append(dirLight.color);
                uniformBuffer.append(dirLight.direction);
                uniformBuffer.append(dirLight.intensity);
                uniformBuffer.pad(3*4); // padding
            }
        }
        uniformBuffer.append(numDirLights);
        uniformBuffer.pad(3*4); // padding


        int numPointLights = 0;
        if(environment != null){
            PointLight pointLight;
            for(Light light : environment.lights){
                if(light instanceof PointLight && numPointLights < MAX_POINT_LIGHTS-1) {
                    numPointLights++;
                    pointLight = (PointLight) light;
                    uniformBuffer.append(pointLight.color);
                    uniformBuffer.append(pointLight.position);
                    uniformBuffer.append(pointLight.intensity);
                    // need padding?
                    uniformBuffer.pad(3*4); // padding
                }
            }
            // fixed length array, fill with placeholders up to MAX
            pointLight = new PointLight(Color.BLACK, new Vector3(), 0);// will be ignored anyway
            for(int i = numPointLights; i < MAX_POINT_LIGHTS; i++) {
                uniformBuffer.append(pointLight.color);
                uniformBuffer.append(pointLight.position);
                uniformBuffer.append(pointLight.intensity);
                uniformBuffer.pad(3*4); // padding
            }
        }
        uniformBuffer.append(numPointLights);
        //uniformBuffer.pad(3*4); // padding

        // roughnessLevels = mip count of radiance map when using IBL
        uniformBuffer.append((environment == null||environment.radianceMap == null) ? 0 : environment.radianceMap.getMipLevelCount());

        uniformBuffer.pad(2*4); // padding
        if(environment != null && environment.shadowCamera != null) {
            uniformBuffer.append(environment.shadowCamera.combined);
            uniformBuffer.append(environment.shadowCamera.position);
        }



        if(passNumber >= MAX_PASSES) throw new RuntimeException("ModelBatch: too many passes");
        uniformBuffer.endFill(passNumber*uniformBuffer.getUniformStride());   // write to GPU buffer
    }

    // add an instance to the instance buffer
    private void addInstance(int instanceIndex, Matrix4 modelTransform){
        if(instanceIndex >= MAX_INSTANCES)
            throw new RuntimeException("Too many instances: "+instanceIndex);

        instanceBuffer.beginFill();
        instanceBuffer.append(modelTransform);
        instanceBuffer.endFill(instanceIndex * 16*Float.BYTES);   // write to GPU buffer at offset for this instance
    }



    private BindGroupLayout createInstancingBindGroupLayout(){
        BindGroupLayout layout = new BindGroupLayout("ModelBatch Binding Group Layout (Instance)");
        layout.begin();
        layout.addBuffer(0, WGPUShaderStage.Vertex , WGPUBufferBindingType.ReadOnlyStorage, 16*Float.BYTES, false);
        layout.end();
        return layout;
    }

    private BindGroup createInstancingBindGroup(BindGroupLayout instanceBindGroupLayout, Buffer instanceBuffer) {
        BindGroup bindGroup = new BindGroup(instanceBindGroupLayout);
        bindGroup.begin();
        bindGroup.addBuffer(0, instanceBuffer);
        bindGroup.end();
        return bindGroup;
    }



    private Pointer floatData;

    private void updateJointBuffer(ModelInstance modelInstance ){
        if(modelInstance.jointBuffer == null)
            return;
        int matrixSize = 16*Float.BYTES;

        if(floatData == null)
            floatData = JavaWebGPU.createDirectPointer(matrixSize );    // allocate native memory for one matrix
        int offset = 0;
        for(Node joint : modelInstance.model.joints) {
            float floats[] = joint.globalTransform.val;
            floatData.put(0, floats, 0, 16);
            modelInstance.jointBuffer.write(offset, floatData, matrixSize);
            offset += matrixSize;
        }
    }

    private BindGroupLayout createSkinningBindGroupLayout(){
        // binding 0: joint matrices
        // binding 1: inverseBoneTransforms matrices
        BindGroupLayout layout = new BindGroupLayout("ModelBatch Binding Group Layout (Skinning)");
        layout.begin();
        layout.addBuffer(0, WGPUShaderStage.Vertex , WGPUBufferBindingType.ReadOnlyStorage, 0, false);
        layout.addBuffer(1, WGPUShaderStage.Vertex , WGPUBufferBindingType.ReadOnlyStorage, 0, false);
        layout.end();
        return layout;
    }

    private BindGroup createSkinningBindGroup(BindGroupLayout layout, Buffer jointBuffer, Buffer inverseBoneBuffer) {
        BindGroup bindGroup = new BindGroup(layout);
        bindGroup.begin();
        // webgpu will raise a fatal error if buffer is size is zero, despite we are not using these buffers for non-skinned models
        // and minBindingSize is 0. So use a dummy buffer if needed.
        bindGroup.addBuffer(0, jointBuffer);
        bindGroup.addBuffer(1, inverseBoneBuffer);
        bindGroup.end();
        return bindGroup;
    }
}
