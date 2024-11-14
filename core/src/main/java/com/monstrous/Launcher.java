package com.monstrous;// com.monstrous.Launcher

public class Launcher {


    public static void main(String[] args) {
        System.out.println("Hello, world!");
        ApplicationConfiguration config = new ApplicationConfiguration();

        config.setSize(1200, 600);
        config.title = "My WebGPU application";
        config.vsyncEnabled = false;

        new Application(new com.monstrous.Demo(), config);
    }
}
