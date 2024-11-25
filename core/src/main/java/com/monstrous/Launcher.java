package com.monstrous;// com.monstrous.Launcher

import com.monstrous.wgpu.WGPUBackendType;

public class Launcher {


    public static void main(String[] args) {
        System.out.println("Hello, world!");
        ApplicationConfiguration config = new ApplicationConfiguration();

        config.setSize(800, 600);
        config.title = "My WebGPU application";
        config.vsyncEnabled = false;
        config.backend = WGPUBackendType.Vulkan;

        new Application(new TestInstancing(), config);
    }
}
