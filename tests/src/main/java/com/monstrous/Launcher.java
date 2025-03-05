package com.monstrous;// com.monstrous.Launcher

import com.monstrous.webgpu.WGPUBackendType;

public class Launcher {


    public static void main(String[] args) {
        System.out.println("Hello, world!");
        ApplicationConfiguration config = new ApplicationConfiguration();

        config.setSize(1200, 800);
        config.title = "My WebGPU application";
        config.vsyncEnabled = false;
        config.backend = WGPUBackendType.Undefined;
        config.numSamples = 1;      // MSAA samples: can be 1 (no MSAA) or 4 (multi-sampling)


        //config.noWindow = true;   // use this to run a program without a window

        config.enableGPUtiming = true;

        new Application(new TestSpriteBatchPerformance(), config);

    }
}
