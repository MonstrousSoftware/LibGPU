package com.monstrous;

import com.monstrous.wgpu.*;
import com.monstrous.wgpuUtils.WgpuJava;
import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;

public class Application {
    public ApplicationConfiguration configuration;
    private final ApplicationListener listener;
    private WGPU wgpu;
    public Pointer depthTextureView;
    public Pointer depthTexture;
    //public Pointer surface;

    public Application(ApplicationListener listener) {
        this(listener, new ApplicationConfiguration());
    }

    public Application(ApplicationListener listener, ApplicationConfiguration config) {
        LibGPU.application = this;
        this.configuration = config;
        this.listener = listener;

        LibGPU.graphics = new Graphics();
        LibGPU.graphics.setSize(config.width, config.height);


        WindowedApp winApp = new WindowedApp();
        winApp.openWindow(this, config);
        initWebGPU(winApp.getWindowHandle());

        listener.init();

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!winApp.shouldClose()) {

            listener.render( winApp.getDeltaTime() );


            // Poll for window events. The key callback above will only be
            // invoked during this call.
            winApp.pollEvents();
        }

        System.out.println("Application exit");
        listener.exit();
        System.out.println("Close Window");
        winApp.closeWindow();
        exitWebGPU();
    }

    public void resize(int width, int height){
        System.out.println("Application resize");
        LibGPU.graphics.setSize(width, height);
        listener.resize(width, height);
    }


    private void initWebGPU(long windowHandle) {
        wgpu = LibraryLoader.create(WGPU.class).load("wrapper"); // load the library
        LibGPU.wgpu = wgpu;

        Runtime runtime =Runtime.getRuntime(wgpu);
        WgpuJava.setRuntime(runtime);


        LibGPU.instance =wgpu.CreateInstance();
        System.out.println("instance = "+ LibGPU.instance);

        System.out.println("window = "+Long.toString(windowHandle,16));
        LibGPU.surface =wgpu.glfwGetWGPUSurface(LibGPU.instance, windowHandle);
        System.out.println("surface = "+LibGPU.surface);

        initDevice();
        initDepthBuffer();
        initSwapChain();
    }

    private void exitWebGPU() {
        terminateDepthBuffer();
        terminateDevice();
        terminateSwapChain();

        wgpu.InstanceRelease(LibGPU.instance);
    }

    private void initDevice() {

        System.out.println("define adapter options");
        WGPURequestAdapterOptions options = WGPURequestAdapterOptions.createDirect();
        options.setNextInChain();
        options.setCompatibleSurface(LibGPU.surface);
        options.setBackendType(LibGPU.application.configuration.backend);

        System.out.println("defined adapter options");

        // Get Adapter
        Pointer adapter = wgpu.RequestAdapterSync(LibGPU.instance, options);
        System.out.println("adapter = " + adapter);

        LibGPU.supportedLimits = WGPUSupportedLimits.createDirect();
        WGPUSupportedLimits supportedLimits = LibGPU.supportedLimits;
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

        Pointer device = wgpu.RequestDeviceSync(adapter, deviceDescriptor);
        LibGPU.device = device;


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

        LibGPU.queue = wgpu.DeviceGetQueue(device);

//        // use a lambda expression to define a callback function
//        WGPUQueueWorkDoneCallback queueCallback = (WGPUQueueWorkDoneStatus status, Pointer userdata) -> {
//            System.out.println("=== Queue work finished with status: " + status);
//        };
//        wgpu.QueueOnSubmittedWorkDone(LibGPU.queue, queueCallback, null);

        LibGPU.surfaceFormat = wgpu.SurfaceGetPreferredFormat(LibGPU.surface, adapter);
        System.out.println("Using format: " + LibGPU.surfaceFormat);

        wgpu.AdapterRelease(adapter);       // we can release our adapter as soon as we have a device
    }

    private void terminateDevice(){
        wgpu.QueueRelease(LibGPU.queue);
        wgpu.DeviceRelease(LibGPU.device);
    }

    private void initSwapChain(){
        // configure the surface
        WGPUSurfaceConfiguration config = WGPUSurfaceConfiguration.createDirect();
        config.setNextInChain();

        config.setWidth(LibGPU.graphics.getWidth());
        config.setHeight(LibGPU.graphics.getHeight());


        config.setFormat(LibGPU.surfaceFormat);
        // And we do not need any particular view format:
        config.setViewFormatCount(0);
        config.setViewFormats(WgpuJava.createNullPointer());
        config.setUsage(WGPUTextureUsage.RenderAttachment);
        config.setDevice(LibGPU.device);
        config.setPresentMode(LibGPU.application.configuration.vsyncEnabled ? WGPUPresentMode.Fifo : WGPUPresentMode.Immediate);
        config.setAlphaMode(WGPUCompositeAlphaMode.Auto);

        wgpu.SurfaceConfigure(LibGPU.surface, config);
    }

    private void terminateSwapChain(){
        wgpu.SurfaceUnconfigure(LibGPU.surface);
        wgpu.SurfaceRelease(LibGPU.surface);
    }

    private void initDepthBuffer(){

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
        depthTextureDesc.setSampleCount(1);
        depthTextureDesc.getSize().setWidth(LibGPU.graphics.getWidth());
        depthTextureDesc.getSize().setHeight(LibGPU.graphics.getHeight());
        depthTextureDesc.getSize().setDepthOrArrayLayers(1);
        depthTextureDesc.setUsage( WGPUTextureUsage.RenderAttachment );
        depthTextureDesc.setViewFormatCount(1);
        depthTextureDesc.setViewFormats( formatPtr );
        depthTexture = wgpu.DeviceCreateTexture(LibGPU.device, depthTextureDesc);


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

    }

    private void terminateDepthBuffer(){
        // Destroy the depth texture and its view
        wgpu.TextureViewRelease(depthTextureView);
        wgpu.TextureDestroy(depthTexture);
        wgpu.TextureRelease(depthTexture);
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
