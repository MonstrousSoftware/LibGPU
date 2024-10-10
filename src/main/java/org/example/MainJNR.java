package org.example;

import jnr.ffi.LibraryLoader;

public class MainJNR {

    public interface LibC { // A representation of libC in Java
        //int puts(String s); // mapping of the puts function, in C `int puts(const char *s);`
        long WGPUCreateInstance();
        void WGPUInstanceRelease(long instance);
        int add(int a, int b);
    }

    public static void main(String[] args) {
        LibC libc = LibraryLoader.create(LibC.class).load("native"); // load the library into the libc variable


        System.out.println("Hello world!");
        int sum = libc.add(1200, 34);
        System.out.println("sum = "+sum);

        long instance = libc.WGPUCreateInstance();

        libc.WGPUInstanceRelease(instance);

    }
}
