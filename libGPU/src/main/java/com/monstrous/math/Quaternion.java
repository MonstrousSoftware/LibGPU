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

    public Quaternion set (Vector3 axis, float angle) {
        return setFromAxis(axis.x, axis.y, axis.z, angle);
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

    /**
     * <p>
     * Sets the Quaternion from the given x-, y- and z-axis which have to be orthonormal.
     * </p>
     *
     * <p>
     * Taken from Bones framework for JPCT, see http://www.aptalkarga.com/bones/ which in turn took it from Graphics Gem code at
     * ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z.
     * </p>
     *
     * @param xx x-axis x-coordinate
     * @param xy x-axis y-coordinate
     * @param xz x-axis z-coordinate
     * @param yx y-axis x-coordinate
     * @param yy y-axis y-coordinate
     * @param yz y-axis z-coordinate
     * @param zx z-axis x-coordinate
     * @param zy z-axis y-coordinate
     * @param zz z-axis z-coordinate */
    public Quaternion setFromAxes (float xx, float xy, float xz, float yx, float yy, float yz, float zx, float zy, float zz) {
        return setFromAxes(false, xx, xy, xz, yx, yy, yz, zx, zy, zz);
    }
    /**
     * <p>
     * Sets the Quaternion from the given x-, y- and z-axis.
     * </p>
     *
     * <p>
     * Taken from Bones framework for JPCT, see http://www.aptalkarga.com/bones/ which in turn took it from Graphics Gem code at
     * ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z.
     * </p>
     *
     * @param normalizeAxes whether to normalize the axes (necessary when they contain scaling)
     * @param xx x-axis x-coordinate
     * @param xy x-axis y-coordinate
     * @param xz x-axis z-coordinate
     * @param yx y-axis x-coordinate
     * @param yy y-axis y-coordinate
     * @param yz y-axis z-coordinate
     * @param zx z-axis x-coordinate
     * @param zy z-axis y-coordinate
     * @param zz z-axis z-coordinate */
    public Quaternion setFromAxes (boolean normalizeAxes, float xx, float xy, float xz, float yx, float yy, float yz, float zx,
                                   float zy, float zz) {
        if (normalizeAxes) {
            final float lx = 1f / Vector3.len(xx, xy, xz);
            final float ly = 1f / Vector3.len(yx, yy, yz);
            final float lz = 1f / Vector3.len(zx, zy, zz);
            xx *= lx;
            xy *= lx;
            xz *= lx;
            yx *= ly;
            yy *= ly;
            yz *= ly;
            zx *= lz;
            zy *= lz;
            zz *= lz;
        }
        // the trace is the sum of the diagonal elements; see
        // http://mathworld.wolfram.com/MatrixTrace.html
        final float t = xx + yy + zz;

        // we protect the division by s by ensuring that s>=1
        if (t >= 0) { // |w| >= .5
            float s = (float)Math.sqrt(t + 1); // |s|>=1 ...
            w = 0.5f * s;
            s = 0.5f / s; // so this division isn't bad
            x = (zy - yz) * s;
            y = (xz - zx) * s;
            z = (yx - xy) * s;
        } else if ((xx > yy) && (xx > zz)) {
            float s = (float)Math.sqrt(1.0 + xx - yy - zz); // |s|>=1
            x = s * 0.5f; // |x| >= .5
            s = 0.5f / s;
            y = (yx + xy) * s;
            z = (xz + zx) * s;
            w = (zy - yz) * s;
        } else if (yy > zz) {
            float s = (float)Math.sqrt(1.0 + yy - xx - zz); // |s|>=1
            y = s * 0.5f; // |y| >= .5
            s = 0.5f / s;
            x = (yx + xy) * s;
            z = (zy + yz) * s;
            w = (xz - zx) * s;
        } else {
            float s = (float)Math.sqrt(1.0 + zz - xx - yy); // |s|>=1
            z = s * 0.5f; // |z| >= .5
            s = 0.5f / s;
            x = (xz + zx) * s;
            y = (zy + yz) * s;
            w = (yx - xy) * s;
        }

        return this;
    }

    /** Sets the Quaternion from the given matrix, optionally removing any scaling. */
    public Quaternion setFromMatrix (boolean normalizeAxes, Matrix4 matrix) {
        return setFromAxes(normalizeAxes, matrix.val[Matrix4.M00], matrix.val[Matrix4.M01], matrix.val[Matrix4.M02],
                matrix.val[Matrix4.M10], matrix.val[Matrix4.M11], matrix.val[Matrix4.M12], matrix.val[Matrix4.M20],
                matrix.val[Matrix4.M21], matrix.val[Matrix4.M22]);
    }

    /** Sets the Quaternion from the given rotation matrix, which must not contain scaling. */
    public Quaternion setFromMatrix (Matrix4 matrix) {
        return setFromMatrix(false, matrix);
    }

    @Override
    public String toString () {
        return "(" + x + "," + y + "," + z + "," + w + ")";
    }


    /** Spherical linear interpolation between this quaternion and the other quaternion, based on the alpha value in the range
     * [0,1]. Taken from Bones framework for JPCT, see http://www.aptalkarga.com/bones/
     * @param end the end quaternion
     * @param alpha alpha in the range [0,1]
     * @return this quaternion for chaining */
    public Quaternion slerp (Quaternion end, float alpha) {
        float d = this.x * end.x + this.y * end.y + this.z * end.z + this.w * end.w;
        float absDot = d < 0.f ? -d : d;

        // Set the first and second scale for the interpolation
        float scale0 = 1f - alpha;
        float scale1 = alpha;

        // Check if the angle between the 2 quaternions was big enough to
        // warrant such calculations
        if ((1 - absDot) > 0.1) {// Get the angle between the 2 quaternions,
            // and then store the sin() of that angle
            final float angle = (float)Math.acos(absDot);
            final float invSinTheta = 1f / (float)Math.sin(angle);

            // Calculate the scale for q1 and q2, according to the angle and
            // it's sine value
            scale0 = ((float)Math.sin((1f - alpha) * angle) * invSinTheta);
            scale1 = ((float)Math.sin((alpha * angle)) * invSinTheta);
        }

        if (d < 0.f) scale1 = -scale1;

        // Calculate the x, y, z and w values for the quaternion by using a
        // special form of linear interpolation for quaternions.
        x = (scale0 * x) + (scale1 * end.x);
        y = (scale0 * y) + (scale1 * end.y);
        z = (scale0 * z) + (scale1 * end.z);
        w = (scale0 * w) + (scale1 * end.w);

        // Return the interpolated quaternion
        return this;
    }

}
