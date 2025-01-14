package com.monstrous.utils;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.webgpu.RenderPass;

public class ScreenUtils {


    public static void clear(Color color){
        clear(color.r, color.g, color.b, color.a);
    }

    public static void clear(float r, float g, float b, float a){
        RenderPass.setClearColor(r,g,b,a);
    }
}
