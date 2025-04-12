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

import com.monstrous.graphics.webgpu.*;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.webgpu.*;
import com.monstrous.graphics.Texture;
import com.monstrous.webgpu.WebGPU_JNI;
import jnr.ffi.Pointer;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

import static com.monstrous.utils.JavaWebGPU.createIntegerArrayPointer;
import static org.lwjgl.system.MemoryStack.stackPush;

public class Application {
    public ApplicationConfiguration configuration;
    public TextureView depthTextureView;
    public WGPUTextureFormat depthTextureFormat;
    public Texture depthTexture;
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
    private Pointer instance;
    private Device device;

    private final WGPUSurfaceTexture surfaceTexture;
    private final WGPUTextureViewDescriptor viewDescriptor;



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
        device = initWebGPU(winApp.getWindowHandle());
        LibGPU.device = device;
        LibGPU.queue = new Queue(device);

        // pre-allocate some structures we'll use often
        surfaceTexture = WGPUSurfaceTexture.createDirect();
        viewDescriptor = WGPUTextureViewDescriptor.createDirect();

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

                    CommandEncoder encoder = new CommandEncoder(device);
                    LibGPU.commandEncoder = encoder.getHandle();        // e.g. RenderPassBuilder needs it

                    LibGPU.graphics.setDeltaTime(winApp.getDeltaTime());
                    LibGPU.graphics.passNumber = 0;

                    listener.render();

                    finishEncoder(encoder);
                    encoder.dispose();
                    LibGPU.commandEncoder = null;

                            // At the end of the frame
                    webGPU.wgpuTextureViewRelease(targetView);
                    webGPU.wgpuSurfacePresent(LibGPU.surface);
                }
                device.tick();

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
        LibGPU.queue.dispose();
        device.dispose();
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


    private Device initWebGPU(long windowHandle) {
        System.out.println(JavaWebGPU.getVersionString());

        webGPU = JavaWebGPU.init();
        LibGPU.webGPU = webGPU;

        instance = webGPU.wgpuCreateInstance(null);

        // get window surface
        if(windowHandle != 0)
            LibGPU.surface = JavaWebGPU.getUtils().glfwGetWGPUSurface(instance, windowHandle);

        Adapter adapter = new Adapter(instance, LibGPU.surface);

        device = new Device(adapter);

        LibGPU.surfaceFormat = getSurfaceFormat(adapter, LibGPU.surface);

        adapter.dispose();  // finished with adapter now that we have a device

        // enable gpu timing if configured
        gpuTiming = new GPUTiming(device, configuration.enableGPUtiming);
        return device;
    }

    private WGPUTextureFormat getSurfaceFormat(Adapter adapter, Pointer surface){
        WGPUTextureFormat surfaceFormat = WGPUTextureFormat.Undefined;
        if(surface != null) {
            WGPUSurfaceCapabilities caps = WGPUSurfaceCapabilities.createDirect();
            webGPU.wgpuSurfaceGetCapabilities(LibGPU.surface, adapter.getHandle(), caps);
            Pointer formats = caps.getFormats();
            int format = formats.getInt(0);
            surfaceFormat = WGPUTextureFormat.values()[format];

            System.out.println("Using format: " + LibGPU.surfaceFormat);
        } else {
            System.out.println("No render surface.");
        }
        return surfaceFormat;
    }

    private void exitWebGPU() {
        gpuTiming.dispose();

        terminateSwapChain();
        terminateDepthBuffer();

        webGPU.wgpuSurfaceRelease(LibGPU.surface);
        webGPU.wgpuInstanceRelease(instance);
    }

    /** obtain Device, create Queue */
//    private Device initDevice() {
//
//
////        WGPURequestAdapterOptions options = WGPURequestAdapterOptions.createDirect();
////        options.setNextInChain();
////        options.setCompatibleSurface(LibGPU.surface);
////        options.setBackendType(LibGPU.app.configuration.backend);
////        options.setPowerPreference(WGPUPowerPreference.HighPerformance);
////
////        if(LibGPU.app.configuration.backend == WGPUBackendType.Null)
////            throw new IllegalStateException("Request Adapter: Back end 'Null' only valid if config.noWindow is true");
////
////        // Get Adapter
////        Pointer adapter = getAdapterSync(instance, options);
////        if(adapter == null){
////            System.out.println("Configured adapter back end ("+LibGPU.app.configuration.backend+") not available, requesting fallback");
////            options.setBackendType(WGPUBackendType.Undefined);
////            options.setPowerPreference(WGPUPowerPreference.HighPerformance);
////            adapter = getAdapterSync(instance, options);
////        }
////
////
////        LibGPU.supportedLimits = WGPUSupportedLimits.createDirect();
////        WGPUSupportedLimits supportedLimits = LibGPU.supportedLimits;
////        webGPU.wgpuAdapterGetLimits(adapter, supportedLimits);
//////        System.out.println("adapter maxVertexAttributes " + supportedLimits.getLimits().getMaxVertexAttributes());
//////        System.out.println("adapter maxBindGroups " + supportedLimits.getLimits().getMaxBindGroups());
//////
//////        System.out.println("maxTextureDimension1D " + supportedLimits.getLimits().getMaxTextureDimension1D());
//////        System.out.println("maxTextureDimension2D " + supportedLimits.getLimits().getMaxTextureDimension2D());
//////        System.out.println("maxTextureDimension3D " + supportedLimits.getLimits().getMaxTextureDimension3D());
//////        System.out.println("maxTextureArrayLayers " + supportedLimits.getLimits().getMaxTextureArrayLayers());
////
////
////        WGPUAdapterProperties adapterProperties = WGPUAdapterProperties.createDirect();
////        adapterProperties.setNextInChain();
////
////        webGPU.wgpuAdapterGetProperties(adapter, adapterProperties);
////        System.out.println("VendorID: " + adapterProperties.getVendorID());
////        System.out.println("Vendor name: " + adapterProperties.getVendorName());
////        System.out.println("Device ID: " + adapterProperties.getDeviceID());
////        System.out.println("Back end: " + adapterProperties.getBackendType());
////        System.out.println("Description: " + adapterProperties.getDriverDescription());
//
////        device = new Device(adapter);
////
////        //System.out.println("Surface Capabilities: formatCount: "+caps.getFormatCount());
////        if(LibGPU.surface != null) {
////            WGPUSurfaceCapabilities caps = WGPUSurfaceCapabilities.createDirect();
////            webGPU.wgpuSurfaceGetCapabilities(LibGPU.surface, adapter, caps);
////            Pointer formats = caps.getFormats();
////            int format = formats.getInt(0);
////            LibGPU.surfaceFormat = WGPUTextureFormat.values()[format];
////
////            // Deprecated:
////            //LibGPU.surfaceFormat = wgpu.SurfaceGetPreferredFormat(LibGPU.surface, adapter);
////            System.out.println("Using format: " + LibGPU.surfaceFormat);
////        } else {
////            System.out.println("No render surface.");
////        }
////
////
////        webGPU.wgpuAdapterRelease(adapter);       // we can release our adapter as soon as we have a device
////        return device;
//    }




//    private Pointer getDeviceSync(Pointer adapter, WGPUDeviceDescriptor deviceDescriptor){
//
//        Pointer userBuf = JavaWebGPU.createLongArrayPointer(new long[1]);
//        WGPURequestDeviceCallback callback = (WGPURequestDeviceStatus status, Pointer device, String message, Pointer userdata) -> {
//            if(status == WGPURequestDeviceStatus.Success)
//                userdata.putPointer(0, device);
//            else
//                System.out.println("Could not get device: "+message);
//        };
//        webGPU.wgpuAdapterRequestDevice(adapter, deviceDescriptor, callback, userBuf);
//        // on native implementations, we don't have to wait for asynchronous operation. It returns result immediately.
//        return  userBuf.getPointer(0);
//    }

//    private void terminateDevice(){
//        LibGPU.queue.dispose();
//        //webGPU.wgpuQueueRelease(LibGPU.queue);
//        device.dispose();
//        //webGPU.wgpuDeviceRelease(device);
//    }

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
        config.setDevice(device.getHandle());
        config.setPresentMode(LibGPU.app.configuration.vsyncEnabled ? WGPUPresentMode.Fifo : WGPUPresentMode.Immediate);
        config.setAlphaMode(WGPUCompositeAlphaMode.Auto);

        webGPU.wgpuSurfaceConfigure(LibGPU.surface, config);

    }

    private void terminateSwapChain(){
        webGPU.wgpuSurfaceUnconfigure(LibGPU.surface);
    }

    private Pointer getNextSurfaceTextureView() {
        // [...] Get the next surface texture

        // WGPUSurfaceTexture surfaceTexture = WGPUSurfaceTexture.createDirect();
        webGPU.wgpuSurfaceGetCurrentTexture(LibGPU.surface, surfaceTexture);
        //System.out.println("get current texture: "+surfaceTexture.status.get());
        if(surfaceTexture.getStatus() != WGPUSurfaceGetCurrentTextureStatus.Success){
            System.out.println("*** No current texture");
            return JavaWebGPU.createNullPointer();
        }
        // [...] Create surface texture view

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

        depthTexture = new Texture(width, height, 1, WGPUTextureUsage.RenderAttachment,
                depthTextureFormat,configuration.numSamples, depthTextureFormat );

        // Create the view of the depth texture manipulated by the rasterizer
        depthTextureView = new TextureView(depthTexture, WGPUTextureAspect.DepthOnly, WGPUTextureViewDimension._2D,depthTextureFormat, 0, 1, 0, 1 );
    }

    private void terminateDepthBuffer(){
        // Destroy the depth texture and its view
        if(depthTextureView != null)
            depthTextureView.dispose();

        if(depthTexture != null) {
            depthTexture.dispose();
        }
        depthTextureView = null;
        depthTexture = null;
    }

//    public Pointer prepareEncoder() {
//        encoderDescriptor.setNextInChain().setLabel("My Encoder");
//        return webGPU.wgpuDeviceCreateCommandEncoder(LibGPU.device, encoderDescriptor);
//    }

//    public void finishEncoder(Pointer encoder){
//        gpuTiming.resolveTimeStamps(encoder);
//
//        //CommandBuffer commandBuffer = enod
//
//        bufferDescriptor.setNextInChain().setLabel("Command Buffer");
//        Pointer commandBuffer = webGPU.wgpuCommandEncoderFinish(encoder, bufferDescriptor);
//        webGPU.wgpuCommandEncoderRelease(encoder);
//
//        try (MemoryStack stack = stackPush()) {
//            // create native array of command buffer pointers
//            ByteBuffer pBuffers = stack.malloc(Long.BYTES);
//            pBuffers.putLong(0, commandBuffer.address());
//
//            webGPU.wgpuQueueSubmit(LibGPU.queue, 1, JavaWebGPU.createByteBufferPointer(pBuffers));
//        }
//
//        gpuTiming.fetchTimestamps();
//
//        webGPU.wgpuCommandBufferRelease(commandBuffer);
//    }

    public void finishEncoder(CommandEncoder encoder){
        gpuTiming.resolveTimeStamps(encoder.getHandle());

        CommandBuffer commandBuffer = encoder.finish();

        LibGPU.queue.submit(commandBuffer);

//        bufferDescriptor.setNextInChain().setLabel("Command Buffer");
//        Pointer commandBuffer = webGPU.wgpuCommandEncoderFinish(encoder, bufferDescriptor);
//        webGPU.wgpuCommandEncoderRelease(encoder);

//        try (MemoryStack stack = stackPush()) {
//            // create native array of command buffer pointers
//            ByteBuffer pBuffers = stack.malloc(Long.BYTES);
//            pBuffers.putLong(0, commandBuffer.address());
//
//            webGPU.wgpuQueueSubmit(LibGPU.queue, 1, JavaWebGPU.createByteBufferPointer(pBuffers));
//        }

        gpuTiming.fetchTimestamps();

        commandBuffer.dispose();

       // webGPU.wgpuCommandBufferRelease(commandBuffer);
    }


    public float getAverageGPUtime() {
        return gpuTiming.getAverageGPUtime();
    }

}
