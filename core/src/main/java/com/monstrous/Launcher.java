package com.monstrous;// com.monstrous.Launcher

import com.monstrous.wgpu.WGPUBackendType;

public class Launcher {


    public static void main(String[] args) {
        System.out.println("Hello, world!");
        ApplicationConfiguration config = new ApplicationConfiguration();

        config.setSize(1200, 800);
        config.title = "My WebGPU application";
        config.vsyncEnabled = false;
        config.backend = WGPUBackendType.D3D12;

        // don't use. will cause crash
        //config.enableGPUtiming = true;

        new Application(new TestShadow(), config);

    }
}
