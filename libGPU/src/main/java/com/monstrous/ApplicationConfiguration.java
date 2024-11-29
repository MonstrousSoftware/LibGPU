package com.monstrous;

import com.monstrous.wgpu.WGPUBackendType;

public class ApplicationConfiguration {
    public int width;
    public int height;
    public String title;
    public boolean vsyncEnabled;
    public WGPUBackendType backend;
    public boolean enableGPUtiming;     // enable for GPU performance measurements

    public ApplicationConfiguration() {
        // set to defaults
        width = 640;
        height = 480;
        title = "Application";
        vsyncEnabled = true;
        backend = WGPUBackendType.D3D12;
        enableGPUtiming = false;
    }

    public void setSize(int w, int h){
        this.width = w;
        this.height = h;
    }
}
