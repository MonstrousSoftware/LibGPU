package com.monstrous;

// Launcher

public class Main {


    public static void main(String[] args) {
        ApplicationConfiguration config = new ApplicationConfiguration();

        config.setSize(1200,600);
        config.title = "My WebGPU application";

        new Application(new Demo(), config);
    }
}
