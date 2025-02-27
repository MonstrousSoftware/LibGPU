#define DAWN


#if __cplusplus < 201103L
  #error This library needs at least a C++11 compliant compiler
#endif

//#include "glfw3webgpu.h"

#include <stdio.h>
#include <cassert>
#include <iostream>
#include <stdalign.h>

#define LOG(x)

using namespace std;

// note we should't mix printf and cout <<

// Include WebGPU header

#ifdef DAWN
#include "dawn/webgpu.h"
#else
#include "webgpu/webgpu.h"
#endif

#if defined(_WIN32)
#   define EXPORT __declspec(dllexport)
#endif

#define GLFW_EXPOSE_NATIVE_WIN32
#include <GLFW/glfw3.h>
#include <GLFW/glfw3native.h>


//#include "org_example_Main.h"

#ifdef __cplusplus
extern "C" {
#endif

#ifndef LEAN

EXPORT WGPUInstance CreateInstance( void ){

#ifdef DAWN
        printf("Linked to dawn.dll\n");
#else
        printf("Linked to wgpu-native.dll\n");
#endif
        WGPUInstance instance = wgpuCreateInstance(nullptr);
        LOG( printf("creating instance %p\n", instance); )
        return instance;
}

EXPORT void InstanceRelease( WGPUInstance instance ){
        LOG( printf("releasing instance %p\n", instance); )
        wgpuInstanceRelease(instance);
}

EXPORT void AdapterRelease( WGPUAdapter adapter ){
    LOG( printf("releasing adapter %p\n", adapter); )
    wgpuAdapterRelease(adapter);
}

EXPORT void DeviceRelease( WGPUDevice device ){
    LOG( printf("releasing device %p\n", device); )
    wgpuDeviceRelease(device);
}

EXPORT void DeviceTick( WGPUDevice device ){
    LOG( printf("device tick\n", device); )
    wgpuDeviceTick(device);
}


EXPORT WGPUQueue DeviceGetQueue(WGPUDevice device ){
     WGPUQueue q = wgpuDeviceGetQueue(device);
     LOG( printf("get queue => %p\n", q); )
     return q;
 }

 EXPORT void DeviceSetUncapturedErrorCallback(WGPUDevice device, WGPUErrorCallback callback, void * userdata){
     LOG( printf("registering callback for device errors\n"); )
     wgpuDeviceSetUncapturedErrorCallback(device, callback, userdata);
 }

EXPORT void QueueRelease( WGPUQueue queue ){
    LOG( printf("releasing queue %p\n", queue); )
    wgpuQueueRelease(queue);
}


EXPORT WGPUCommandEncoder DeviceCreateCommandEncoder(WGPUDevice device,  WGPUCommandEncoderDescriptor *encoderDescriptor ){
     LOG( printf("encode descriptor label: [%s]\n", encoderDescriptor->label); )
     WGPUCommandEncoder e = wgpuDeviceCreateCommandEncoder(device, encoderDescriptor);
     LOG( printf("get command encoder => %p\n", e); )

     return e;
}

EXPORT void CommandEncoderRelease(WGPUCommandEncoder encoder){
    LOG( printf("releasing encoder %p\n", encoder); )
    wgpuCommandEncoderRelease(encoder);
}

EXPORT void RenderPassEncoderEnd(WGPURenderPassEncoder encoder){
    wgpuRenderPassEncoderEnd(encoder);
}

EXPORT void RenderPassEncoderRelease(WGPURenderPassEncoder encoder){
    wgpuRenderPassEncoderRelease(encoder);
}

EXPORT void CommandEncoderInsertDebugMarker(WGPUCommandEncoder encoder, char *marker){
    //printf("insert debug marker [%s]\n", marker);
    wgpuCommandEncoderInsertDebugMarker(encoder, marker);
}

EXPORT WGPUCommandBuffer CommandEncoderFinish(WGPUCommandEncoder encoder, WGPUCommandBufferDescriptor *bufferDescriptor){
    WGPUCommandBuffer buf = wgpuCommandEncoderFinish(encoder, bufferDescriptor);
    LOG( printf("encoder finish => command %p\n", buf); )
    return buf;
}

void dumpColAtt( WGPURenderPassColorAttachment colAtt){
    cout << "nextInChain=" << colAtt.nextInChain << endl;
    cout << "depthSlice=" << colAtt.depthSlice << endl;
    printf("clearValue @ %p size %lu\n", &colAtt.clearValue, (unsigned long)sizeof(WGPUColor));
    printf("clearValue rgba @ %p, %p, %p, %p \n", &colAtt.clearValue.r,  &colAtt.clearValue.g,  &colAtt.clearValue.b,  &colAtt.clearValue.a);
    cout << "clearValue: r=" << colAtt.clearValue.r
     << "g=" << colAtt.clearValue.g
     << "b=" << colAtt.clearValue.b
     << "a=" << colAtt.clearValue.a << endl;
    cout << "loadOp=" << colAtt.loadOp << endl;
    cout << "storeOp=" << colAtt.storeOp << endl;
}

EXPORT WGPURenderPassEncoder  CommandEncoderBeginRenderPass(WGPUCommandEncoder encoder, WGPURenderPassDescriptor *renderPassDescriptor){
    WGPURenderPassEncoder pass = wgpuCommandEncoderBeginRenderPass(encoder, renderPassDescriptor);
    LOG( dumpColAtt(renderPassDescriptor->colorAttachments[0]); )
    return pass;
}



EXPORT void QueueSubmit(WGPUQueue queue, size_t count, WGPUCommandBuffer *commands){
//    printf("submit %d commands to queue\n", (int)count);
//    printf("commands at %p\n", commands);
//    printf("command[0] = %p\n", commands[0]);
    wgpuQueueSubmit(queue, count, commands);
}



EXPORT void QueueOnSubmittedWorkDone(WGPUQueue queue, WGPUQueueWorkDoneCallback callback, void * userdata){
    LOG( printf("registering callback for queue submitted work done\n"); )
    wgpuQueueOnSubmittedWorkDone(queue, callback, userdata);
}

EXPORT void CommandBufferRelease(WGPUCommandBuffer commandBuffer){
    //printf("releasing command buffer %p\n", commandBuffer);
    wgpuCommandBufferRelease(commandBuffer);
}

EXPORT void SurfaceRelease(WGPUSurface surface){
    //printf("releasing surface %p\n", surface);
    wgpuSurfaceRelease(surface);
}

EXPORT void SurfaceConfigure(WGPUSurface surface, WGPUSurfaceConfiguration *config){
    LOG( printf("configure surface %p\n", surface); )
    wgpuSurfaceConfigure(surface, config);
}

EXPORT void SurfaceUnconfigure(WGPUSurface surface){
    LOG( printf("unconfiguring surface %p\n", surface); )
    wgpuSurfaceUnconfigure(surface);
}

EXPORT int SurfaceGetPreferredFormat(WGPUSurface surface, WGPUAdapter adapter){
    //cout << "SurfaceGetPreferredFormat " << surface << ", " << adapter << endl;

    WGPUTextureFormat format = wgpuSurfaceGetPreferredFormat(surface, adapter);

    //cout << "preferred format " << format << endl;
    return format;
}

EXPORT int SurfaceGetCapabilities(WGPUSurface surface, WGPUAdapter adapter, WGPUSurfaceCapabilities *caps){
    LOG( "SurfaceGetCapabilities ");

    WGPUStatus status = wgpuSurfaceGetCapabilities(surface, adapter, caps);

    //cout << "preferred format " << format << endl;
    return status;
}

EXPORT void SurfaceGetCurrentTexture(WGPUSurface surface, WGPUSurfaceTexture *surfaceTexture ){
    //cout << "SurfaceGetCurrentTexture " << endl;

    wgpuSurfaceGetCurrentTexture(surface, surfaceTexture);
}

EXPORT WGPUTextureFormat TextureGetFormat(WGPUTexture texture){
    //cout << "TextureGetFormat " << endl;
    WGPUTextureFormat format = wgpuTextureGetFormat(texture);
    return format;
}

EXPORT WGPUTextureView TextureCreateView(WGPUTexture texture, WGPUTextureViewDescriptor *viewDescriptor){
    WGPUTextureView view = wgpuTextureCreateView(texture, viewDescriptor);
    return view;
}

EXPORT void TextureViewRelease(WGPUTextureView view){
    //cout << "TextureViewRelease " << endl;
    wgpuTextureViewRelease(view);
}

EXPORT void SurfacePresent(WGPUSurface surface){
    //cout << "SurfacePresent " << endl;
    wgpuSurfacePresent(surface);
}

EXPORT WGPUBool    AdapterGetLimits(WGPUAdapter adapter, WGPUSupportedLimits *supportedLimits) {
    //printf("get limits for adapter %p\n", adapter);

    bool ok = wgpuAdapterGetLimits(adapter, supportedLimits);
//    if (ok) {
//        std::cout << "Adapter limits:" << std::endl;
//        std::cout << " - maxTextureDimension1D: " << supportedLimits->limits.maxTextureDimension1D << std::endl;
//        std::cout << " - maxTextureDimension2D: " << supportedLimits->limits.maxTextureDimension2D << std::endl;
//        std::cout << " - maxTextureDimension3D: " << supportedLimits->limits.maxTextureDimension3D << std::endl;
//        std::cout << " - maxTextureArrayLayers: " << supportedLimits->limits.maxTextureArrayLayers << std::endl;
//    }
    return ok;
}

EXPORT void AdapterGetProperties(WGPUAdapter adapter, WGPUAdapterProperties *properties){
//	WGPUAdapterProperties properties = {};
//	properties.nextInChain = nullptr;

    //printf("get properties for adapter %p to %p\n", adapter, properties);
    wgpuAdapterGetProperties(adapter, properties);
    std::cout << "Vendor ID:" << properties->vendorID << std::endl;
    std::cout << "Vendor name:" << properties->vendorName << std::endl;
    std::cout << "Architecture:" << properties->architecture << std::endl;
    std::cout << "Device ID:" << properties->deviceID << std::endl;
    std::cout << "Driver Description:" << properties->driverDescription << std::endl;
}


EXPORT WGPUBool    DeviceGetLimits(WGPUDevice device, WGPUSupportedLimits *supportedLimits) {
    //printf("get limits for device %p\n", device);

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


EXPORT void RenderPassEncoderSetPipeline(WGPURenderPassEncoder  renderPass, WGPURenderPipeline pipeline){
    wgpuRenderPassEncoderSetPipeline(renderPass, pipeline);
}

EXPORT void RenderPassEncoderDraw(WGPURenderPassEncoder renderPass, uint32_t numVertices,uint32_t numInstances, uint32_t firstVertex, uint32_t firstInstance){
    wgpuRenderPassEncoderDraw(renderPass, numVertices, numInstances, firstVertex, firstInstance);
}

EXPORT void RenderPassEncoderDrawIndexed(WGPURenderPassEncoder renderPass, uint32_t indexCount, uint32_t instanceCount, uint32_t firstIndex, int32_t baseVertex, uint32_t firstInstance){
   wgpuRenderPassEncoderDrawIndexed(renderPass, indexCount, instanceCount, firstIndex, baseVertex, firstInstance);
}

EXPORT void RenderPassEncoderSetIndexBuffer(WGPURenderPassEncoder renderPassEncoder, WGPUBuffer buffer, WGPUIndexFormat format, uint64_t offset, uint64_t size) {
    wgpuRenderPassEncoderSetIndexBuffer( renderPassEncoder,  buffer,  format,  offset,  size);
};


EXPORT WGPURenderPipeline DeviceCreateRenderPipeline(WGPUDevice device, WGPURenderPipelineDescriptor *pipelineDesc){
//    WGPUVertexState vertex = pipelineDesc->vertex;
//    std::cout << " - buffer Count: " << vertex.bufferCount << std::endl;
//    WGPUVertexBufferLayout layout = vertex.buffers[0];
//    std::cout << " - attribute Count: " << layout.attributeCount << std::endl;
//
//    WGPUVertexAttribute atty[2], va;
//    atty[0].format = WGPUVertexFormat_Float32x2;
//    atty[0].offset = -1;
//    atty[0].shaderLocation = 8;
//        atty[1].format = WGPUVertexFormat_Float32x3;
//        atty[1].offset = -1;
//        atty[1].shaderLocation = 9;
//    unsigned char const *p2 = (unsigned char const *)&atty;
//
//     printf("sizeof VA %ld \n", sizeof(va));
//     printf("alignof VA %ld \n", alignof(WGPUVertexAttribute));

//    for(int i = 0; i < 48; i++){
//        unsigned char k = *p2;
//        printf("atty[%d]: %d\n", i, k);
//                p2++;
//    }

//    printf("attribs @ %p\n", layout.attributes);
//    unsigned char const *p = (unsigned char const *)layout.attributes;
//    for(int i = 0; i < 48; i++){
//        unsigned char k = *p;
//
//        printf("attribs[%d]: %d\n", i, k);
//                p++;
//    }
//    for(int i = 0; i < layout.attributeCount; i++){
//        std::cout << " - attribute " << i << std::endl;
//        printf("attribs[%d] @ %p\n", i, &(layout.attributes[i]) );
//        WGPUVertexAttribute attrib = layout.attributes[i];
//        std::cout << "    - attribute " << attrib.format  << "," << attrib.offset << ","<< attrib.shaderLocation << std::endl;
//
//    }
    WGPURenderPipeline pipeline = wgpuDeviceCreateRenderPipeline(device, pipelineDesc);
    return pipeline;
}

EXPORT WGPUShaderModule DeviceCreateShaderModule(WGPUDevice device, WGPUShaderModuleDescriptor *shaderDesc){
    return wgpuDeviceCreateShaderModule(device, shaderDesc);
}

EXPORT void RenderPipelineRelease(WGPURenderPipeline pipeline){
    wgpuRenderPipelineRelease(pipeline);
}

EXPORT void ShaderModuleRelease(WGPUShaderModule shaderModule){
    wgpuShaderModuleRelease(shaderModule);
}

EXPORT WGPUBuffer DeviceCreateBuffer(WGPUDevice device, WGPUBufferDescriptor *bufferDesc){
    WGPUBuffer buf =  wgpuDeviceCreateBuffer(device, bufferDesc);
    //printf("created buffer at %p\n", buf);
    return buf;
}

EXPORT void BufferRelease(WGPUBuffer buffer){
    wgpuBufferRelease(buffer);
}

EXPORT void QueueWriteBuffer(WGPUQueue queue, WGPUBuffer buffer, uint64_t bufferOffset, void const * data, size_t size){
    wgpuQueueWriteBuffer(queue, buffer, bufferOffset, data, size);
}

EXPORT void CommandEncoderCopyBufferToBuffer(WGPUCommandEncoder commandEncoder, WGPUBuffer source, uint64_t sourceOffset, WGPUBuffer destination, uint64_t destinationOffset, uint64_t size) {
    //printf("copy buf to buf: encoder %p src %p srcOffset %lld dst %p dstOffset %lld amnt %lld", commandEncoder, source, sourceOffset, destination, destinationOffset, size);
    wgpuCommandEncoderCopyBufferToBuffer(commandEncoder, source, sourceOffset, destination, destinationOffset, size);
//        wgpuCommandEncoderCopyBufferToBuffer(commandEncoder, source, sourceOffset, destination, destinationOffset, size);
}

EXPORT void BufferMapAsync(WGPUBuffer buffer, WGPUMapMode wgpuMapMode, size_t offset, size_t size, WGPUBufferMapCallback callback, void * userData){
    wgpuBufferMapAsync(buffer, wgpuMapMode, offset, size, callback, userData);
}

EXPORT void const * BufferGetConstMappedRange(WGPUBuffer buffer, size_t offset, size_t size){
    void const * ptr = wgpuBufferGetConstMappedRange(buffer, offset, size);
    //printf("buffer const map range: %p\n", ptr);
    return ptr;
}

EXPORT void BufferUnmap(WGPUBuffer buffer){
    wgpuBufferUnmap(buffer);
}

EXPORT void BufferDestroy(WGPUBuffer buffer){
    wgpuBufferDestroy(buffer);
}

EXPORT  uint64_t BufferGetSize(WGPUBuffer buffer) {
    return wgpuBufferGetSize(buffer);
}

EXPORT void RenderPassEncoderSetVertexBuffer(WGPURenderPassEncoder renderPassEncoder, uint32_t slot, WGPU_NULLABLE WGPUBuffer buffer, uint64_t offset, uint64_t size){
    wgpuRenderPassEncoderSetVertexBuffer(renderPassEncoder, slot, buffer, offset, size);
}

EXPORT void RenderPassEncoderSetScissorRect(WGPURenderPassEncoder renderPassEncoder, uint32_t x, uint32_t y, uint32_t width, uint32_t height) {
    wgpuRenderPassEncoderSetScissorRect(renderPassEncoder, x, y, width, height);
}

EXPORT void RenderPassEncoderSetViewport(WGPURenderPassEncoder renderPassEncoder, float x, float y, float width, float height, float minDepth, float maxDepth) {
    wgpuRenderPassEncoderSetViewport(renderPassEncoder, x, y, width, height, minDepth, maxDepth);
}

EXPORT void BindGroupRelease(WGPUBindGroup bindGroup){
    wgpuBindGroupRelease(bindGroup);
}

EXPORT void BindGroupLayoutRelease(WGPUBindGroupLayout bindGroupLayout){
    wgpuBindGroupLayoutRelease(bindGroupLayout);
}


EXPORT void PipelineLayoutRelease(WGPUPipelineLayout layout){
    wgpuPipelineLayoutRelease(layout);
}

EXPORT WGPUBindGroupLayout DeviceCreateBindGroupLayout(WGPUDevice device, WGPUBindGroupLayoutDescriptor *bindGroupLayoutDesc){
    return wgpuDeviceCreateBindGroupLayout(device, bindGroupLayoutDesc);
}

EXPORT void RenderPassEncoderSetBindGroup(WGPURenderPassEncoder renderPassEncoder, uint32_t groupIndex, WGPU_NULLABLE WGPUBindGroup group, size_t dynamicOffsetCount, uint32_t const * dynamicOffsets) {
    wgpuRenderPassEncoderSetBindGroup( renderPassEncoder,  groupIndex,   group,  dynamicOffsetCount,  dynamicOffsets);
}

EXPORT WGPUBindGroup DeviceCreateBindGroup(WGPUDevice device, WGPUBindGroupDescriptor *bindGroupDesc){
    return wgpuDeviceCreateBindGroup(device, bindGroupDesc);
}

EXPORT WGPUPipelineLayout DeviceCreatePipelineLayout(WGPUDevice device, WGPUPipelineLayoutDescriptor const * descriptor){
    return wgpuDeviceCreatePipelineLayout(device, descriptor);
}


EXPORT WGPUTexture DeviceCreateTexture(WGPUDevice device, WGPUTextureDescriptor const * descriptor){
    return wgpuDeviceCreateTexture(device, descriptor);
}


EXPORT void TextureDestroy(WGPUTexture texture){
    wgpuTextureDestroy(texture);
}

EXPORT void TextureRelease(WGPUTexture texture){
    wgpuTextureRelease(texture);
}

EXPORT void QueueWriteTexture(WGPUQueue queue, WGPUImageCopyTexture const * destination, void const * data, size_t dataSize, WGPUTextureDataLayout const * dataLayout, WGPUExtent3D const  *writeSize){
    wgpuQueueWriteTexture(queue, destination, data, dataSize, dataLayout, writeSize);
}


EXPORT WGPUSampler DeviceCreateSampler(WGPUDevice device, WGPUSamplerDescriptor const *samplerDesc){
    return wgpuDeviceCreateSampler(device, samplerDesc);
}

EXPORT WGPUQuerySet CreateQuerySet(WGPUDevice device, WGPUQuerySetDescriptor const *desc){
    return wgpuDeviceCreateQuerySet(device, desc);
}

EXPORT void CommandEncoderResolveQuerySet(WGPUCommandEncoder commandEncoder, WGPUQuerySet querySet, uint32_t firstQuery, uint32_t queryCount, WGPUBuffer destination, uint64_t destinationOffset) {
    wgpuCommandEncoderResolveQuerySet(commandEncoder, querySet, firstQuery, queryCount, destination, destinationOffset);
}

#endif // LEAN

/**
 * Utility function to get a WebGPU adapter, so that
 *     WGPUAdapter adapter = requestAdapterSync(options);
 * is roughly equivalent to
 *     const adapter = await navigator.gpu.requestAdapter(options);
 */
EXPORT WGPUAdapter RequestAdapterSync(WGPUInstance instance, WGPURequestAdapterOptions const * options) {

//    WGPURequestAdapterOptions theOptions, *options;
//    options = &theOptions;
//    theOptions.nextInChain = nullptr;
//    theOptions.compatibleSurface = nullptr;
//    theOptions.powerPreference = WGPUPowerPreference_Undefined;
//    theOptions.backendType = WGPUBackendType_D3D12; // WGPUBackendType_D3D12; //WGPUBackendType_Vulkan;
//    theOptions.forceFallbackAdapter = false;
//    theOptions.compatibilityMode = false;

    // A simple structure holding the local information shared with the
    // onAdapterRequestEnded callback.
    struct UserData {
        WGPUAdapter adapter = nullptr;
        bool requestEnded = false;
    };
    UserData userData;
    LOG( printf("RequestAdapterSync\n"); )

    // Callback called by wgpuInstanceRequestAdapter when the request returns
    // This is a C++ lambda function, but could be any function defined in the
    // global scope. It must be non-capturing (the brackets [] are empty) so
    // that it behaves like a regular C function pointer, which is what
    // wgpuInstanceRequestAdapter expects (WebGPU being a C API). The workaround
    // is to convey what we want to capture through the pUserData pointer,
    // provided as the last argument of wgpuInstanceRequestAdapter and received
    // by the callback as its last argument.
    auto onAdapterRequestEnded = [](WGPURequestAdapterStatus status, WGPUAdapter adapter, char const * message, void * pUserData) {
        //std::cout << "onAdapterRequestEnded : " << status << std::endl;
        UserData& userData = *reinterpret_cast<UserData*>(pUserData);
        if (status == WGPURequestAdapterStatus_Success) {
            userData.adapter = adapter;
        } else {
            std::cout << "Could not get WebGPU adapter: " << message << std::endl;
        }
        userData.requestEnded = true;
    };

//    printf("backend: %d\n", options->backendType);
//    printf("power: %d\n", options->powerPreference);
//    printf("fallback: %d\n", options->forceFallbackAdapter);
//    printf("surface: %p\n", options->compatibleSurface);

    LOG( printf("wgpuInstanceRequestAdapter\n"); )

    // Call to the WebGPU request adapter procedure
    wgpuInstanceRequestAdapter(
        instance /* equivalent of navigator.gpu */,
        options,
        onAdapterRequestEnded,
        (void*)&userData
    );

    // We wait until userData.requestEnded gets true
    // [...] Wait for request to end

    LOG( printf("requested ended? %d\n", (int)userData.requestEnded); )
    LOG( printf("requested adapter %p\n", userData.adapter); )

    assert(userData.requestEnded);
    return userData.adapter;
}

/**
 * Utility function to get a WebGPU device, so that
 *     WGPUAdapter device = requestDeviceSync(adapter, options);
 * is roughly equivalent to
 *     const device = await adapter.requestDevice(descriptor);
 * It is very similar to requestAdapter
 */
EXPORT WGPUDevice RequestDeviceSync(WGPUAdapter adapter, WGPUDeviceDescriptor const * descriptor) {
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
    return userData.device;
}

EXPORT    WGPUSurface glfwGetWGPUSurface(WGPUInstance instance, void * hwnd){
            LOG( printf("getting surface from GLFW window %p (HWND)\n", hwnd); )
            LOG( printf("instance => %p\n", instance); )
            if(hwnd == nullptr)
                printf("** Window handle (HWND) is NULL!\n");
            HINSTANCE hinstance = GetModuleHandle(NULL);

            WGPUSurfaceDescriptorFromWindowsHWND fromWindowsHWND;
            fromWindowsHWND.chain.next = NULL;
            fromWindowsHWND.chain.sType = WGPUSType_SurfaceDescriptorFromWindowsHWND;
            fromWindowsHWND.hinstance = hinstance;
            fromWindowsHWND.hwnd = hwnd;

            WGPUSurfaceDescriptor surfaceDescriptor;
            surfaceDescriptor.nextInChain = &fromWindowsHWND.chain;
            surfaceDescriptor.label = NULL;

            WGPUSurface s = wgpuInstanceCreateSurface(instance, &surfaceDescriptor);
            LOG( printf("surface => %p\n", s); )
            return s;
    }
#ifdef __cplusplus
} // extern "C"
#endif