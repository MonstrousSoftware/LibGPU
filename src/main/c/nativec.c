
#include <stdio.h>
#include <iostream>
#include <cassert>

// Include WebGPU header
#include "webgpu/webgpu.h"


#include "org_example_Main.h"

//#define nullptr ((void*)0)

extern "C" {

//typedef enum WGPUBackendType {
//    /**
//     * `0x00000000`.
//     * Indicates no value is passed for this argument. See @ref SentinelValues.
//     */
//    WGPUBackendType_Undefined = 0x00000000,
//    WGPUBackendType_Null = 0x00000001,
//    WGPUBackendType_WebGPU = 0x00000002,
//    WGPUBackendType_D3D11 = 0x00000003,
//    WGPUBackendType_D3D12 = 0x00000004,
//    WGPUBackendType_Metal = 0x00000005,
//    WGPUBackendType_Vulkan = 0x00000006,
//    WGPUBackendType_OpenGL = 0x00000007,
//    WGPUBackendType_OpenGLES = 0x00000008,
//    WGPUBackendType_Force32 = 0x7FFFFFFF
//} WGPUBackendType WGPU_ENUM_ATTRIBUTE;

/*
 * Class:     org_example_Main
 * Method:    add
 * Signature: (II)I
 */
 int add(int a, int b ){
    return a +b;
 }


 int subtract(int a, int b ){
    return a - b;
 }

 void testStruct( WGPURequestAdapterOptions options ){
    printf("backend: %d\n", options.backendType);
    printf("power: %d\n", options.powerPreference);
    printf("fallback: %d\n", options.forceFallbackAdapter);
    printf("surface: %p\n", options.compatibleSurface);

 }



WGPUInstance WGPUCreateInstance( void ){

        WGPUInstance instance = wgpuCreateInstance(nullptr);
        printf("creating instance %p\n", instance);
        return instance;
}

void WGPUInstanceRelease( WGPUInstance instance ){
        printf("releasing instance %p\n", instance);
        wgpuInstanceRelease(instance);
}

void WGPUAdapterRelease( WGPUAdapter adapter ){
    printf("releasing adapter %p\n", adapter);
    wgpuAdapterRelease(adapter);
}

WGPUBool    WGPUAdapterGetLimits(WGPUAdapter adapter, WGPUSupportedLimits *supportedLimits) {
    printf("get limits for adapter %p, place in struct at %p\n", adapter, supportedLimits);
    printf("supportedLimits.next %p\n",supportedLimits->nextInChain);
    printf("supportedLimits.limits %p\n",supportedLimits->limits);

    bool ok = wgpuAdapterGetLimits(adapter, supportedLimits);
    if (ok) {
        std::cout << "Adapter limits:" << std::endl;
        std::cout << " - maxTextureDimension1D: " << supportedLimits->limits.maxTextureDimension1D << std::endl;
        std::cout << " - maxTextureDimension2D: " << supportedLimits->limits.maxTextureDimension2D << std::endl;
        std::cout << " - maxTextureDimension3D: " << supportedLimits->limits.maxTextureDimension3D << std::endl;
        std::cout << " - maxTextureArrayLayers: " << supportedLimits->limits.maxTextureArrayLayers << std::endl;
    }

    supportedLimits->nextInChain = 1234;
    return ok;
}

/**
 * Utility function to get a WebGPU adapter, so that
 *     WGPUAdapter adapter = requestAdapterSync(options);
 * is roughly equivalent to
 *     const adapter = await navigator.gpu.requestAdapter(options);
 */
WGPUAdapter requestAdapterSync(WGPUInstance instance, WGPURequestAdapterOptions const * options) {
    // A simple structure holding the local information shared with the
    // onAdapterRequestEnded callback.
    struct UserData {
        WGPUAdapter adapter = nullptr;
        bool requestEnded = false;
    };
    UserData userData;

    // Callback called by wgpuInstanceRequestAdapter when the request returns
    // This is a C++ lambda function, but could be any function defined in the
    // global scope. It must be non-capturing (the brackets [] are empty) so
    // that it behaves like a regular C function pointer, which is what
    // wgpuInstanceRequestAdapter expects (WebGPU being a C API). The workaround
    // is to convey what we want to capture through the pUserData pointer,
    // provided as the last argument of wgpuInstanceRequestAdapter and received
    // by the callback as its last argument.
    auto onAdapterRequestEnded = [](WGPURequestAdapterStatus status, WGPUAdapter adapter, char const * message, void * pUserData) {
        UserData& userData = *reinterpret_cast<UserData*>(pUserData);
        if (status == WGPURequestAdapterStatus_Success) {
            userData.adapter = adapter;
        } else {
            std::cout << "Could not get WebGPU adapter: " << message << std::endl;
        }
        userData.requestEnded = true;
    };

    printf("backend: %d\n", options->backendType);
    printf("power: %d\n", options->powerPreference);
    printf("fallback: %d\n", options->forceFallbackAdapter);
    printf("surface: %p\n", options->compatibleSurface);


    // Call to the WebGPU request adapter procedure
    wgpuInstanceRequestAdapter(
        instance /* equivalent of navigator.gpu */,
        options,
        onAdapterRequestEnded,
        (void*)&userData
    );

    // We wait until userData.requestEnded gets true
    // [...] Wait for request to end

    assert(userData.requestEnded);

    printf("requested adapter %p\n", userData.adapter);
    return userData.adapter;
}

}