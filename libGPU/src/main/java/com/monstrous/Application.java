package com.monstrous;

import com.monstrous.graphics.Texture;
import com.monstrous.graphics.webgpu.RenderPassBuilder;
import com.monstrous.wgpu.*;
import com.monstrous.wgpuUtils.WgpuJava;
import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;

import static com.monstrous.wgpuUtils.WgpuJava.createIntegerArrayPointer;

public class Application {
    public ApplicationConfiguration configuration;
    private final ApplicationListener listener;
    private ApplicationListener nextListener;
    private boolean returnToPreviousListener;
    private boolean mustExitRenderLoop = false;
    private WebGPU webGPU;
    public Pointer depthTextureView;
    public Pointer depthTexture;
    public Pointer targetView;
    private boolean surfaceConfigured = false;
    private boolean isMinimized = false;
    private WindowedApp winApp;
    public GPUTiming gpuTiming;
    public Texture multiSamplingTexture;


    public Application(ApplicationListener listener) {
        this(listener, new ApplicationConfiguration());
    }

    public Application(ApplicationListener listener, ApplicationConfiguration config) {
        LibGPU.app = this;
        this.configuration = config;
        this.listener = listener;

        LibGPU.input = new Input();
        LibGPU.graphics = new Graphics();
        LibGPU.graphics.setSize(config.width, config.height);

        winApp = new WindowedApp();
        winApp.openWindow(this, config);
        initWebGPU(winApp.getWindowHandle());

        while(listener != null) {

            System.out.println("Creating application listener");
            listener.create();
            resize(config.width, config.height);


            // Run the rendering loop until the user has attempted to close
            // the window or the application has called Application.exit().
            while (!mustExitRenderLoop && !winApp.getShouldClose()) {

                // skip rendering if window is minimized to size zero
                // note: also means render() is not called
                if (!isMinimized) {
                    targetView = getNextSurfaceTextureView();
                    if (targetView.address() == 0) {
                        System.out.println("*** Invalid target view");
                        return;
                    }

                    Pointer encoder = prepareEncoder();
                    RenderPassBuilder.setCommandEncoder(encoder);

                    LibGPU.graphics.setDeltaTime(winApp.getDeltaTime());

                    listener.render();

                    finishEncoder(encoder);

                    // At the end of the frame
                    webGPU.TextureViewRelease(targetView);
                    webGPU.SurfacePresent(LibGPU.surface);
                }

                webGPU.DeviceTick(LibGPU.device);

                // Poll for window events. The key callback above will only be
                // invoked during this call.
                winApp.pollEvents();
            }

            System.out.println("Application exit");
            listener.pause();
            listener.dispose();

            // Application chaining
            ApplicationListener currentListener = listener;
            listener = nextListener;    // switch to next listener (null is there is none)
            nextListener = returnToPreviousListener ? currentListener : null;   // return to current listener afterwards
            returnToPreviousListener = false;
            mustExitRenderLoop = false;
        }
        System.out.println("Close Window");
        winApp.closeWindow();
        exitWebGPU();
    }

    // set next listener to create after exiting the current one.
    public void setNextListener( ApplicationListener next, boolean returnAfter ){
        this.nextListener = next;
        this.returnToPreviousListener = returnAfter;
    }

    public void resize(int width, int height){
        System.out.println("Application resize");
        LibGPU.graphics.setSize(width, height);

        terminateDepthBuffer();
        if(surfaceConfigured) {
            terminateSwapChain();
            surfaceConfigured = false;
        }
        // don't crash on resize(0,0) in case of window minimize
        if(width*height > 0) {
            initSwapChain(width, height);
            surfaceConfigured = true;
            initDepthBuffer(width, height);

            if(isMinimized)
                listener.resume();  // resume after restore from minimize
            isMinimized = false;
            listener.resize(width, height); // don't call listener with resize(0,0)

            if(configuration.numSamples > 1) {
                if(multiSamplingTexture != null)
                    multiSamplingTexture.dispose();
                multiSamplingTexture = new Texture(width, height, false, true, LibGPU.surfaceFormat, configuration.numSamples);
            }
        } else {
            if(!isMinimized)
                listener.pause();   // pause on minimize
            isMinimized = true;
        }
    }

    public void exit(){
        if(nextListener != null)
            mustExitRenderLoop = true;
        else
            winApp.setShouldClose(true);
    }


    private void initWebGPU(long windowHandle) {
        webGPU = LibraryLoader.create(WebGPU.class).load("wrapper"); // load the library
        LibGPU.webGPU = webGPU;

        Runtime runtime =Runtime.getRuntime(webGPU);
        WgpuJava.setRuntime(runtime);


        LibGPU.instance = webGPU.CreateInstance();
        System.out.println("instance = "+ LibGPU.instance);

        System.out.println("window = "+Long.toString(windowHandle,16));
        LibGPU.surface = webGPU.glfwGetWGPUSurface(LibGPU.instance, windowHandle);
        System.out.println("surface = "+LibGPU.surface);

        LibGPU.device = initDevice();

        gpuTiming = new GPUTiming(LibGPU.device, configuration.enableGPUtiming);
    }

    private void exitWebGPU() {
        gpuTiming.dispose();

        terminateSwapChain();
        terminateDepthBuffer();
        terminateDevice();

        webGPU.SurfaceRelease(LibGPU.surface);
        webGPU.InstanceRelease(LibGPU.instance);
    }

    private Pointer initDevice() {

        System.out.println("define adapter options");
        WGPURequestAdapterOptions options = WGPURequestAdapterOptions.createDirect();
        options.setNextInChain();
        options.setCompatibleSurface(LibGPU.surface);
        options.setBackendType(LibGPU.app.configuration.backend);
        options.setPowerPreference(WGPUPowerPreference.HighPerformance);

        System.out.println("defined adapter options");

        // Get Adapter
        Pointer adapter = webGPU.RequestAdapterSync(LibGPU.instance, options);
        System.out.println("adapter = " + adapter);

        LibGPU.supportedLimits = WGPUSupportedLimits.createDirect();
        WGPUSupportedLimits supportedLimits = LibGPU.supportedLimits;
        webGPU.AdapterGetLimits(adapter, supportedLimits);
        System.out.println("adapter maxVertexAttributes " + supportedLimits.getLimits().getMaxVertexAttributes());

        System.out.println("maxTextureDimension1D " + supportedLimits.getLimits().getMaxTextureDimension1D());
        System.out.println("maxTextureDimension2D " + supportedLimits.getLimits().getMaxTextureDimension2D());
        System.out.println("maxTextureDimension3D " + supportedLimits.getLimits().getMaxTextureDimension3D());
        System.out.println("maxTextureArrayLayers " + supportedLimits.getLimits().getMaxTextureArrayLayers());


        WGPUAdapterProperties adapterProperties = WGPUAdapterProperties.createDirect();
        adapterProperties.setNextInChain();

        webGPU.AdapterGetProperties(adapter, adapterProperties);

        System.out.println("VendorID: " + adapterProperties.getVendorID());
        System.out.println("Vendor name: " + adapterProperties.getVendorName());
        System.out.println("Device ID: " + adapterProperties.getDeviceID());
        System.out.println("Back end: " + adapterProperties.getBackendType());
        System.out.println("Description: " + adapterProperties.getDriverDescription());


        WGPURequiredLimits requiredLimits = WGPURequiredLimits.createDirect();
        setDefault(requiredLimits.getLimits());
        requiredLimits.getLimits().setMaxVertexAttributes(8);
        requiredLimits.getLimits().setMaxVertexBuffers(2);
        requiredLimits.getLimits().setMaxInterStageShaderComponents(20); //


        // from vert to frag
        requiredLimits.getLimits().setMaxBufferSize(300);
        requiredLimits.getLimits().setMaxVertexBufferArrayStride(11*Float.BYTES);
        requiredLimits.getLimits().setMaxDynamicUniformBuffersPerPipelineLayout(1);
        requiredLimits.getLimits().setMaxTextureDimension1D(2048);
        requiredLimits.getLimits().setMaxTextureDimension2D(2048);
        requiredLimits.getLimits().setMaxTextureArrayLayers(6);
        requiredLimits.getLimits().setMaxSampledTexturesPerShaderStage(1);
        requiredLimits.getLimits().setMaxSamplersPerShaderStage(1);

        // todo these values are incorrect

        requiredLimits.getLimits().setMaxBindGroups(6);
        requiredLimits.getLimits().setMaxUniformBuffersPerShaderStage(4);// We use at most 1 uniform buffer per stage
        // Uniform structs have a size of maximum 16 float (more than what we need)
        requiredLimits.getLimits().setMaxUniformBufferBindingSize(16*4*Float.BYTES);


        // Get Device
        WGPUDeviceDescriptor deviceDescriptor = WGPUDeviceDescriptor.createDirect();
        deviceDescriptor.setNextInChain();
        deviceDescriptor.setLabel("My Device");
        deviceDescriptor.setRequiredLimits(requiredLimits);
        deviceDescriptor.setRequiredFeatureCount(0);
        deviceDescriptor.setRequiredFeatures(null);

        if(configuration.enableGPUtiming){
            int[] featureValues = new int[1];
            featureValues[0] = WGPUFeatureName.TimestampQuery;
            Pointer requiredFeatures = createIntegerArrayPointer(featureValues);

            deviceDescriptor.setRequiredFeatureCount(1);
            deviceDescriptor.setRequiredFeatures( requiredFeatures );
        }

        Pointer device = webGPU.RequestDeviceSync(adapter, deviceDescriptor);

        // use a lambda expression to define a callback function
        WGPUErrorCallback deviceCallback = (WGPUErrorType type, String message, Pointer userdata) -> {
            System.out.println("*** Device error: " + type + " : " + message);
            System.exit(-1);
        };
        webGPU.DeviceSetUncapturedErrorCallback(device, deviceCallback, null);

        webGPU.DeviceGetLimits(device, supportedLimits);
        System.out.println("device maxVertexAttributes " + supportedLimits.getLimits().getMaxVertexAttributes());

        System.out.println("maxTextureDimension1D " + supportedLimits.getLimits().getMaxTextureDimension1D());
        System.out.println("maxTextureDimension2D " + supportedLimits.getLimits().getMaxTextureDimension2D());
        System.out.println("maxTextureDimension3D " + supportedLimits.getLimits().getMaxTextureDimension3D());
        System.out.println("maxTextureArrayLayers " + supportedLimits.getLimits().getMaxTextureArrayLayers());

        LibGPU.queue = webGPU.DeviceGetQueue(device);

//        // use a lambda expression to define a callback function
//        WGPUQueueWorkDoneCallback queueCallback = (WGPUQueueWorkDoneStatus status, Pointer userdata) -> {
//            System.out.println("=== Queue work finished with status: " + status);
//        };
//        wgpu.QueueOnSubmittedWorkDone(LibGPU.queue, queueCallback, null);



        WGPUSurfaceCapabilities caps = WGPUSurfaceCapabilities.createDirect();
        webGPU.SurfaceGetCapabilities(LibGPU.surface, adapter, caps);
        //System.out.println("Surface Capabilities: formatCount: "+caps.getFormatCount());
        Pointer formats = caps.getFormats();
        int format = formats.getInt(0);
        LibGPU.surfaceFormat = WGPUTextureFormat.values()[format];

        // Deprecated:
        //LibGPU.surfaceFormat = wgpu.SurfaceGetPreferredFormat(LibGPU.surface, adapter);
        System.out.println("Using format: " + LibGPU.surfaceFormat);

        webGPU.AdapterRelease(adapter);       // we can release our adapter as soon as we have a device
        return device;
    }

    private void terminateDevice(){
        webGPU.QueueRelease(LibGPU.queue);
        webGPU.DeviceRelease(LibGPU.device);
    }

    private void initSwapChain(int width, int height){
        // configure the surface
        WGPUSurfaceConfiguration config = WGPUSurfaceConfiguration.createDirect();
        config.setNextInChain();

        config.setWidth(width);
        config.setHeight(height);

        config.setFormat(LibGPU.surfaceFormat);
        // And we do not need any particular view format:
        config.setViewFormatCount(0);
        config.setViewFormats(WgpuJava.createNullPointer());
        config.setUsage(WGPUTextureUsage.RenderAttachment);
        config.setDevice(LibGPU.device);
        config.setPresentMode(LibGPU.app.configuration.vsyncEnabled ? WGPUPresentMode.Fifo : WGPUPresentMode.Immediate);
        config.setAlphaMode(WGPUCompositeAlphaMode.Auto);

        webGPU.SurfaceConfigure(LibGPU.surface, config);

    }

    private void terminateSwapChain(){
        webGPU.SurfaceUnconfigure(LibGPU.surface);
    }

    private Pointer getNextSurfaceTextureView() {
        // [...] Get the next surface texture


        WGPUSurfaceTexture surfaceTexture = WGPUSurfaceTexture.createDirect();
        webGPU.SurfaceGetCurrentTexture(LibGPU.surface, surfaceTexture);
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
        WGPUTextureFormat format = webGPU.TextureGetFormat(tex);
        //System.out.println("Set format "+format);
        viewDescriptor.setFormat(format);
        viewDescriptor.setDimension(WGPUTextureViewDimension._2D);
        viewDescriptor.setBaseMipLevel(0);
        viewDescriptor.setMipLevelCount(1);
        viewDescriptor.setBaseArrayLayer(0);
        viewDescriptor.setArrayLayerCount(1);
        viewDescriptor.setAspect(WGPUTextureAspect.All);
        return webGPU.TextureCreateView(surfaceTexture.getTexture(), viewDescriptor);
    }

    private void initDepthBuffer(int width, int height){

        WGPUTextureFormat depthTextureFormat = WGPUTextureFormat.Depth24Plus;

        long[] formats = new long[1];
        formats[0] = depthTextureFormat.ordinal();
        Pointer formatPtr = WgpuJava.createLongArrayPointer(formats);

        // Create the depth texture
        WGPUTextureDescriptor depthTextureDesc = WGPUTextureDescriptor.createDirect();
        depthTextureDesc.setNextInChain();
        depthTextureDesc.setDimension( WGPUTextureDimension._2D);
        depthTextureDesc.setFormat( depthTextureFormat );
        depthTextureDesc.setMipLevelCount(1);
        depthTextureDesc.setSampleCount(configuration.numSamples);
        depthTextureDesc.getSize().setWidth(width);
        depthTextureDesc.getSize().setHeight(height);
        depthTextureDesc.getSize().setDepthOrArrayLayers(1);
        depthTextureDesc.setUsage( WGPUTextureUsage.RenderAttachment );
        depthTextureDesc.setViewFormatCount(1);
        depthTextureDesc.setViewFormats( formatPtr );
        depthTexture = webGPU.DeviceCreateTexture(LibGPU.device, depthTextureDesc);


        // Create the view of the depth texture manipulated by the rasterizer
        WGPUTextureViewDescriptor depthTextureViewDesc = WGPUTextureViewDescriptor.createDirect();
        depthTextureViewDesc.setAspect(WGPUTextureAspect.DepthOnly);
        depthTextureViewDesc.setBaseArrayLayer(0);
        depthTextureViewDesc.setArrayLayerCount(1);
        depthTextureViewDesc.setBaseMipLevel(0);
        depthTextureViewDesc.setMipLevelCount(1);
        depthTextureViewDesc.setDimension( WGPUTextureViewDimension._2D);
        depthTextureViewDesc.setFormat(depthTextureFormat);
        depthTextureView = webGPU.TextureCreateView(depthTexture, depthTextureViewDesc);

    }

    private void terminateDepthBuffer(){
        // Destroy the depth texture and its view
        if(depthTextureView != null)
            webGPU.TextureViewRelease(depthTextureView);
        if(depthTexture != null) {
            webGPU.TextureDestroy(depthTexture);
            webGPU.TextureRelease(depthTexture);
        }
        depthTextureView = null;
        depthTexture = null;
    }

    private Pointer prepareEncoder() {
        WGPUCommandEncoderDescriptor encoderDescriptor = WGPUCommandEncoderDescriptor.createDirect();
        encoderDescriptor.setNextInChain();
        encoderDescriptor.setLabel("My Encoder");

        return webGPU.DeviceCreateCommandEncoder(LibGPU.device, encoderDescriptor);
    }

//    private Pointer prepareRenderPass(Pointer encoder){
//
//        WGPURenderPassColorAttachment renderPassColorAttachment = WGPURenderPassColorAttachment.createDirect();
//        renderPassColorAttachment.setNextInChain();
//        renderPassColorAttachment.setView(LibGPU.app.targetView);
//        renderPassColorAttachment.setResolveTarget(WgpuJava.createNullPointer());
//        renderPassColorAttachment.setLoadOp(WGPULoadOp.Clear);
//        renderPassColorAttachment.setStoreOp(WGPUStoreOp.Store);
//
//        renderPassColorAttachment.getClearValue().setR(clearColor.r);
//        renderPassColorAttachment.getClearValue().setG(clearColor.g);
//        renderPassColorAttachment.getClearValue().setB(clearColor.b);
//        renderPassColorAttachment.getClearValue().setA(clearColor.a);
//
//        renderPassColorAttachment.setDepthSlice(wgpu.WGPU_DEPTH_SLICE_UNDEFINED);
//
//
//        WGPURenderPassDepthStencilAttachment depthStencilAttachment = WGPURenderPassDepthStencilAttachment.createDirect();
//        depthStencilAttachment.setView( LibGPU.app.depthTextureView );
//        depthStencilAttachment.setDepthClearValue(1.0f);
//        depthStencilAttachment.setDepthLoadOp(WGPULoadOp.Clear);
//        depthStencilAttachment.setDepthStoreOp(WGPUStoreOp.Store);
//        depthStencilAttachment.setDepthReadOnly(0L);
//        depthStencilAttachment.setStencilClearValue(0);
//        depthStencilAttachment.setStencilLoadOp(WGPULoadOp.Undefined);
//        depthStencilAttachment.setStencilStoreOp(WGPUStoreOp.Undefined);
//        depthStencilAttachment.setStencilReadOnly(1L);
//
//
//
//        WGPURenderPassDescriptor renderPassDescriptor = WGPURenderPassDescriptor.createDirect();
//        renderPassDescriptor.setNextInChain();
//
//        renderPassDescriptor.setLabel("Main Render Pass");
//
//        renderPassDescriptor.setColorAttachmentCount(1);
//        renderPassDescriptor.setColorAttachments( renderPassColorAttachment );
//        renderPassDescriptor.setOcclusionQuerySet(WgpuJava.createNullPointer());
//        renderPassDescriptor.setDepthStencilAttachment( depthStencilAttachment );
//
//        gpuTiming.configureRenderPassDescriptor(renderPassDescriptor);
//
//        return wgpu.CommandEncoderBeginRenderPass(encoder, renderPassDescriptor);
//    }

//    private void finalizeRenderPass(Pointer renderPass) {
//        wgpu.RenderPassEncoderEnd(renderPass);
//        wgpu.RenderPassEncoderRelease(renderPass);
//    }

    private void finishEncoder(Pointer encoder){
        gpuTiming.resolveTimeStamps(encoder);
        WGPUCommandBufferDescriptor bufferDescriptor =  WGPUCommandBufferDescriptor.createDirect();
        bufferDescriptor.setNextInChain();
        bufferDescriptor.setLabel("Command Buffer");
        Pointer commandBuffer = webGPU.CommandEncoderFinish(encoder, bufferDescriptor);
        webGPU.CommandEncoderRelease(encoder);


        long[] buffers = new long[1];
        buffers[0] = commandBuffer.address();
        Pointer bufferPtr = WgpuJava.createLongArrayPointer(buffers);

        webGPU.QueueSubmit(LibGPU.queue, 1, bufferPtr);

        gpuTiming.fetchTimestamps();

        webGPU.CommandBufferRelease(commandBuffer);
    }


    public float getAverageGPUtime() {
        return gpuTiming.getAverageGPUtime();
    }

    final static long WGPU_LIMIT_U32_UNDEFINED = 4294967295L;
    final static long WGPU_LIMIT_U64_UNDEFINED = Long.MAX_VALUE;//.   18446744073709551615L;
    // should be 18446744073709551615L but Java longs are signed so it is half that, will it work?
    // todo


    public void setDefault(WGPULimits limits) {
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
}
