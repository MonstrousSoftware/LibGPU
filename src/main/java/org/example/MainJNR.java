package org.example;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;

public class MainJNR {
    private static Runtime runtime;

    enum WGPUPowerPreference {
        Undefined,
        LowPower,
        HighPerformance,
    };
    enum WGPUBackendType {
        Undefined,
        Null,
        WebGPU,
        D3D11,
        D3D12,
        Metal,
        Vulkan,
        OpenGL,
        OpenGLES
    };


    public interface WGPU { // A representation of libC in Java

        int add(int a, int b);

        void testStruct(WGPURequestAdapterOptions options);
        void testLimitsStruct(WGPUSupportedLimits supported);

        Pointer WGPUCreateInstance();
        void WGPUInstanceRelease(Pointer instance);

        Pointer requestAdapterSync(Pointer instance, WGPURequestAdapterOptions options);

        void WGPUAdapterRelease(Pointer adapter);

        boolean    WGPUAdapterGetLimits(Pointer adapter, WGPUSupportedLimits limits);
    }

    public static void main(String[] args) {
        MainJNR main = new MainJNR();
        main.runTest();

    }

    public void runTest() {
        WGPU wgpu = LibraryLoader.create(WGPU.class).load("nativec"); // load the library into the libc variable
        runtime = Runtime.getRuntime(wgpu);
        WgpuJava.setRuntime(runtime);

        // debug test
        System.out.println("Hello world!");
        int sum = wgpu.add(1200, 34);
        System.out.println("sum = "+sum);


        WGPURequestAdapterOptions options = new WGPURequestAdapterOptions();
        options.backendType.set(WGPUBackendType.Undefined);
        options.forceFallbackAdapter.set(true);
        options.powerPreference.set(WGPUPowerPreference.HighPerformance);

        Pointer instance = wgpu.WGPUCreateInstance();
        Pointer adapter = wgpu.requestAdapterSync(instance, options);

        WGPUSupportedLimits supportedLimits = new WGPUSupportedLimits();
        supportedLimits.useDirectMemory();

        wgpu.WGPUAdapterGetLimits(adapter, supportedLimits);

        System.out.println("maxTextureDimension1D " + supportedLimits.limits.maxTextureDimension1D);
        System.out.println("maxTextureDimension2D " + supportedLimits.limits.maxTextureDimension2D);
        System.out.println("maxTextureDimension3D " + supportedLimits.limits.maxTextureDimension3D);
        System.out.println("maxTextureArrayLayers " + supportedLimits.limits.maxTextureArrayLayers);


        // cleanup
        wgpu.WGPUAdapterRelease(adapter);
        wgpu.WGPUInstanceRelease(instance);

    }

}
