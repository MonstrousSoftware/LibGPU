package com.monstrous;

import com.monstrous.utils.WgpuJava;
import com.monstrous.wgpu.WGPU;
import jnr.ffi.LibraryLoader;
import jnr.ffi.Runtime;

public class Application {

    public Application(ApplicationListener listener) {
        this(listener, new ApplicationConfiguration());
    }

    public Application(ApplicationListener listener, ApplicationConfiguration config) {


        WindowedApp winApp = new WindowedApp();
        winApp.openWindow(config);
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
    }

    private void initWebGPU(long windowHandle) {
        WGPU wgpu = LibraryLoader.create(WGPU.class).load("wrapper"); // load the library
        LibGPU.wgpu = wgpu;

        Runtime runtime =Runtime.getRuntime(wgpu);
        WgpuJava.setRuntime(runtime);


        LibGPU.instance =wgpu.CreateInstance();
        System.out.println("instance = "+ LibGPU.instance);

        System.out.println("window = "+Long.toString(windowHandle,16));
        LibGPU.surface =wgpu.glfwGetWGPUSurface(LibGPU.instance, windowHandle);
        System.out.println("surface = "+LibGPU.surface);
    }

    private void exitWebGPU() {
        WGPU wgpu = LibGPU.wgpu;

        wgpu.SurfaceUnconfigure(LibGPU.surface);
        wgpu.SurfaceRelease(LibGPU.surface);
        wgpu.InstanceRelease(LibGPU.instance);
    }
}
