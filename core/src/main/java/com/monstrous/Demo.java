package com.monstrous;

import com.monstrous.graphics.SpriteBatch;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.TextureRegion;
import com.monstrous.math.Matrix4;
import com.monstrous.utils.WgpuJava;
import com.monstrous.wgpu.*;
import jnr.ffi.Pointer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;


public class Demo implements ApplicationListener {
    private String shaderSource = readShaderSource();

    private WGPU wgpu;
    private Pointer surface;
    private Pointer device;
    private Pointer queue;
    private Pointer pipeline;
    private WGPUTextureFormat surfaceFormat = WGPUTextureFormat.Undefined;
    private Pointer vertexBuffer;
    private Pointer indexBuffer;
    private int indexCount;
    private Pointer uniformBuffer;
    private Pointer layout;
    private Pointer bindGroupLayout;
    private Pointer bindGroup;
    private Pointer bindGroup2;
    private int uniformBufferSize;  // in bytes
    private int uniformStride;
    private int uniformInstances;
    private Pointer uniformData;
    private Pointer depthTextureView;
    private Pointer depthTexture;
    private Matrix4 projectionMatrix;
    private Matrix4 viewMatrix;
    private Matrix4 modelMatrix;
    private Texture texture;
    private Texture texture2;
    private Texture textureFont;
    private float currentTime;
    private SpriteBatch batch;
    private long startTime;
    private int frames;

    public void init() {

        startTime = System.nanoTime();
        frames = 0;

        wgpu = LibGPU.wgpu;
        surface = LibGPU.surface;

        // debug test
        System.out.println("Hello world!");
        int sum = wgpu.add(1200, 34);
        System.out.println("sum = " + sum);

        System.out.println("define adapter options");
        WGPURequestAdapterOptions options = WGPURequestAdapterOptions.createDirect();
        options.setNextInChain();
        options.setCompatibleSurface(LibGPU.surface);
        options.setBackendType(WGPUBackendType.Vulkan);

        System.out.println("defined adapter options");

        // Get Adapter
        Pointer adapter = wgpu.RequestAdapterSync(LibGPU.instance, options);
        System.out.println("adapter = " + adapter);

        WGPUSupportedLimits supportedLimits = WGPUSupportedLimits.createDirect();
        wgpu.AdapterGetLimits(adapter, supportedLimits);
        System.out.println("adapter maxVertexAttributes " + supportedLimits.getLimits().getMaxVertexAttributes());

        System.out.println("maxTextureDimension1D " + supportedLimits.getLimits().getMaxTextureDimension1D());
        System.out.println("maxTextureDimension2D " + supportedLimits.getLimits().getMaxTextureDimension2D());
        System.out.println("maxTextureDimension3D " + supportedLimits.getLimits().getMaxTextureDimension3D());
        System.out.println("maxTextureArrayLayers " + supportedLimits.getLimits().getMaxTextureArrayLayers());


        WGPUAdapterProperties adapterProperties = WGPUAdapterProperties.createDirect();
        adapterProperties.setNextInChain();

        wgpu.AdapterGetProperties(adapter, adapterProperties);

        System.out.println("VendorID: " + adapterProperties.getVendorID());
        System.out.println("Vendor name: " + adapterProperties.getVendorName());
        System.out.println("Device ID: " + adapterProperties.getDeviceID());
        System.out.println("Back end: " + adapterProperties.getBackendType());
        System.out.println("Description: " + adapterProperties.getDriverDescription());

        WGPURequiredLimits requiredLimits = WGPURequiredLimits.createDirect();
        setDefault(requiredLimits.getLimits());
        requiredLimits.getLimits().setMaxVertexAttributes(2);
        requiredLimits.getLimits().setMaxVertexBuffers(2);
        requiredLimits.getLimits().setMaxInterStageShaderComponents(8); //

        // from vert to frag
        requiredLimits.getLimits().setMaxBufferSize(300);
        requiredLimits.getLimits().setMaxVertexBufferArrayStride(11*Float.BYTES);
        requiredLimits.getLimits().setMaxDynamicUniformBuffersPerPipelineLayout(1);
        requiredLimits.getLimits().setMaxTextureDimension1D(2048);
        requiredLimits.getLimits().setMaxTextureDimension2D(2048);
        requiredLimits.getLimits().setMaxTextureArrayLayers(1);
        requiredLimits.getLimits().setMaxSampledTexturesPerShaderStage(1);
        requiredLimits.getLimits().setMaxSamplersPerShaderStage(1);

        requiredLimits.getLimits().setMaxBindGroups(1);        // We use at most 1 bind group for now
        requiredLimits.getLimits().setMaxUniformBuffersPerShaderStage(1);// We use at most 1 uniform buffer per stage
        // Uniform structs have a size of maximum 16 float (more than what we need)
        requiredLimits.getLimits().setMaxUniformBufferBindingSize(16*4*Float.BYTES);



        // Get Device
        WGPUDeviceDescriptor deviceDescriptor = WGPUDeviceDescriptor.createDirect();
        deviceDescriptor.setNextInChain();
        deviceDescriptor.setLabel("My Device");
        deviceDescriptor.setRequiredLimits(requiredLimits);

        device = wgpu.RequestDeviceSync(adapter, deviceDescriptor);
        LibGPU.device = device;
        wgpu.AdapterRelease(adapter);       // we can release our adapter as soon as we have a device

        // use a lambda expression to define a callback function
        WGPUErrorCallback deviceCallback = (WGPUErrorType type, String message, Pointer userdata) -> {
            System.out.println("*** Device error: " + type + " : " + message);
        };
        wgpu.DeviceSetUncapturedErrorCallback(device, deviceCallback, null);

        wgpu.DeviceGetLimits(device, supportedLimits);
        System.out.println("device maxVertexAttributes " + supportedLimits.getLimits().getMaxVertexAttributes());

        System.out.println("maxTextureDimension1D " + supportedLimits.getLimits().getMaxTextureDimension1D());
        System.out.println("maxTextureDimension2D " + supportedLimits.getLimits().getMaxTextureDimension2D());
        System.out.println("maxTextureDimension3D " + supportedLimits.getLimits().getMaxTextureDimension3D());
        System.out.println("maxTextureArrayLayers " + supportedLimits.getLimits().getMaxTextureArrayLayers());

        queue = wgpu.DeviceGetQueue(device);
        LibGPU.queue = queue;





        // use a lambda expression to define a callback function
        WGPUQueueWorkDoneCallback queueCallback = (WGPUQueueWorkDoneStatus status, Pointer userdata) -> {
            System.out.println("=== Queue work finished with status: " + status);
        };
        wgpu.QueueOnSubmittedWorkDone(queue, queueCallback, null);


        // configure the surface
        WGPUSurfaceConfiguration config = WGPUSurfaceConfiguration.createDirect();
        config.setNextInChain();

        config.setWidth(LibGPU.graphics.getWidth());
        config.setHeight(LibGPU.graphics.getHeight());

        surfaceFormat = wgpu.SurfaceGetPreferredFormat(surface, adapter);
        System.out.println("Using format: " + surfaceFormat);
        config.setFormat(surfaceFormat);
        // And we do not need any particular view format:
        config.setViewFormatCount(0);
        config.setViewFormats(WgpuJava.createNullPointer());
        config.setUsage(WGPUTextureUsage.RenderAttachment);
        config.setDevice(device);
        config.setPresentMode(LibGPU.application.configuration.vsyncEnabled ? WGPUPresentMode.Fifo : WGPUPresentMode.Immediate);
        config.setAlphaMode(WGPUCompositeAlphaMode.Auto);

        wgpu.SurfaceConfigure(surface, config);

        initializePipeline();
        //playingWithBuffers();

        texture = new Texture("monstrous.png", false);
        texture2 = new Texture("jackRussel.png", false);
        textureFont = new Texture("lsans-15.png", false);

        projectionMatrix = new Matrix4();
        modelMatrix = new Matrix4();
//        modelMatrix.scale(0.5f, 0.5f, 0.5f);
//        modelMatrix.translate(1,0,0);
//        modelMatrix.setToYRotation(0.59f);
        System.out.println(modelMatrix.toString());
        viewMatrix = new Matrix4();

        // P matrix: 16 float
        // M matrix: 16 float
        // V matrix: 16 float
        // time: 1 float
        // 3 floats padding
        // color: 4 floats
        uniformBufferSize = (3*16+8) * Float.BYTES;
        float[] uniforms = new float[uniformBufferSize];
        uniformData = WgpuJava.createFloatArrayPointer(uniforms);

        int minAlign = (int)supportedLimits.getLimits().getMinUniformBufferOffsetAlignment();
        uniformStride = ceilToNextMultiple(uniformBufferSize, minAlign);
        uniformInstances = 1;   // how many sets of uniforms?

        System.out.println("min uniform alignment: "+minAlign);
        System.out.println("uniform stride: "+uniformStride);
        System.out.println("uniformBufferSize: "+uniformBufferSize);
        initBuffers();


        bindGroup = initBindGroups(texture);
        bindGroup2 = initBindGroups(texture2);

        batch = new SpriteBatch();

    }

    private int ceilToNextMultiple(int value, int step){
        int d = value / step + (value % step == 0 ? 0 : 1);
        return step * d;
    }

    private void playingWithBuffers() {

        WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
        bufferDesc.setLabel("Some GPU-side data buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.CopySrc );
        bufferDesc.setSize(16);
        bufferDesc.setMappedAtCreation(0L);
        Pointer buffer1 = wgpu.DeviceCreateBuffer(device, bufferDesc);

        bufferDesc.setLabel("Output buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.MapRead );
        bufferDesc.setSize(16);
        bufferDesc.setMappedAtCreation(0L);

        Pointer buffer2 = wgpu.DeviceCreateBuffer(device, bufferDesc);

        // Create some CPU-side data buffer (of size 16 bytes)
        byte[] numbers = new byte[16];
        for(int i = 0; i < 16; i++)
            numbers[i] = (byte)i;
        Pointer data = WgpuJava.createByteArrayPointer(numbers);
        // `numbers` now contains [ 0, 1, 2, ... ]

        // Copy this from `numbers` (RAM) to `buffer1` (VRAM)
        wgpu.QueueWriteBuffer(queue, buffer1, 0, data, numbers.length);

        Pointer encoder = wgpu.DeviceCreateCommandEncoder(device, null);

        // After creating the command encoder

        // [...] Copy buffer to buffer
        // size must be multiple of 4
        wgpu.CommandEncoderCopyBufferToBuffer(encoder, buffer1, 0, buffer2, 0, 16);


        Pointer command = wgpu.CommandEncoderFinish(encoder, null);
        wgpu.CommandEncoderRelease(encoder);

        // BEWARE: we need this convoluted call sequence or it will crash
        long[] buffers = new long[1];
        buffers[0] = command.address();
        Pointer bufferPtr = WgpuJava.createLongArrayPointer(buffers);
        wgpu.QueueSubmit(queue, 1, bufferPtr);


        wgpu.CommandBufferRelease(command);

        // use a lambda expression to define a callback function
        WGPUBufferMapCallback onBuffer2Mapped = (WGPUBufferMapAsyncStatus status, Pointer userData) -> {
            System.out.println("=== Buffer 2 mapped with status: " + status);
            userData.putInt(0, 1);
        };

        int[] ready = new int[1];
        ready[0] = 0;

        Pointer udata = WgpuJava.createIntegerArrayPointer(ready);
        System.out.println(udata);
        wgpu.BufferMapAsync(buffer2, WGPUMapMode.Read, 0, 16, onBuffer2Mapped, udata);


        int iters = 0;
        // note you cannot test ready[0] because createIntegerArrayPointer made a copy
        while(udata.getInt(0) == 0){
            iters++;
            wgpu.DeviceTick(device);
        }
        System.out.println(" Iterations: "+iters);

        System.out.println(" received: " + String.valueOf(udata.getInt(0)));

        // Get a pointer to wherever the driver mapped the GPU memory to the RAM
        Pointer ram =  wgpu.BufferGetConstMappedRange(buffer2, 0, 16);
        for(int i = 0; i < 16; i++){
            byte num = ram.getByte(i);
            System.out.print(num);
            System.out.print(' ');
        }
        System.out.println();

// Then do not forget to unmap the memory
        wgpu.BufferUnmap(buffer2);

        wgpu.BufferRelease(buffer1);
        wgpu.BufferRelease(buffer2);

    }

    private String readShaderSource() {
        String src = null;
        try {
            src = Files.readString(Paths.get("shader.wgsl"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return src;
    }

    private void initBuffers() {
        int dimensions = 3;
        FileInput input = new FileInput("plane.txt");
        int vertSize = 8+dimensions; // in floats
        ArrayList<Integer> indexValues = new ArrayList<>();
        ArrayList<Float> vertFloats = new ArrayList<>();
        int mode = 0;
        for(int lineNr = 0; lineNr < input.size(); lineNr++) {
            String line = input.get(lineNr).strip();
            if (line.contentEquals("[points]")) {
                mode = 1;
                continue;
            }
            if (line.contentEquals("[indices]")) {
                mode = 2;
                continue;
            }
            if(line.startsWith("#"))
                continue;
            if(line.length() == 0)
                continue;
            if(mode == 1){
                String [] words = line.split("[ \t]+");
                if(words.length != vertSize)
                    System.out.println("Expected "+vertSize+" floats per vertex : "+line);
                for(int i = 0; i < vertSize; i++)
                    vertFloats.add(Float.parseFloat(words[i]));
            } else if (mode == 2){
                String [] words = line.split("[ \t]+");
                if(words.length != 3)
                    System.out.println("Expected 3 indices per line: "+line);
                for(int i = 0; i < 3; i++)
                    indexValues.add(Integer.parseInt(words[i]));
            } else {
                System.out.println("Unexpected input: "+line);
            }
        }

        int vertexCount = vertFloats.size()/vertSize;
        float[] vertexData = new float[ vertFloats.size() ];
        for(int i = 0; i < vertFloats.size(); i++){
            vertexData[i] = vertFloats.get(i);
        }

        indexCount = indexValues.size();
        int [] indexData = new int[ indexCount ];
        for(int i = 0; i < indexCount; i++){
            indexData[i] = indexValues.get(i);
        }

        // Create vertex buffer
        WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
        bufferDesc.setLabel("Vertex buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.Vertex );
        bufferDesc.setSize(vertexData.length*Float.BYTES);
        bufferDesc.setMappedAtCreation(0L);
        vertexBuffer = wgpu.DeviceCreateBuffer(device, bufferDesc);

        Pointer data = WgpuJava.createFloatArrayPointer(vertexData);

        // Upload geometry data to the buffer
        wgpu.QueueWriteBuffer(queue, vertexBuffer, 0, data, (int)bufferDesc.getSize());

        // Create index buffer
        bufferDesc.setLabel("Index buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.Index );
        bufferDesc.setSize(indexData.length*Integer.BYTES);
        // in case we use a sort index:
        //bufferDesc.size = (bufferDesc.size + 3) & ~3; // round up to the next multiple of 4
        bufferDesc.setMappedAtCreation(0L);
        indexBuffer = wgpu.DeviceCreateBuffer(device, bufferDesc);

        Pointer idata = WgpuJava.createIntegerArrayPointer(indexData);


        // Upload data to the buffer
        wgpu.QueueWriteBuffer(queue, indexBuffer, 0, idata, (int)bufferDesc.getSize());

        // Create uniform buffer
        //WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
        bufferDesc.setLabel("Uniform buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.Uniform );
        bufferDesc.setSize(uniformStride * uniformInstances);
        bufferDesc.setMappedAtCreation(0L);
        uniformBuffer = wgpu.DeviceCreateBuffer(device, bufferDesc);

    }

    private Pointer initBindGroups(Texture texture) {
        // Create a binding
        WGPUBindGroupEntry binding = WGPUBindGroupEntry.createDirect();
        binding.setNextInChain();
        binding.setBinding(0);  // binding index
        binding.setBuffer(uniformBuffer);
        binding.setOffset(0);
        binding.setSize(uniformBufferSize);

        // A bind group contains one or multiple bindings
        WGPUBindGroupDescriptor bindGroupDesc = WGPUBindGroupDescriptor.createDirect();
        bindGroupDesc.setNextInChain();
        bindGroupDesc.setLayout(bindGroupLayout);
        // There must be as many bindings as declared in the layout!
        bindGroupDesc.setEntryCount(3);
        bindGroupDesc.setEntries(binding, texture.getBinding(1), texture.getSamplerBinding(2));
        return wgpu.DeviceCreateBindGroup(device, bindGroupDesc);
    }

    private void initializePipeline() {

        // Create Shader Module
        WGPUShaderModuleDescriptor shaderDesc = WGPUShaderModuleDescriptor.createDirect();
        shaderDesc.setLabel("My Shader");



        WGPUShaderModuleWGSLDescriptor shaderCodeDesc = WGPUShaderModuleWGSLDescriptor.createDirect();
        shaderCodeDesc.getChain().setNext();
        shaderCodeDesc.getChain().setSType(WGPUSType.ShaderModuleWGSLDescriptor);
        shaderCodeDesc.setCode(shaderSource);

        //System.out.println("shaderSource: "+shaderSource);

        shaderDesc.getNextInChain().set(shaderCodeDesc.getPointerTo());

        Pointer shaderModule = wgpu.DeviceCreateShaderModule(device, shaderDesc);


        //  create an array of WGPUVertexAttribute
        int attribCount = 4;

        WGPUVertexAttribute positionAttrib =  WGPUVertexAttribute.createDirect();

        positionAttrib.setFormat(WGPUVertexFormat.Float32x3);
        positionAttrib.setOffset(0);
        positionAttrib.setShaderLocation(0);

        WGPUVertexAttribute normalAttrib =  WGPUVertexAttribute.createDirect();

        normalAttrib.setFormat(WGPUVertexFormat.Float32x3);
        normalAttrib.setOffset(3*Float.BYTES);
        normalAttrib.setShaderLocation(1);


        WGPUVertexAttribute colorAttrib = WGPUVertexAttribute.createDirect();   // freed where?

        colorAttrib.setFormat(WGPUVertexFormat.Float32x3);
        colorAttrib.setOffset(6*Float.BYTES);
        colorAttrib.setShaderLocation(2);

        WGPUVertexAttribute uvAttrib = WGPUVertexAttribute.createDirect();   // freed where?

        uvAttrib.setFormat(WGPUVertexFormat.Float32x2);
        uvAttrib.setOffset(9*Float.BYTES);
        uvAttrib.setShaderLocation(3);


        WGPUVertexBufferLayout vertexBufferLayout = WGPUVertexBufferLayout.createDirect();
        vertexBufferLayout.setAttributeCount(attribCount);

        vertexBufferLayout.setAttributes(positionAttrib, normalAttrib, colorAttrib, uvAttrib);
        vertexBufferLayout.setArrayStride(11*Float.BYTES);
        vertexBufferLayout.setStepMode(WGPUVertexStepMode.Vertex);


        WGPURenderPipelineDescriptor pipelineDesc = WGPURenderPipelineDescriptor.createDirect();
        pipelineDesc.setNextInChain();
        pipelineDesc.setLabel("pipeline");

        pipelineDesc.getVertex().setBufferCount(1);
        pipelineDesc.getVertex().setBuffers(vertexBufferLayout);

        pipelineDesc.getVertex().setModule(shaderModule);
        pipelineDesc.getVertex().setEntryPoint("vs_main");
        pipelineDesc.getVertex().setConstantCount(0);
        pipelineDesc.getVertex().setConstants();

        pipelineDesc.getPrimitive().setTopology(WGPUPrimitiveTopology.TriangleList);
        pipelineDesc.getPrimitive().setStripIndexFormat(WGPUIndexFormat.Undefined);
        pipelineDesc.getPrimitive().setFrontFace(WGPUFrontFace.CCW);
        pipelineDesc.getPrimitive().setCullMode(WGPUCullMode.None);

        WGPUFragmentState fragmentState = WGPUFragmentState.createDirect();
        fragmentState.setNextInChain();
        fragmentState.setModule(shaderModule);
        fragmentState.setEntryPoint("fs_main");
        fragmentState.setConstantCount(0);
        fragmentState.setConstants();

        // blend
        WGPUBlendState blendState = WGPUBlendState.createDirect();
        blendState.getColor().setSrcFactor(WGPUBlendFactor.SrcAlpha);
        blendState.getColor().setDstFactor(WGPUBlendFactor.OneMinusSrcAlpha);
        blendState.getColor().setOperation(WGPUBlendOperation.Add);
        blendState.getAlpha().setSrcFactor(WGPUBlendFactor.Zero);
        blendState.getAlpha().setDstFactor(WGPUBlendFactor.One);
        blendState.getAlpha().setOperation(WGPUBlendOperation.Add);

        WGPUColorTargetState colorTarget = WGPUColorTargetState.createDirect();
        colorTarget.setFormat(surfaceFormat);
        colorTarget.setBlend(blendState);
        colorTarget.setWriteMask(WGPUColorWriteMask.All);

        fragmentState.setTargetCount(1);
        fragmentState.setTargets(colorTarget);

        pipelineDesc.setFragment(fragmentState);

        WGPUDepthStencilState depthStencilState = WGPUDepthStencilState.createDirect();
        setDefault(depthStencilState);
        depthStencilState.setDepthCompare(WGPUCompareFunction.Less);
        depthStencilState.setDepthWriteEnabled(1L);
        WGPUTextureFormat depthTextureFormat = WGPUTextureFormat.Depth24Plus;
        depthStencilState.setFormat(depthTextureFormat);
        // deactivate stencil
        depthStencilState.setStencilReadMask(0L);
        depthStencilState.setStencilWriteMask(0L);

        pipelineDesc.setDepthStencil(depthStencilState);


        long[] formats = new long[1];
        formats[0] = depthTextureFormat.ordinal();
        Pointer formatPtr = WgpuJava.createLongArrayPointer(formats);

        // Create the depth texture
        WGPUTextureDescriptor depthTextureDesc = WGPUTextureDescriptor.createDirect();
        depthTextureDesc.setNextInChain();
        depthTextureDesc.setDimension( WGPUTextureDimension._2D);
        depthTextureDesc.setFormat( depthTextureFormat );
        depthTextureDesc.setMipLevelCount(1);
        depthTextureDesc.setSampleCount(1);
        depthTextureDesc.getSize().setWidth(LibGPU.graphics.getWidth());
        depthTextureDesc.getSize().setHeight(LibGPU.graphics.getHeight());
        depthTextureDesc.getSize().setDepthOrArrayLayers(1);
        depthTextureDesc.setUsage( WGPUTextureUsage.RenderAttachment );
        depthTextureDesc.setViewFormatCount(1);
        depthTextureDesc.setViewFormats( formatPtr );
        depthTexture = wgpu.DeviceCreateTexture(device, depthTextureDesc);


        // Create the view of the depth texture manipulated by the rasterizer
        WGPUTextureViewDescriptor depthTextureViewDesc = WGPUTextureViewDescriptor.createDirect();
        depthTextureViewDesc.setAspect(WGPUTextureAspect.DepthOnly);
        depthTextureViewDesc.setBaseArrayLayer(0);
        depthTextureViewDesc.setArrayLayerCount(1);
        depthTextureViewDesc.setBaseMipLevel(0);
        depthTextureViewDesc.setMipLevelCount(1);
        depthTextureViewDesc.setDimension( WGPUTextureViewDimension._2D);
        depthTextureViewDesc.setFormat(depthTextureFormat);
        depthTextureView = wgpu.TextureCreateView(depthTexture, depthTextureViewDesc);

        pipelineDesc.getMultisample().setCount(1);
        pipelineDesc.getMultisample().setMask( 0xFFFFFFFF);
        pipelineDesc.getMultisample().setAlphaToCoverageEnabled(0);

        // Define binding layout
        WGPUBindGroupLayoutEntry bindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(bindingLayout);
        bindingLayout.setBinding(0);
        bindingLayout.setVisibility(WGPUShaderStage.Vertex | WGPUShaderStage.Fragment);
        bindingLayout.getBuffer().setType(WGPUBufferBindingType.Uniform);
        bindingLayout.getBuffer().setMinBindingSize(uniformBufferSize);

        WGPUBindGroupLayoutEntry texBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(texBindingLayout);
        texBindingLayout.setBinding(1);
        texBindingLayout.setVisibility(WGPUShaderStage.Fragment);
        texBindingLayout.getTexture().setSampleType(WGPUTextureSampleType.Float);
        texBindingLayout.getTexture().setViewDimension(WGPUTextureViewDimension._2D);

        WGPUBindGroupLayoutEntry samplerBindingLayout = WGPUBindGroupLayoutEntry.createDirect();
        setDefault(samplerBindingLayout);
        samplerBindingLayout.setBinding(2);
        samplerBindingLayout.setVisibility(WGPUShaderStage.Fragment);
        samplerBindingLayout.getSampler().setType(WGPUSamplerBindingType.Filtering);

        // Create a bind group layout
        WGPUBindGroupLayoutDescriptor bindGroupLayoutDesc = WGPUBindGroupLayoutDescriptor.createDirect();
        bindGroupLayoutDesc.setNextInChain();
        bindGroupLayoutDesc.setLabel("My BG Layout");
        bindGroupLayoutDesc.setEntryCount(3);
        bindGroupLayoutDesc.setEntries(bindingLayout, texBindingLayout, samplerBindingLayout);
        bindGroupLayout = wgpu.DeviceCreateBindGroupLayout(device, bindGroupLayoutDesc);


        long[] layouts = new long[1];
        layouts[0] = bindGroupLayout.address();
        Pointer layoutPtr = WgpuJava.createLongArrayPointer(layouts);

        // Create the pipeline layout
        WGPUPipelineLayoutDescriptor layoutDesc = WGPUPipelineLayoutDescriptor.createDirect();
        layoutDesc.setNextInChain();
        layoutDesc.setLabel("Pipeline Layout");
        layoutDesc.setBindGroupLayoutCount(1);
        layoutDesc.setBindGroupLayouts(layoutPtr);
        layout = wgpu.DeviceCreatePipelineLayout(device, layoutDesc);

        pipelineDesc.setLayout(layout);
        pipeline = wgpu.DeviceCreateRenderPipeline(device, pipelineDesc);
        wgpu.ShaderModuleRelease(shaderModule);


    }

    private void setUniformColor(Pointer data, int offset, float r, float g, float b, float a ){
        data.putFloat(offset+0*Float.BYTES, r);
        data.putFloat(offset+1*Float.BYTES, g);
        data.putFloat(offset+2*Float.BYTES, b);
        data.putFloat(offset+3*Float.BYTES, a);
    }
    private void setUniformMatrix(Pointer data, int offset, Matrix4 mat ){
        for(int i = 0; i < 16; i++){
            data.putFloat(offset+i*Float.BYTES, mat.val[i]);
        }
    }

    private void updateMatrices(float currentTime){
        projectionMatrix.setToOrtho(-1.1f, 1.1f, -1.1f, 1.1f, -1, 1);

        modelMatrix.setToXRotation((float) ( -0.5f*Math.PI ));  // tilt to face camera
        viewMatrix.idt();

    }

    private void updateMatrices2(float currentTime){

        float aspectRatio = (float)LibGPU.graphics.getWidth()/(float)LibGPU.graphics.getHeight();
        projectionMatrix.setToPerspective(1.5f, 0.01f, 9.0f, aspectRatio);
        //projectionMatrix.setToProjection(0.001f, 3.0f, 60f, 640f/480f);
        //modelMatrix.setToYRotation(currentTime*0.2f).scale(0.5f);
        modelMatrix.idt();//.setToXRotation((float) ( -0.5f*Math.PI ));

        Matrix4 RT = new Matrix4().setToXRotation((float) ( -0.5f*Math.PI ));

        //modelMatrix.idt().scale(0.5f);
        viewMatrix.idt();
        Matrix4 R1 = new Matrix4().setToYRotation(currentTime*0.6f);
        Matrix4 R2 = new Matrix4().setToXRotation((float) (-0.5* Math.PI / 4.0)); // tilt the view
        Matrix4 S = new Matrix4().scale(1.6f);
        Matrix4 T = new Matrix4().translate(0.8f, 0f, 0f);
        Matrix4 TC = new Matrix4().translate(0.0f, -1f, 3f);


        modelMatrix.idt().mul(R1).mul(T).mul(RT);

        TC.mul(S);
        R2.mul(TC); // tilt
        viewMatrix.set(R2);
        //viewMatrix.translate(0,0.2f, 0);
        //viewMatrix.setToZRotation((float) (Math.PI*0.5f));
        //viewMatrix.translate(0, 0, (float)Math.cos(currentTime)*0.5f );
    }

    private void setUniforms(){


        updateMatrices(currentTime);

        int offset = 0;
        setUniformMatrix(uniformData, offset, projectionMatrix);
        offset += 16*Float.BYTES;
        setUniformMatrix(uniformData, offset, viewMatrix);
        offset += 16*Float.BYTES;
        setUniformMatrix(uniformData, offset, modelMatrix);
        offset += 16*Float.BYTES;
        uniformData.putFloat(offset, currentTime);
        offset += 4*Float.BYTES;
        // 3 floats of padding
        setUniformColor(uniformData, offset, 0.0f, 1.0f, 0.4f, 1.0f);
        wgpu.QueueWriteBuffer(queue, uniformBuffer, 0, uniformData, uniformBufferSize);
    }

    public void render( float deltaTime ){
        currentTime += deltaTime;

        Pointer targetView = getNextSurfaceTextureView();
        if (targetView.address() == 0) {
            System.out.println("*** Invalid target view");
            return;
        }





        setUniforms();

        WGPUCommandEncoderDescriptor encoderDescriptor = WGPUCommandEncoderDescriptor.createDirect();
        encoderDescriptor.setNextInChain();
        encoderDescriptor.setLabel("My Encoder");

        Pointer encoder = wgpu.DeviceCreateCommandEncoder(device, encoderDescriptor);

        WGPURenderPassColorAttachment renderPassColorAttachment = WGPURenderPassColorAttachment.createDirect();
        renderPassColorAttachment.setNextInChain();
        renderPassColorAttachment.setView(targetView);
        renderPassColorAttachment.setResolveTarget(WgpuJava.createNullPointer());
        renderPassColorAttachment.setLoadOp(WGPULoadOp.Clear);
        renderPassColorAttachment.setStoreOp(WGPUStoreOp.Store);

        renderPassColorAttachment.getClearValue().setR(0.25);
        renderPassColorAttachment.getClearValue().setG(0.25);
        renderPassColorAttachment.getClearValue().setB(0.25);
        renderPassColorAttachment.getClearValue().setA(1.0);

        renderPassColorAttachment.setDepthSlice(wgpu.WGPU_DEPTH_SLICE_UNDEFINED);


        WGPURenderPassDepthStencilAttachment depthStencilAttachment = WGPURenderPassDepthStencilAttachment.createDirect();
        depthStencilAttachment.setView( depthTextureView );
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

        renderPassDescriptor.setLabel("Main Render Pass");

        renderPassDescriptor.setColorAttachmentCount(1);
        renderPassDescriptor.setColorAttachments( renderPassColorAttachment );
        renderPassDescriptor.setOcclusionQuerySet(WgpuJava.createNullPointer());
        renderPassDescriptor.setDepthStencilAttachment(); // depthStencilAttachment );
        renderPassDescriptor.setTimestampWrites();


        Pointer renderPass = wgpu.CommandEncoderBeginRenderPass(encoder, renderPassDescriptor);
// [...] Use Render Pass


        System.nanoTime();


        // SpriteBatch testing
        batch.begin(renderPass);    // todo param for now
//char id=65 x=80 y=33 width=11 height=13 xoffset=-1 yoffset=2 xadvance=9 page=0 chnl=0

//        TextureRegion letterA = new TextureRegion(textureFont, 80f/256f, (33f+13f)/128f, (80+11f)/256f, 33f/128f);
//        batch.draw(letterA, 100, 100);

        batch.setColor(1,0,0,0.1f);
        batch.draw(texture, 0, 0, 100, 100);

        batch.draw(texture, 0, 0, 300, 300, 0.5f, 0.5f, 0.9f, 0.1f);
        batch.draw(texture, 300, 300, 50, 50);
        batch.setColor(1,1,1,1);

        batch.draw(texture2, 400, 100, 100, 100);

        TextureRegion region = new TextureRegion(texture2, 0, 0, 512, 512);
        batch.draw(region, 200, 300, 64, 64);

        TextureRegion region2 = new TextureRegion(texture2, 0f, 1f, .5f, 0.5f);
        batch.draw(region2, 400, 300, 64, 64);

//        batch.setColor(0,1,0,1);
//        for(int i = 0; i < 800; i++){
//            batch.draw(texture2, (int) (Math.random()*640), (int) (Math.random()*480), 32, 32);
//        }
        batch.end();


//        wgpu.RenderPassEncoderSetPipeline(renderPass, pipeline);
//
//        // Set vertex buffer while encoding the render pass
//        wgpu.RenderPassEncoderSetVertexBuffer(renderPass, 0, vertexBuffer, 0, wgpu.BufferGetSize(vertexBuffer));
//        wgpu.RenderPassEncoderSetIndexBuffer(renderPass, indexBuffer, WGPUIndexFormat.Uint32, 0, wgpu.BufferGetSize(indexBuffer));
//
//        Pointer bg = initBindGroups(texture);
//        wgpu.RenderPassEncoderSetBindGroup(renderPass, 0, bg, 0, null);
//        wgpu.RenderPassEncoderDrawIndexed(renderPass, 3, 1, 0, 0, 0);
//        wgpu.BindGroupRelease(bg);
//
//        wgpu.RenderPassEncoderSetVertexBuffer(renderPass, 0, vertexBuffer, 0, wgpu.BufferGetSize(vertexBuffer));
//        wgpu.RenderPassEncoderSetIndexBuffer(renderPass, indexBuffer, WGPUIndexFormat.Uint32, 0, wgpu.BufferGetSize(indexBuffer));
//
//        bg = initBindGroups(texture2);
//        wgpu.RenderPassEncoderSetBindGroup(renderPass, 0, bg, 0, null);
//        wgpu.RenderPassEncoderDrawIndexed(renderPass, indexCount, 1, 0, 0, 0);
//        wgpu.BindGroupRelease(bg);
//
//        wgpu.RenderPassEncoderEnd(renderPass);
//
//        wgpu.RenderPassEncoderRelease(renderPass);



        WGPUCommandBufferDescriptor bufferDescriptor =  WGPUCommandBufferDescriptor.createDirect();
        bufferDescriptor.setNextInChain();
        bufferDescriptor.setLabel("Command Buffer");
        Pointer commandBuffer = wgpu.CommandEncoderFinish(encoder, bufferDescriptor);
        wgpu.CommandEncoderRelease(encoder);


        long[] buffers = new long[1];
        buffers[0] = commandBuffer.address();
        Pointer bufferPtr = WgpuJava.createLongArrayPointer(buffers);
        //System.out.println("Pointer: "+bufferPtr.toString());
        //System.out.println("Submitting command...");
        wgpu.QueueSubmit(queue, 1, bufferPtr);

        wgpu.CommandBufferRelease(commandBuffer);
        //System.out.println("Command submitted...");


        // At the end of the frame
        wgpu.TextureViewRelease(targetView);
        wgpu.SurfacePresent(surface);


        if (System.nanoTime() - startTime > 1000000000) {
            System.out.println("SpriteBatch : fps: " + frames  );
            frames = 0;
            startTime = System.nanoTime();
        }
        frames++;

        for(int i = 0; i < 10; i++)
            wgpu.DeviceTick(device);
    }

    public void exit(){
        // cleanup
        System.out.println("demo exit");
        texture.dispose();
        texture2.dispose();
        batch.dispose();
        System.out.println("demo exit2");

        // Destroy the depth texture and its view
        wgpu.TextureViewRelease(depthTextureView);
        wgpu.TextureDestroy(depthTexture);
        wgpu.TextureRelease(depthTexture);
        System.out.println("demo exit3");

        wgpu.PipelineLayoutRelease(layout);
        wgpu.BindGroupLayoutRelease(bindGroupLayout);
        wgpu.BindGroupRelease(bindGroup);
        wgpu.BufferRelease(indexBuffer);
        wgpu.BufferRelease(vertexBuffer);
        wgpu.BufferRelease(uniformBuffer);
        wgpu.RenderPipelineRelease(pipeline);
        System.out.println("demo exit4");
//        wgpu.SurfaceUnconfigure(surface);
//        wgpu.SurfaceRelease(surface);
        wgpu.QueueRelease(queue);
        wgpu.DeviceRelease(device);
        //wgpu.InstanceRelease(instance);
        System.out.println("demo exit5");
    }

    private Pointer getNextSurfaceTextureView() {
        // [...] Get the next surface texture


        WGPUSurfaceTexture surfaceTexture = WGPUSurfaceTexture.createDirect();
        wgpu.SurfaceGetCurrentTexture(surface, surfaceTexture);
        //System.out.println("get current texture: "+surfaceTexture.status.get());
        if(surfaceTexture.getStatus() != WGPUSurfaceGetCurrentTextureStatus.Success){
            System.out.println("*** No current texture");
            return WgpuJava.createNullPointer();
        }
        // [...] Create surface texture view
        WGPUTextureViewDescriptor viewDescriptor = WGPUTextureViewDescriptor.createDirect();
        viewDescriptor.setNextInChain();
        viewDescriptor.setLabel("Surface texture view");
        Pointer tex = surfaceTexture.getTexture();
        WGPUTextureFormat format = wgpu.TextureGetFormat(tex);
        //System.out.println("Set format "+format);
        viewDescriptor.setFormat(format);
        viewDescriptor.setDimension(WGPUTextureViewDimension._2D);
        viewDescriptor.setBaseMipLevel(0);
        viewDescriptor.setMipLevelCount(1);
        viewDescriptor.setBaseArrayLayer(0);
        viewDescriptor.setArrayLayerCount(1);
        viewDescriptor.setAspect(WGPUTextureAspect.All);
        Pointer targetView = wgpu.TextureCreateView(surfaceTexture.getTexture(), viewDescriptor);

        return targetView;
    }


   final static long WGPU_LIMIT_U32_UNDEFINED = 4294967295L;
   final static long WGPU_LIMIT_U64_UNDEFINED = Long.MAX_VALUE;//.   18446744073709551615L;
   // should be 18446744073709551615L but Java longs are signed so it is half that, will it work?
    // todo


    void setDefault(WGPULimits limits) {
        limits.setMaxTextureDimension1D(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxTextureDimension2D(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxTextureDimension3D(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxTextureArrayLayers(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxBindGroups(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxBindGroupsPlusVertexBuffers(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxBindingsPerBindGroup(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxDynamicUniformBuffersPerPipelineLayout(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxDynamicStorageBuffersPerPipelineLayout(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxSampledTexturesPerShaderStage(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxSamplersPerShaderStage(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxStorageBuffersPerShaderStage(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxStorageTexturesPerShaderStage(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxUniformBuffersPerShaderStage(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxUniformBufferBindingSize(WGPU_LIMIT_U64_UNDEFINED);
        limits.setMaxStorageBufferBindingSize(WGPU_LIMIT_U64_UNDEFINED);
        limits.setMinUniformBufferOffsetAlignment(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMinStorageBufferOffsetAlignment(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxVertexBuffers(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxBufferSize(WGPU_LIMIT_U64_UNDEFINED);
        limits.setMaxVertexAttributes(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxVertexBufferArrayStride(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxInterStageShaderComponents(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxInterStageShaderVariables(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxColorAttachments(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxColorAttachmentBytesPerSample(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxComputeWorkgroupStorageSize(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxComputeInvocationsPerWorkgroup(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxComputeWorkgroupSizeX(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxComputeWorkgroupSizeY(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxComputeWorkgroupSizeZ(WGPU_LIMIT_U32_UNDEFINED);
        limits.setMaxComputeWorkgroupsPerDimension(WGPU_LIMIT_U32_UNDEFINED);
    }

    private void setDefault(WGPUBindGroupLayoutEntry bindingLayout) {

        bindingLayout.getBuffer().setNextInChain();
        bindingLayout.getBuffer().setType(WGPUBufferBindingType.Undefined);
        bindingLayout.getBuffer().setHasDynamicOffset(0L);

        bindingLayout.getSampler().setNextInChain();
        bindingLayout.getSampler().setType(WGPUSamplerBindingType.Undefined);

        bindingLayout.getStorageTexture().setNextInChain();
        bindingLayout.getStorageTexture().setAccess(WGPUStorageTextureAccess.Undefined);
        bindingLayout.getStorageTexture().setFormat(WGPUTextureFormat.Undefined);
        bindingLayout.getStorageTexture().setViewDimension(WGPUTextureViewDimension.Undefined);

        bindingLayout.getTexture().setNextInChain();
        bindingLayout.getTexture().setMultisampled(0L);
        bindingLayout.getTexture().setSampleType(WGPUTextureSampleType.Undefined);
        bindingLayout.getTexture().setViewDimension(WGPUTextureViewDimension.Undefined);

    }

    private void setDefault(WGPUStencilFaceState stencilFaceState) {
        stencilFaceState.setCompare( WGPUCompareFunction.Always);
        stencilFaceState.setFailOp( WGPUStencilOperation.Keep);
        stencilFaceState.setDepthFailOp( WGPUStencilOperation.Keep);
        stencilFaceState.setPassOp( WGPUStencilOperation.Keep);
    }

    private void setDefault(WGPUDepthStencilState  depthStencilState ) {
        depthStencilState.setFormat(WGPUTextureFormat.Undefined);
        depthStencilState.setDepthWriteEnabled(0L);
        depthStencilState.setDepthCompare(WGPUCompareFunction.Always);
        depthStencilState.setStencilReadMask(0xFFFFFFFF);
        depthStencilState.setStencilWriteMask(0xFFFFFFFF);
        depthStencilState.setDepthBias(0);
        depthStencilState.setDepthBiasSlopeScale(0);
        depthStencilState.setDepthBiasClamp(0);
        setDefault(depthStencilState.getStencilFront());
        setDefault(depthStencilState.getStencilBack());
    }

}
