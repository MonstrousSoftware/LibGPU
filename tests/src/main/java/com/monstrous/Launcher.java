package com.monstrous;// com.monstrous.Launcher

import com.monstrous.webgpu.WGPUBackendType;

public class Launcher {

    public static void main(String[] args) {

        ApplicationConfiguration config = new ApplicationConfiguration();

        config.setSize(1200, 800);
        config.title = "My WebGPU application";
        config.vsyncEnabled = false;
        config.backend = WGPUBackendType.Vulkan;
        config.numSamples = 1;      // MSAA samples: can be 1 (no MSAA) or 4 (multi-sampling)
        //config.noWindow = true;   // use this to run a program without a window

        config.enableGPUtiming = false;

        new Application(new Menu(), config);

    }
}
