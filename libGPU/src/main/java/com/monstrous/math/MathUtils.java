package com.monstrous.math;

public final class MathUtils {

    private MathUtils () {
    }

    static public final float nanoToSec = 1 / 1000000000f;

    // ---
    static public final float FLOAT_ROUNDING_ERROR = 0.000001f; // 32 bits
    static public final float PI = (float)Math.PI;
    static public final float PI2 = PI * 2;
    static public final float HALF_PI = PI / 2;

    static public final float E = (float)Math.E;

    static private final float radFull = PI2;
    static private final float degFull = 360;

    /** multiply by this to convert from radians to degrees */
    static public final float radiansToDegrees = 180f / PI;
    static public final float radDeg = radiansToDegrees;
    /** multiply by this to convert from degrees to radians */
    static public final float degreesToRadians = PI / 180;
    static public final float degRad = degreesToRadians;

    /** Returns true if a is nearly equal to b. The function uses the default floating error tolerance.
     * @param a the first value.
     * @param b the second value. */
    static public boolean isEqual (float a, float b) {
        return Math.abs(a - b) <= FLOAT_ROUNDING_ERROR;
    }

    /** Returns true if the value is zero (using the default tolerance as upper bound) */
    static public boolean isZero (float value) {
        return Math.abs(value) <= FLOAT_ROUNDING_ERROR;
    }

    /** Returns true if the value is zero.
     * @param tolerance represent an upper bound below which the value is considered zero. */
    static public boolean isZero (float value, float tolerance) {
        return Math.abs(value) <= tolerance;
    }

}
