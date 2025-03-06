package com.monstrous.utils;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.g2d.SpriteBatch;

public class ScreenUtils {
    private static final SpriteBatch batch = new SpriteBatch();


    public static void clear(Color color){
        batch.begin(color);
        batch.end();
    }

    public static void clear(float r, float g, float b, float a){
        clear(new Color(r,g,b,a));
    }
}
