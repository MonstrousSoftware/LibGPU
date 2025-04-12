package com.monstrous.graphics.webgpu;

import com.monstrous.LibGPU;
import com.monstrous.utils.Disposable;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.webgpu.*;
import jnr.ffi.Pointer;

public class Adapter implements Disposable {
    private Pointer adapter;

    public Adapter(Pointer instance, Pointer surface) {

        WGPURequestAdapterOptions options = WGPURequestAdapterOptions.createDirect();
        options.setNextInChain();
        options.setCompatibleSurface(surface);
        options.setBackendType(LibGPU.app.configuration.backend);
        options.setPowerPreference(WGPUPowerPreference.HighPerformance);

        if(LibGPU.app.configuration.backend == WGPUBackendType.Null)
            throw new IllegalStateException("Request Adapter: Back end 'Null' only valid if config.noWindow is true");

        // Get Adapter
        adapter = getAdapterSync(instance, options);
        if(adapter == null){
            System.out.println("Configured adapter back end ("+LibGPU.app.configuration.backend+") not available, requesting fallback");
            options.setBackendType(WGPUBackendType.Undefined);
            options.setPowerPreference(WGPUPowerPreference.HighPerformance);
            adapter = getAdapterSync(instance, options);
        }


        LibGPU.supportedLimits = WGPUSupportedLimits.createDirect();
        WGPUSupportedLimits supportedLimits = LibGPU.supportedLimits;
        LibGPU.webGPU.wgpuAdapterGetLimits(adapter, supportedLimits);
//        System.out.println("adapter maxVertexAttributes " + supportedLimits.getLimits().getMaxVertexAttributes());
//        System.out.println("adapter maxBindGroups " + supportedLimits.getLimits().getMaxBindGroups());
//
//        System.out.println("maxTextureDimension1D " + supportedLimits.getLimits().getMaxTextureDimension1D());
//        System.out.println("maxTextureDimension2D " + supportedLimits.getLimits().getMaxTextureDimension2D());
//        System.out.println("maxTextureDimension3D " + supportedLimits.getLimits().getMaxTextureDimension3D());
//        System.out.println("maxTextureArrayLayers " + supportedLimits.getLimits().getMaxTextureArrayLayers());


        WGPUAdapterProperties adapterProperties = WGPUAdapterProperties.createDirect();
        adapterProperties.setNextInChain();

        LibGPU.webGPU.wgpuAdapterGetProperties(adapter, adapterProperties);
        System.out.println("VendorID: " + adapterProperties.getVendorID());
        System.out.println("Vendor name: " + adapterProperties.getVendorName());
        System.out.println("Device ID: " + adapterProperties.getDeviceID());
        System.out.println("Back end: " + adapterProperties.getBackendType());
        System.out.println("Description: " + adapterProperties.getDriverDescription());
    }

    public Pointer getHandle(){
        return adapter;
    }

    @Override
    public void dispose() {
        LibGPU.webGPU.wgpuAdapterRelease(adapter);       // we can release our adapter as soon as we have a device

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
        LibGPU.webGPU.wgpuInstanceRequestAdapter(instance, options, callback, userBuf);
        // on native implementations, we don't have to wait for asynchronous operation. It returns result immediately.
        return  userBuf.getPointer(0);
    }
}
