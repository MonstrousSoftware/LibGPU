package com.monstrous.math;

public class Quaternion {
    public float x;
    public float y;
    public float z;
    public float w;

    /** Constructor, sets the four components of the quaternion.
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     * @param w The w-component */
    public Quaternion (float x, float y, float z, float w) {
        this.set(x, y, z, w);
    }

    public Quaternion () {
        idt();
    }


    /** Sets the components of the quaternion
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     * @param w The w-component
     * @return This quaternion for chaining */
    public Quaternion set (float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    /** Sets the quaternion to an identity Quaternion
     * @return this quaternion for chaining */
    public Quaternion idt () {
        return this.set(0, 0, 0, 1);
    }

}
