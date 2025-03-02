package com.monstrous;

import com.monstrous.webgpu.WGPUBackendType;

public class ApplicationConfiguration {
    public int width;
    public int height;
    public String title;
    public boolean vsyncEnabled;
    public int numSamples;
    public WGPUBackendType backend;
    public boolean enableGPUtiming;     // enable for GPU performance measurements
    public boolean noWindow;    // run without a window, e.g. for a compute shader app

    public ApplicationConfiguration() {
        // set to defaults
        width = 640;
        height = 480;
        title = "Application";
        vsyncEnabled = true;
        numSamples = 1;
        backend = WGPUBackendType.D3D12;
        enableGPUtiming = false;
        noWindow = false;
    }

    public void setSize(int w, int h){
        this.width = w;
        this.height = h;
    }
}
