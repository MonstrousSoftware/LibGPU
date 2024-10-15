package org.example.wgpu;

import org.example.WgpuJavaStruct;

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


//    public WGPULimits(Runtime runtime) {
//        super(runtime);
//    }
//
//    public WGPULimits() {
//        super(runtime);
//    }
}
