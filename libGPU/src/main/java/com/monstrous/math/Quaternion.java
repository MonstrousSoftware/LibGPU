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

    public Quaternion set (Quaternion quaternion){
        return this.set(quaternion.x, quaternion.y, quaternion.z, quaternion.w);
    }

    /** Sets the quaternion to an identity Quaternion
     * @return this quaternion for chaining */
    public Quaternion idt () {
        return this.set(0, 0, 0, 1);
    }

    /** Sets the quaternion components from the given axis and angle around that axis.
     * @param x X direction of the axis
     * @param y Y direction of the axis
     * @param z Z direction of the axis
     * @param degrees The angle in degrees
     * @return This quaternion for chaining. */
    public Quaternion setFromAxis (final float x, final float y, final float z, final float degrees) {
        return setFromAxisRad(x, y, z, degrees * MathUtils.degreesToRadians);
    }

    /** Sets the quaternion components from the given axis and angle around that axis.
     * @param x X direction of the axis
     * @param y Y direction of the axis
     * @param z Z direction of the axis
     * @param radians The angle in radians
     * @return This quaternion for chaining. */
    public Quaternion setFromAxisRad (final float x, final float y, final float z, final float radians) {
        float d = Vector3.len(x, y, z);
        if (d == 0f) return idt();
        d = 1f / d;
        float l_ang = radians < 0 ? MathUtils.PI2 - (-radians % MathUtils.PI2) : radians % MathUtils.PI2;
        float l_sin = (float)Math.sin(l_ang / 2);
        float l_cos = (float)Math.cos(l_ang / 2);
        return this.set(d * x * l_sin, d * y * l_sin, d * z * l_sin, l_cos).nor();
    }
    public final static float len2 (final float x, final float y, final float z, final float w) {
        return x * x + y * y + z * z + w * w;
    }

    /** @return the length of this quaternion without square root */
    public float len2 () {
        return x * x + y * y + z * z + w * w;
    }

    /** Normalizes this quaternion to unit length
     * @return the quaternion for chaining */
    public Quaternion nor () {
        float len = len2();
        if (len != 0.f && !MathUtils.isEqual(len, 1f)) {
            len = (float)Math.sqrt(len);
            w /= len;
            x /= len;
            y /= len;
            z /= len;
        }
        return this;
    }

    @Override
    public String toString () {
        return "(" + x + "," + y + "," + z + "," + w + ")";
    }

}
