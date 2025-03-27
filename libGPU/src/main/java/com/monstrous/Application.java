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

package com.monstrous;

import com.monstrous.utils.JavaWebGPU;
import com.monstrous.webgpu.*;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.webgpu.RenderPassBuilder;
import com.monstrous.webgpu.WebGPU_JNI;
import jnr.ffi.Pointer;

import static com.monstrous.utils.JavaWebGPU.createIntegerArrayPointer;

public class Application {
    public ApplicationConfiguration configuration;
    public Pointer depthTextureView;
    public WGPUTextureFormat depthTextureFormat;
    public Pointer depthTexture;
    public Pointer targetView;
    public GPUTiming gpuTiming;
    public Texture multiSamplingTexture;

    private ApplicationListener listener;
    private ApplicationListener nextListener;
    private boolean returnToPreviousListener;
    private boolean mustExitRenderLoop = false;
    private WebGPU_JNI webGPU;
    private boolean surfaceConfigured = false;
    private boolean isMinimized = false;
    private WindowedApp winApp;



    public Application(ApplicationListener listener) {
        this(listener, new ApplicationConfiguration());
    }

    public Application(ApplicationListener applicationListener, ApplicationConfiguration config) {
        System.out.println("LibGPU v0.1");
        LibGPU.app = this;
        this.listener = applicationListener;
        this.configuration = config;

        LibGPU.input = new Input();
        LibGPU.graphics = new Graphics();
        LibGPU.graphics.setSize(config.width, config.height);

        winApp = new WindowedApp();
        if(!config.noWindow)
            winApp.openWindow(this, config);
        initWebGPU(winApp.getWindowHandle());

        while(listener != null) {

            //System.out.println("Creating application listener");
            listener.create();

            if(!config.noWindow)
                resize(LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());


            // Run the rendering loop until the user has attempted to close
            // the window or the application has called Application.exit().
            while ( !mustExitRenderLoop && !winApp.getShouldClose()) {

                // skip rendering if window is minimized to size zero
                // note: also means render() is not called
                if (!isMinimized) {
                    targetView = getNextSurfaceTextureView();
                    if (targetView.address() == 0) {
                        System.out.println("*** Invalid target view");
                        return;
                    }

                    LibGPU.commandEncoder = prepareEncoder();

                    LibGPU.graphics.setDeltaTime(winApp.getDeltaTime());
                    LibGPU.graphics.passNumber = 0;

                    listener.render();

                    finishEncoder(LibGPU.commandEncoder);
                    LibGPU.commandEncoder = null;

                    // At the end of the frame
                    webGPU.wgpuTextureViewRelease(targetView);
                    webGPU.wgpuSurfacePresent(LibGPU.surface);
                }

                webGPU.wgpuDeviceTick(LibGPU.device);

                // Poll for window events. The key callback above will only be
                // invoked during this call.
                winApp.pollEvents();
            }

            System.out.println("Application exit");
            listener.pause();
            listener.dispose();

            // Application chaining: if nextListener is defined, we start up a new listener now that the current listener has exited.
            // if returnToPreviousListener is true, then we will return to the current listener when the new listener exits.
            //
            ApplicationListener currentListener = listener;
            listener = nextListener;    // switch to next listener (null is there is none)
            nextListener = returnToPreviousListener ? currentListener : null;   // return to current listener afterwards
            returnToPreviousListener = false;
            mustExitRenderLoop = false;
        }
        winApp.closeWindow();
        exitWebGPU();
    }

    // set next listener to create after exiting the current one.
    public void setNextListener( ApplicationListener next, boolean returnAfter ){
        this.nextListener = next;
        this.returnToPreviousListener = returnAfter;
    }

    public void resize(int width, int height){
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

            if(isMinimized) {
                listener.resume();  // resume after restore from minimize
                isMinimized = false;
            }
            listener.resize(width, height);

            if(configuration.numSamples > 1 ) {
                if(multiSamplingTexture != null)
                    multiSamplingTexture.dispose();
                multiSamplingTexture = new Texture(width, height, false, true, LibGPU.surfaceFormat, configuration.numSamples);
            }
        } else {
            if(!isMinimized) {
                listener.pause();   // pause on minimize
                isMinimized = true;
            }
        }
    }

    public void exit(){
        if(nextListener != null)
            mustExitRenderLoop = true;
        else
            winApp.setShouldClose(true);
    }


    private void initWebGPU(long windowHandle) {
        webGPU = JavaWebGPU.init();
        LibGPU.webGPU = webGPU;

        LibGPU.instance = webGPU.wgpuCreateInstance(null);

        // get window surface
        if(windowHandle != 0)
            LibGPU.surface = JavaWebGPU.getUtils().glfwGetWGPUSurface(LibGPU.instance, windowHandle);

        LibGPU.device = initDevice();

        // enable gpu timing if configured
        gpuTiming = new GPUTiming(LibGPU.device, configuration.enableGPUtiming);
    }

    private void exitWebGPU() {
        gpuTiming.dispose();

        terminateSwapChain();
        terminateDepthBuffer();
        terminateDevice();

        webGPU.wgpuSurfaceRelease(LibGPU.surface);
        webGPU.wgpuInstanceRelease(LibGPU.instance);
    }

    /** obtain Device, create Queue */
    private Pointer initDevice() {


        WGPURequestAdapterOptions options = WGPURequestAdapterOptions.createDirect();
        options.setNextInChain();
        options.setCompatibleSurface(LibGPU.surface);
        options.setBackendType(LibGPU.app.configuration.backend);
        options.setPowerPreference(WGPUPowerPreference.HighPerformance);

        if(LibGPU.app.configuration.backend == WGPUBackendType.Null)
            throw new IllegalStateException("Request Adapter: Back end 'Null' only valid if config.noWindow is true");

        // Get Adapter
        Pointer adapter = getAdapterSync(LibGPU.instance, options);
        if(adapter == null){
            System.out.println("Configured adapter back end ("+LibGPU.app.configuration.backend+") not available, requesting fallback");
            options.setBackendType(WGPUBackendType.Undefined);
            options.setPowerPreference(WGPUPowerPreference.HighPerformance);
            adapter = getAdapterSync(LibGPU.instance, options);
        }


        LibGPU.supportedLimits = WGPUSupportedLimits.createDirect();
        WGPUSupportedLimits supportedLimits = LibGPU.supportedLimits;
        webGPU.wgpuAdapterGetLimits(adapter, supportedLimits);
//        System.out.println("adapter maxVertexAttributes " + supportedLimits.getLimits().getMaxVertexAttributes());
//        System.out.println("adapter maxBindGroups " + supportedLimits.getLimits().getMaxBindGroups());
//
//        System.out.println("maxTextureDimension1D " + supportedLimits.getLimits().getMaxTextureDimension1D());
//        System.out.println("maxTextureDimension2D " + supportedLimits.getLimits().getMaxTextureDimension2D());
//        System.out.println("maxTextureDimension3D " + supportedLimits.getLimits().getMaxTextureDimension3D());
//        System.out.println("maxTextureArrayLayers " + supportedLimits.getLimits().getMaxTextureArrayLayers());


        WGPUAdapterProperties adapterProperties = WGPUAdapterProperties.createDirect();
        adapterProperties.setNextInChain();

        webGPU.wgpuAdapterGetProperties(adapter, adapterProperties);
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

        // todo these values are rather random

        requiredLimits.getLimits().setMaxBindGroups(4);
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

        // required feature to do timestamp queries
        if(configuration.enableGPUtiming){
            int[] featureValues = new int[1];
            featureValues[0] = WGPUFeatureName.TimestampQuery;
            Pointer requiredFeatures = createIntegerArrayPointer(featureValues);

            deviceDescriptor.setRequiredFeatureCount(1);
            deviceDescriptor.setRequiredFeatures( requiredFeatures );
        }

        Pointer device = getDeviceSync(adapter, deviceDescriptor);

        // use a lambda expression to define a callback function
        WGPUErrorCallback deviceCallback = (WGPUErrorType type, String message, Pointer userdata) -> {
            System.out.println("*** Device error: " + type + " : " + message);
            System.exit(-1);
        };
        webGPU.wgpuDeviceSetUncapturedErrorCallback(device, deviceCallback, null);

        // Collect the device limits which may be more constrained than the adapter limits
        // e.g. getMinUniformBufferOffsetAlignment maybe becomes 256 on the device instead of 64 on the adapter.
        webGPU.wgpuDeviceGetLimits(device, LibGPU.supportedLimits);
//        System.out.println("device maxVertexAttributes " + supportedLimits.getLimits().getMaxVertexAttributes());
//
//        System.out.println("maxTextureDimension1D " + supportedLimits.getLimits().getMaxTextureDimension1D());
//        System.out.println("maxTextureDimension2D " + supportedLimits.getLimits().getMaxTextureDimension2D());
//        System.out.println("maxTextureDimension3D " + supportedLimits.getLimits().getMaxTextureDimension3D());
//        System.out.println("maxTextureArrayLayers " + supportedLimits.getLimits().getMaxTextureArrayLayers());


        if (configuration.enableGPUtiming && !webGPU.wgpuDeviceHasFeature(device, WGPUFeatureName.TimestampQuery)) {
            System.out.println("** Requested timestamp queries are not supported!");
        }


        LibGPU.queue = webGPU.wgpuDeviceGetQueue(device);

//        // use a lambda expression to define a callback function
//        WGPUQueueWorkDoneCallback queueCallback = (WGPUQueueWorkDoneStatus status, Pointer userdata) -> {
//            System.out.println("=== Queue work finished with status: " + status);
//        };
//        wgpu.QueueOnSubmittedWorkDone(LibGPU.queue, queueCallback, null);

        //System.out.println("Surface Capabilities: formatCount: "+caps.getFormatCount());
        if(LibGPU.surface != null) {
            WGPUSurfaceCapabilities caps = WGPUSurfaceCapabilities.createDirect();
            webGPU.wgpuSurfaceGetCapabilities(LibGPU.surface, adapter, caps);
            Pointer formats = caps.getFormats();
            int format = formats.getInt(0);
            LibGPU.surfaceFormat = WGPUTextureFormat.values()[format];

            // Deprecated:
            //LibGPU.surfaceFormat = wgpu.SurfaceGetPreferredFormat(LibGPU.surface, adapter);
            System.out.println("Using format: " + LibGPU.surfaceFormat);
        } else {
            System.out.println("No render surface.");
        }


        webGPU.wgpuAdapterRelease(adapter);       // we can release our adapter as soon as we have a device
        return device;
    }


    private Pointer getAdapterSync(Pointer instance, WGPURequestAdapterOptions options){

        Pointer userBuf = JavaWebGPU.createLongArrayPointer(new long[1]);
        userBuf.putPointer(0, null);

        WGPURequestAdapterCallback callback = (WGPURequestAdapterStatus status, Pointer adapter, String message, Pointer userdata) -> {
            if(status == WGPURequestAdapterStatus.Success)
                userdata.putPointer(0, adapter);
            else
                System.out.println("Could not get adapter: "+message);
        };
        webGPU.wgpuInstanceRequestAdapter(instance, options, callback, userBuf);
        // on native implementations, we don't have to wait for asynchronous operation. It returns result immediately.
        return  userBuf.getPointer(0);
    }

    private Pointer getDeviceSync(Pointer adapter, WGPUDeviceDescriptor deviceDescriptor){

        Pointer userBuf = JavaWebGPU.createLongArrayPointer(new long[1]);
        WGPURequestDeviceCallback callback = (WGPURequestDeviceStatus status, Pointer device, String message, Pointer userdata) -> {
            if(status == WGPURequestDeviceStatus.Success)
                userdata.putPointer(0, device);
            else
                System.out.println("Could not get device: "+message);
        };
        webGPU.wgpuAdapterRequestDevice(adapter, deviceDescriptor, callback, userBuf);
        // on native implementations, we don't have to wait for asynchronous operation. It returns result immediately.
        return  userBuf.getPointer(0);
    }

    private void terminateDevice(){
        webGPU.wgpuQueueRelease(LibGPU.queue);
        webGPU.wgpuDeviceRelease(LibGPU.device);
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
        config.setViewFormats(JavaWebGPU.createNullPointer());
        config.setUsage(WGPUTextureUsage.RenderAttachment);
        config.setDevice(LibGPU.device);
        config.setPresentMode(LibGPU.app.configuration.vsyncEnabled ? WGPUPresentMode.Fifo : WGPUPresentMode.Immediate);
        config.setAlphaMode(WGPUCompositeAlphaMode.Auto);

        webGPU.wgpuSurfaceConfigure(LibGPU.surface, config);

    }

    private void terminateSwapChain(){
        webGPU.wgpuSurfaceUnconfigure(LibGPU.surface);
    }

    private Pointer getNextSurfaceTextureView() {
        // [...] Get the next surface texture


        WGPUSurfaceTexture surfaceTexture = WGPUSurfaceTexture.createDirect();
        webGPU.wgpuSurfaceGetCurrentTexture(LibGPU.surface, surfaceTexture);
        //System.out.println("get current texture: "+surfaceTexture.status.get());
        if(surfaceTexture.getStatus() != WGPUSurfaceGetCurrentTextureStatus.Success){
            System.out.println("*** No current texture");
            return JavaWebGPU.createNullPointer();
        }
        // [...] Create surface texture view
        WGPUTextureViewDescriptor viewDescriptor = WGPUTextureViewDescriptor.createDirect();
        viewDescriptor.setNextInChain();
        viewDescriptor.setLabel("Surface texture view");
        Pointer tex = surfaceTexture.getTexture();
        WGPUTextureFormat format = webGPU.wgpuTextureGetFormat(tex);
        //System.out.println("Set format "+format);
        viewDescriptor.setFormat(format);
        viewDescriptor.setDimension(WGPUTextureViewDimension._2D);
        viewDescriptor.setBaseMipLevel(0);
        viewDescriptor.setMipLevelCount(1);
        viewDescriptor.setBaseArrayLayer(0);
        viewDescriptor.setArrayLayerCount(1);
        viewDescriptor.setAspect(WGPUTextureAspect.All);
        return webGPU.wgpuTextureCreateView(surfaceTexture.getTexture(), viewDescriptor);
    }

    private void initDepthBuffer(int width, int height){

        depthTextureFormat = WGPUTextureFormat.Depth24Plus;

        long[] formats = new long[1];
        formats[0] = depthTextureFormat.ordinal();
        Pointer formatPtr = JavaWebGPU.createLongArrayPointer(formats);

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
        depthTexture = webGPU.wgpuDeviceCreateTexture(LibGPU.device, depthTextureDesc);


        // Create the view of the depth texture manipulated by the rasterizer
        WGPUTextureViewDescriptor depthTextureViewDesc = WGPUTextureViewDescriptor.createDirect();
        depthTextureViewDesc.setAspect(WGPUTextureAspect.DepthOnly);
        depthTextureViewDesc.setBaseArrayLayer(0);
        depthTextureViewDesc.setArrayLayerCount(1);
        depthTextureViewDesc.setBaseMipLevel(0);
        depthTextureViewDesc.setMipLevelCount(1);
        depthTextureViewDesc.setDimension( WGPUTextureViewDimension._2D);
        depthTextureViewDesc.setFormat(depthTextureFormat);
        depthTextureView = webGPU.wgpuTextureCreateView(depthTexture, depthTextureViewDesc);

    }

    private void terminateDepthBuffer(){
        // Destroy the depth texture and its view
        if(depthTextureView != null)
            webGPU.wgpuTextureViewRelease(depthTextureView);
        if(depthTexture != null) {
            webGPU.wgpuTextureDestroy(depthTexture);
            webGPU.wgpuTextureRelease(depthTexture);
        }
        depthTextureView = null;
        depthTexture = null;
    }

    public Pointer prepareEncoder() {
        WGPUCommandEncoderDescriptor encoderDescriptor = WGPUCommandEncoderDescriptor.createDirect();
        encoderDescriptor.setNextInChain();
        encoderDescriptor.setLabel("My Encoder");

        return webGPU.wgpuDeviceCreateCommandEncoder(LibGPU.device, encoderDescriptor);
    }

    public void finishEncoder(Pointer encoder){
        gpuTiming.resolveTimeStamps(encoder);
        WGPUCommandBufferDescriptor bufferDescriptor =  WGPUCommandBufferDescriptor.createDirect();
        bufferDescriptor.setNextInChain();
        bufferDescriptor.setLabel("Command Buffer");
        Pointer commandBuffer = webGPU.wgpuCommandEncoderFinish(encoder, bufferDescriptor);
        webGPU.wgpuCommandEncoderRelease(encoder);


        long[] buffers = new long[1];
        buffers[0] = commandBuffer.address();
        Pointer bufferPtr = JavaWebGPU.createLongArrayPointer(buffers);

        webGPU.wgpuQueueSubmit(LibGPU.queue, 1, bufferPtr);

        gpuTiming.fetchTimestamps();

        webGPU.wgpuCommandBufferRelease(commandBuffer);
    }


    public float getAverageGPUtime() {
        return gpuTiming.getAverageGPUtime();
    }

    final static long WGPU_LIMIT_U32_UNDEFINED = -1L;
    final static long WGPU_LIMIT_U64_UNDEFINED = -1L;//.   18446744073709551615L;
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
