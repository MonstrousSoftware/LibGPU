package com.monstrous.utils;

import com.monstrous.LibGPU;
import com.monstrous.graphics.Color;

public class ScreenUtils {


    public static void clear(Color color){
        clear(color.r, color.g, color.b, color.a);
    }

    public static void clear(float r, float g, float b, float a){
        LibGPU.application.clearColor.set(r,g,b,a);
    }
}
