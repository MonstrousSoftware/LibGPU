package org.example;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.Struct;

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



    public interface LibC { // A representation of libC in Java


        public final class WGPURequestAdapterOptions extends WgpuJavaStruct {
            public final Pointer nextInChain = new Pointer();

            public final Pointer compatibleSurface = new Pointer();
            public final Struct.Enum<WGPUPowerPreference> powerPreference = new Struct.Enum<>(WGPUPowerPreference.class);
            public final Struct.Enum<WGPUBackendType> backendType = new Struct.Enum<>(WGPUBackendType.class);
            public final WBOOL forceFallbackAdapter = new WBOOL();
        }

//        typedef struct WGPULimits {
//            uint32_t maxTextureDimension1D;
//            uint32_t maxTextureDimension2D;
//            uint32_t maxTextureDimension3D;
//            uint32_t maxTextureArrayLayers;
//            uint32_t maxBindGroups;
        //  etc.

        public final class WGPULimits extends WgpuJavaStruct {
            public final Unsigned32 maxTextureDimension1D = new Unsigned32();
            public final Unsigned32 maxTextureDimension2D = new Unsigned32();
            public final Unsigned32 maxTextureDimension3D = new Unsigned32();
            public final Unsigned32 maxTextureArrayLayers = new Unsigned32();
            public final Unsigned32 maxBindGroups = new Unsigned32();
            public final Unsigned32 maxBindGroupsPlusVertexBuffers = new Unsigned32();
            public final Unsigned32 maxBindingsPerBindGroup = new Unsigned32();
            public final Unsigned32 maxDynamicUniformBuffersPerPipelineLayout = new Unsigned32();
            public final Unsigned32 maxDynamicStorageBuffersPerPipelineLayout = new Unsigned32();
            public final Unsigned32 maxSampledTexturesPerShaderStage = new Unsigned32();
            public final Unsigned32 maxSamplersPerShaderStage = new Unsigned32();
            public final Unsigned32 maxStorageBuffersPerShaderStage = new Unsigned32();
            public final Unsigned32 maxStorageTexturesPerShaderStage = new Unsigned32();
            public final Unsigned32 maxUniformBuffersPerShaderStage = new Unsigned32();
            public final Unsigned64 maxUniformBufferBindingSize = new Unsigned64();
            public final Unsigned64 maxStorageBufferBindingSize = new Unsigned64();
            public final Unsigned32 minUniformBufferOffsetAlignment = new Unsigned32();
            public final Unsigned32 minStorageBufferOffsetAlignment = new Unsigned32();
            public final Unsigned32 maxVertexBuffers = new Unsigned32();
            public final Unsigned64 maxBufferSize = new Unsigned64();
            public final Unsigned32 maxVertexAttributes = new Unsigned32();
            public final Unsigned32 maxVertexBufferArrayStride = new Unsigned32();
            public final Unsigned32 maxInterStageShaderComponents = new Unsigned32();
            public final Unsigned32 maxInterStageShaderVariables = new Unsigned32();
            public final Unsigned32 maxColorAttachments = new Unsigned32();
            public final Unsigned32 maxColorAttachmentBytesPerSample = new Unsigned32();
            public final Unsigned32 maxComputeWorkgroupStorageSize = new Unsigned32();
            public final Unsigned32 maxComputeInvocationsPerWorkgroup = new Unsigned32();
            public final Unsigned32 maxComputeWorkgroupSizeX = new Unsigned32();
            public final Unsigned32 maxComputeWorkgroupSizeY = new Unsigned32();
            public final Unsigned32 maxComputeWorkgroupSizeZ = new Unsigned32();
            public final Unsigned32 maxComputeWorkgroupsPerDimension = new Unsigned32();


            public WGPULimits(Runtime runtime) {
                super(runtime);
            }

            public WGPULimits() {
                super(runtime);
            }
        }

//        typedef struct WGPUSupportedLimits {
//            WGPUChainedStructOut * nextInChain;
//            WGPULimits limits;                                    // not a pointer but an inner struct!!!
//        } WGPUSupportedLimits WGPU_STRUCTURE_ATTRIBUTE;

        public final class WGPUSupportedLimits extends WgpuJavaStruct {
            public final Pointer nextInChain = new Pointer();
            public final WGPULimits limits = inner(new WGPULimits());

        }


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
        LibC libc = LibraryLoader.create(LibC.class).load("nativec"); // load the library into the libc variable
        runtime = Runtime.getRuntime(libc);
        WgpuJava.setRuntime(runtime);


        System.out.println("Hello world!");
        int sum = libc.add(1200, 34);
        System.out.println("sum = "+sum);

        LibC.WGPURequestAdapterOptions options = new LibC.WGPURequestAdapterOptions();
        options.backendType.set(WGPUBackendType.Undefined);
        options.forceFallbackAdapter.set(true);
        options.powerPreference.set(WGPUPowerPreference.HighPerformance);

        libc.testStruct(options);


        Pointer instance = libc.WGPUCreateInstance();

        Pointer adapter = libc.requestAdapterSync(instance, options);

        LibC.WGPUSupportedLimits supportedLimits = new LibC.WGPUSupportedLimits();
        supportedLimits.useDirectMemory();

        //libc.testLimitsStruct(supportedLimits);
        libc.WGPUAdapterGetLimits(adapter, supportedLimits);

        System.out.println("maxTextureDimension1D " + supportedLimits.limits.maxTextureDimension1D);
        System.out.println("maxTextureDimension2D " + supportedLimits.limits.maxTextureDimension2D);
        System.out.println("maxTextureDimension3D " + supportedLimits.limits.maxTextureDimension3D);
        System.out.println("maxTextureArrayLayers " + supportedLimits.limits.maxTextureArrayLayers);

        libc.WGPUAdapterRelease(adapter);
        libc.WGPUInstanceRelease(instance);

    }

}
