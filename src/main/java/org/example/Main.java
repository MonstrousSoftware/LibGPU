package org.example;

// To generate header file use the following from the terminal (from source directory)
// javac -h ../../../c Main.java


public class Main {
    static {
        System.loadLibrary("native");
    }

    public static void main(String[] args) {

        System.out.println("Hello world!");
        int sum = add(1200, 34);
        System.out.println("sum = "+sum);

        long instance = WGPUCreateInstance();

        WGPUInstanceRelease(instance);

    }


    public static native int add(int a, int b);

    public static native long WGPUCreateInstance();

    public static native void WGPUInstanceRelease(long instance);
}