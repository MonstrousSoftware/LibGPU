package com.monstrous.graphics.g3d;

public class NodeKeyframe<T> {

    public float keyTime;
    public T value;

    public NodeKeyframe(final float t, final T v) {
        keyTime = t;
        value = v;
    }


}
