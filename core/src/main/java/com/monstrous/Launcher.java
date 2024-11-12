package com.monstrous;// com.monstrous.Launcher

public class Launcher {


    public static void main(String[] args) {
        System.out.println("Hello, world!");
        ApplicationConfiguration config = new ApplicationConfiguration();

        config.setSize(640, 480);
        config.title = "My WebGPU application";

        new Application(new com.monstrous.Demo(), config);
    }
}
