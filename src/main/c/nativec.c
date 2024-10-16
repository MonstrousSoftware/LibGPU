
#include <stdio.h>
#include <iostream>
#include <cassert>

// Include WebGPU header
#include "webgpu/webgpu.h"


#include "org_example_Main.h"

//#define nullptr ((void*)0)

extern "C" {
/*
 * Class:     org_example_Main
 * Method:    add
 * Signature: (II)I
 */
 int add(int a, int b ){
    return a +b;
 }


 void testStruct( WGPURequestAdapterOptions options ){
    printf("backend: %d\n", options.backendType);
    printf("power: %d\n", options.powerPreference);
    printf("fallback: %d\n", options.forceFallbackAdapter);
    printf("surface: %p\n", options.compatibleSurface);
 }


  void testLimitsStruct( WGPUSupportedLimits *supported ){
     printf("supported: %p\n", supported);
     printf("supported.nextInChain: %p @ %p\n", supported->nextInChain, &(supported->nextInChain));
     printf("supported.limits.maxTextureDimension1D: %d\n", supported->limits.maxTextureDimension1D);
  }





WGPUInstance CreateInstance( void ){

        WGPUInstance instance = wgpuCreateInstance(nullptr);
        printf("creating instance %p\n", instance);
        return instance;
}

void InstanceRelease( WGPUInstance instance ){
        printf("releasing instance %p\n", instance);
        wgpuInstanceRelease(instance);
}

void AdapterRelease( WGPUAdapter adapter ){
    printf("releasing adapter %p\n", adapter);
    wgpuAdapterRelease(adapter);
}

void DeviceRelease( WGPUDevice device ){
    printf("releasing device %p\n", device);
    wgpuDeviceRelease(device);
}

WGPUQueue DeviceGetQueue(WGPUDevice device ){
     WGPUQueue q = wgpuDeviceGetQueue(device);
     printf("get queue => %p\n", q);
     return q;
 }

void QueueRelease( WGPUQueue queue ){
    printf("releasing queue %p\n", queue);
    wgpuQueueRelease(queue);
}


WGPUCommandEncoder DeviceCreateCommandEncoder(WGPUDevice device,  WGPUCommandEncoderDescriptor *encoderDescriptor ){
     printf("encode descriptor label: [%s]\n", encoderDescriptor->label);
     WGPUCommandEncoder e = wgpuDeviceCreateCommandEncoder(device, encoderDescriptor);
     printf("get command encoder => %p\n", e);

     return e;
}

void CommandEncoderRelease(WGPUCommandEncoder encoder){
    printf("releasing encoder %p\n", encoder);
    wgpuCommandEncoderRelease(encoder);
}

void CommandEncoderInsertDebugMarker(WGPUCommandEncoder encoder, char *marker){
    printf("insert debug marker [%s]\n", marker);
    wgpuCommandEncoderInsertDebugMarker(encoder, marker);
}

WGPUCommandBuffer CommandEncoderFinish(WGPUCommandEncoder encoder, WGPUCommandBufferDescriptor *bufferDescriptor){
    WGPUCommandBuffer buf = wgpuCommandEncoderFinish(encoder, bufferDescriptor);
    printf("encoder finish => command %p\n", buf);
    return buf;
}

void QueueSubmit(WGPUQueue queue, size_t count, WGPUCommandBuffer *commands){
    printf("submit %d commands to queue\n", (int)count);
    printf("commands at %p\n", commands);
    printf("command[0] = %p\n", commands[0]);
    wgpuQueueSubmit(queue, count, commands);
}


void QueueOnSubmittedWorkDone(WGPUQueue queue, WGPUQueueWorkDoneCallback callback, void * userdata){
    printf("registering callback for queue submitted work done\n");
    wgpuQueueOnSubmittedWorkDone(queue, callback, userdata);
}

void CommandBufferRelease(WGPUCommandBuffer commandBuffer){
    printf("releasing command buffer %p\n", commandBuffer);
    wgpuCommandBufferRelease(commandBuffer);
}


WGPUBool    AdapterGetLimits(WGPUAdapter adapter, WGPUSupportedLimits *supportedLimits) {
    printf("get limits for adapter %p\n", adapter);

    bool ok = wgpuAdapterGetLimits(adapter, supportedLimits);
    if (ok) {
        std::cout << "Adapter limits:" << std::endl;
        std::cout << " - maxTextureDimension1D: " << supportedLimits->limits.maxTextureDimension1D << std::endl;
        std::cout << " - maxTextureDimension2D: " << supportedLimits->limits.maxTextureDimension2D << std::endl;
        std::cout << " - maxTextureDimension3D: " << supportedLimits->limits.maxTextureDimension3D << std::endl;
        std::cout << " - maxTextureArrayLayers: " << supportedLimits->limits.maxTextureArrayLayers << std::endl;
    }
    return ok;
}


WGPUBool    DeviceGetLimits(WGPUDevice device, WGPUSupportedLimits *supportedLimits) {
    printf("get limits for device %p\n", device);

    bool ok = wgpuDeviceGetLimits(device, supportedLimits);
    if (ok) {
        std::cout << "Device limits:" << std::endl;
        std::cout << " - maxTextureDimension1D: " << supportedLimits->limits.maxTextureDimension1D << std::endl;
        std::cout << " - maxTextureDimension2D: " << supportedLimits->limits.maxTextureDimension2D << std::endl;
        std::cout << " - maxTextureDimension3D: " << supportedLimits->limits.maxTextureDimension3D << std::endl;
        std::cout << " - maxTextureArrayLayers: " << supportedLimits->limits.maxTextureArrayLayers << std::endl;
    }
    return ok;
}

/**
 * Utility function to get a WebGPU adapter, so that
 *     WGPUAdapter adapter = requestAdapterSync(options);
 * is roughly equivalent to
 *     const adapter = await navigator.gpu.requestAdapter(options);
 */
WGPUAdapter RequestAdapterSync(WGPUInstance instance, WGPURequestAdapterOptions const * options) {
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

/**
 * Utility function to get a WebGPU device, so that
 *     WGPUAdapter device = requestDeviceSync(adapter, options);
 * is roughly equivalent to
 *     const device = await adapter.requestDevice(descriptor);
 * It is very similar to requestAdapter
 */
WGPUDevice RequestDeviceSync(WGPUAdapter adapter, WGPUDeviceDescriptor const * descriptor) {
    struct UserData {
        WGPUDevice device = nullptr;
        bool requestEnded = false;
    };
    UserData userData;

    auto onDeviceRequestEnded = [](WGPURequestDeviceStatus status, WGPUDevice device, char const * message, void * pUserData) {
        UserData& userData = *reinterpret_cast<UserData*>(pUserData);
        if (status == WGPURequestDeviceStatus_Success) {
            userData.device = device;
        } else {
            std::cout << "Could not get WebGPU device: " << message << std::endl;
        }
        userData.requestEnded = true;
    };

    wgpuAdapterRequestDevice(
        adapter,
        descriptor,
        onDeviceRequestEnded,
        (void*)&userData
    );

    assert(userData.requestEnded);

    printf("requested device %p\n", userData.device);
    return userData.device;
}

}