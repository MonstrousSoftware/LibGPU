package com.monstrous;

public class Main {
    private Application application;
    private Demo demo;


    public static void main(String[] args) {
       new Main().run();
    }

    public void run(){
        application = new Application();
        demo = new Demo();

        application.init(demo);             // need to think how best to separate the framework from the app
        application.loop(demo);
        application.exit(demo);
    }
}
