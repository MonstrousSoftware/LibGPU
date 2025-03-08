package com.monstrous.jlay.utils;

public class Align {
        static public final int CENTER = 1 << 0;
        static public final int TOP = 1 << 1;
        static public final int BOTTOM = 1 << 2;
        static public final int LEFT = 1 << 3;
        static public final int RIGHT = 1 << 4;

        static public final int TOP_LEFT = TOP | LEFT;
        static public final int TOP_RIGHT = TOP | RIGHT;
        static public final int BOTTOM_LEFT = BOTTOM | LEFT;
        static public final int BOTTOM_RIGHT = BOTTOM | RIGHT;
}
