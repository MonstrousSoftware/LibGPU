package com.monstrous.graphics.webgpu;

public enum RenderPassType {
    SHADOW_PASS,
    DEPTH_PREPASS,
    COLOR_PASS,
    COLOR_PASS_AFTER_DEPTH_PREPASS,
    NO_DEPTH /* use this only when output is not to the screen */
}
