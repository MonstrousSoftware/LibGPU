package com.monstrous.utils;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.webgpu.RenderPassBuilder;

public class ScreenUtils {


    public static void clear(Color color){
        clear(color.r, color.g, color.b, color.a);
    }

    public static void clear(float r, float g, float b, float a){
        RenderPassBuilder.setClearColor(r,g,b,a);
    }
}
