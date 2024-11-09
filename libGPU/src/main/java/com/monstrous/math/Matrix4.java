package com.monstrous.math;

// heavily borrowed from LibGDX

public class Matrix4 {
    // logical view:
    // M00 M01 M02 M03
    // M10 M11 M12 M13
    // M20 M21 M22 M23
    // M30 M31 M32 M33
    //
    // translation vector in the matrix is M03, M13, M23
    public static final int M00 = 0;
    public static final int M01 = 4;
    public static final int M02 = 8;
    public static final int M03 = 12;
    public static final int M10 = 1;
    public static final int M11 = 5;
    public static final int M12 = 9;
    public static final int M13 = 13;
    public static final int M20 = 2;
    public static final int M21 = 6;
    public static final int M22 = 10;
    public static final int M23 = 14;
    public static final int M30 = 3;
    public static final int M31 = 7;
    public static final int M32 = 11;
    public static final int M33 = 15;

    public final float val[] = new float[16];

    /**
     * Constructs an identity matrix
     */
    public Matrix4() {
        val[M00] = 1f;
        val[M11] = 1f;
        val[M22] = 1f;
        val[M33] = 1f;
    }

    /** Constructs a matrix from the given matrix.
     * @param matrix The matrix to copy. (This matrix is not modified) */
    public Matrix4 (Matrix4 matrix) {
        set(matrix);
    }

    /** Constructs a matrix from the given float array. The array must have at least 16 elements; the first 16 will be copied.
     * @param values The float array to copy. Remember that this matrix is in
     *           <a href="http://en.wikipedia.org/wiki/Row-major_order">column major</a> order. (The float array is not
     *           modified) */
    public Matrix4 (float[] values) {
        set(values);
    }


    /** Sets the matrix to the given matrix.
     * @param matrix The matrix that is to be copied. (The given matrix is not modified)
     * @return This matrix for the purpose of chaining methods together. */
    public Matrix4 set (Matrix4 matrix) {
        return set(matrix.val);
    }


    public Matrix4 set (float[] values) {
        System.arraycopy(values, 0, val, 0, val.length);
        return this;
    }

    /**
     * Sets the matrix to an identity matrix.
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4 idt() {
        val[M00] = 1f;
        val[M01] = 0f;
        val[M02] = 0f;
        val[M03] = 0f;
        val[M10] = 0f;
        val[M11] = 1f;
        val[M12] = 0f;
        val[M13] = 0f;
        val[M20] = 0f;
        val[M21] = 0f;
        val[M22] = 1f;
        val[M23] = 0f;
        val[M30] = 0f;
        val[M31] = 0f;
        val[M32] = 0f;
        val[M33] = 1f;
        return this;
    }

    /**
     * Postmultiplies this matrix by a translation matrix. Postmultiplication is also used by OpenGL ES' 1.x
     * glTranslate/glRotate/glScale.
     *
     * @param x Translation in the x-axis.
     * @param y Translation in the y-axis.
     * @param z Translation in the z-axis.
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4 translate(float x, float y, float z) {
        val[M03] += val[M00] * x + val[M01] * y + val[M02] * z;
        val[M13] += val[M10] * x + val[M11] * y + val[M12] * z;
        val[M23] += val[M20] * x + val[M21] * y + val[M22] * z;
        val[M33] += val[M30] * x + val[M31] * y + val[M32] * z;
        return this;
    }


    public Matrix4 setToZRotation(float angleInRadians) {
        float c = (float) Math.cos(angleInRadians);
        float s = (float) Math.sin(angleInRadians);
        idt();
        val[M00] = c;
        val[M01] = -s;

        val[M10] = s;
        val[M11] = c;
        return this;
    }

    public Matrix4 setToXRotation(float angleInRadians) {
        float c = (float) Math.cos(angleInRadians);
        float s = (float) Math.sin(angleInRadians);
        idt();
        val[M11] = c;
        val[M12] = -s;
        val[M21] = s;
        val[M22] = c;
        return this;
    }

    public Matrix4 setToPerspective(float focalLength, float near, float far, float aspectRatio) {

        float divides = 1.0f/(focalLength*(far-near));
        idt();
        val[M00] = focalLength;
        val[M11] = focalLength * aspectRatio;
        val[M22] = far/(far-near);
        val[M23] = -far*near/(far-near);
        val[M32] = 1.0f;
        val[M33] = 0f;
        return this;
    }

    /** Sets the matrix to a projection matrix with a near- and far plane, a field of view in degrees and an aspect ratio. Note
     * that the field of view specified is the angle in degrees for the height, the field of view for the width will be calculated
     * according to the aspect ratio.
     * @param near The near plane
     * @param far The far plane
     * @param fovy The field of view of the height in degrees
     * @param aspectRatio The "width over height" aspect ratio
     * @return This matrix for the purpose of chaining methods together. */
    public Matrix4 setToProjection (float near, float far, float fovy, float aspectRatio) {
        idt();
        float l_fd = (float)(1.0 / Math.tan((fovy * (Math.PI / 180)) / 2.0));
        float l_a1 = (far + near) / (near - far);
        float l_a2 = (2 * far * near) / (near - far);
        val[M00] = l_fd / aspectRatio;
        val[M10] = 0;
        val[M20] = 0;
        val[M30] = 0;
        val[M01] = 0;
        val[M11] = l_fd;
        val[M21] = 0;
        val[M31] = 0;
        val[M02] = 0;
        val[M12] = 0;
        val[M22] = l_a1;
        val[M32] = -1;
        val[M03] = 0;
        val[M13] = 0;
        val[M23] = l_a2;
        val[M33] = 0;
        return this;
    }


    public Matrix4 scale (float scale) {
        return this.scale(scale, scale, scale);
    }


    /** Postmultiplies this matrix with a scale matrix. Postmultiplication is also used by OpenGL ES' 1.x
     * glTranslate/glRotate/glScale.
     * @param scaleX The scale in the x-axis.
     * @param scaleY The scale in the y-axis.
     * @param scaleZ The scale in the z-axis.
     * @return This matrix for the purpose of chaining methods together. */
    public Matrix4 scale (float scaleX, float scaleY, float scaleZ) {
        val[M00] *= scaleX;
        val[M01] *= scaleY;
        val[M02] *= scaleZ;
        val[M10] *= scaleX;
        val[M11] *= scaleY;
        val[M12] *= scaleZ;
        val[M20] *= scaleX;
        val[M21] *= scaleY;
        val[M22] *= scaleZ;
        val[M30] *= scaleX;
        val[M31] *= scaleY;
        val[M32] *= scaleZ;
        return this;
    }

    /** Transposes the matrix.
     * @return This matrix for the purpose of chaining methods together. */
    public Matrix4 tra () {
        float m01 = val[M01];
        float m02 = val[M02];
        float m03 = val[M03];
        float m12 = val[M12];
        float m13 = val[M13];
        float m23 = val[M23];
        val[M01] = val[M10];
        val[M02] = val[M20];
        val[M03] = val[M30];
        val[M10] = m01;
        val[M12] = val[M21];
        val[M13] = val[M31];
        val[M20] = m02;
        val[M21] = m12;
        val[M23] = val[M32];
        val[M30] = m03;
        val[M31] = m13;
        val[M32] = m23;
        return this;
    }
    /** Postmultiplies this matrix with the given matrix, storing the result in this matrix. For example:
     *
     * <pre>
     * A.mul(B) results in A := AB.
     * </pre>
     *
     * @param matrix The other matrix to multiply by.
     * @return This matrix for the purpose of chaining operations together. */
    public Matrix4 mul (Matrix4 matrix) {
        mul(val, matrix.val);
        return this;
    }

    /** Multiplies the matrix mata with matrix matb, storing the result in mata. The arrays are assumed to hold 4x4 column major
     * matrices as you can get from {@link Matrix4#val}. This is the same as {@link Matrix4#mul(Matrix4)}.
     *
     * @param mata the first matrix.
     * @param matb the second matrix. */
    public static void mul (float[] mata, float[] matb) {
        float m00 = mata[M00] * matb[M00] + mata[M01] * matb[M10] + mata[M02] * matb[M20] + mata[M03] * matb[M30];
        float m01 = mata[M00] * matb[M01] + mata[M01] * matb[M11] + mata[M02] * matb[M21] + mata[M03] * matb[M31];
        float m02 = mata[M00] * matb[M02] + mata[M01] * matb[M12] + mata[M02] * matb[M22] + mata[M03] * matb[M32];
        float m03 = mata[M00] * matb[M03] + mata[M01] * matb[M13] + mata[M02] * matb[M23] + mata[M03] * matb[M33];
        float m10 = mata[M10] * matb[M00] + mata[M11] * matb[M10] + mata[M12] * matb[M20] + mata[M13] * matb[M30];
        float m11 = mata[M10] * matb[M01] + mata[M11] * matb[M11] + mata[M12] * matb[M21] + mata[M13] * matb[M31];
        float m12 = mata[M10] * matb[M02] + mata[M11] * matb[M12] + mata[M12] * matb[M22] + mata[M13] * matb[M32];
        float m13 = mata[M10] * matb[M03] + mata[M11] * matb[M13] + mata[M12] * matb[M23] + mata[M13] * matb[M33];
        float m20 = mata[M20] * matb[M00] + mata[M21] * matb[M10] + mata[M22] * matb[M20] + mata[M23] * matb[M30];
        float m21 = mata[M20] * matb[M01] + mata[M21] * matb[M11] + mata[M22] * matb[M21] + mata[M23] * matb[M31];
        float m22 = mata[M20] * matb[M02] + mata[M21] * matb[M12] + mata[M22] * matb[M22] + mata[M23] * matb[M32];
        float m23 = mata[M20] * matb[M03] + mata[M21] * matb[M13] + mata[M22] * matb[M23] + mata[M23] * matb[M33];
        float m30 = mata[M30] * matb[M00] + mata[M31] * matb[M10] + mata[M32] * matb[M20] + mata[M33] * matb[M30];
        float m31 = mata[M30] * matb[M01] + mata[M31] * matb[M11] + mata[M32] * matb[M21] + mata[M33] * matb[M31];
        float m32 = mata[M30] * matb[M02] + mata[M31] * matb[M12] + mata[M32] * matb[M22] + mata[M33] * matb[M32];
        float m33 = mata[M30] * matb[M03] + mata[M31] * matb[M13] + mata[M32] * matb[M23] + mata[M33] * matb[M33];
        mata[M00] = m00;
        mata[M10] = m10;
        mata[M20] = m20;
        mata[M30] = m30;
        mata[M01] = m01;
        mata[M11] = m11;
        mata[M21] = m21;
        mata[M31] = m31;
        mata[M02] = m02;
        mata[M12] = m12;
        mata[M22] = m22;
        mata[M32] = m32;
        mata[M03] = m03;
        mata[M13] = m13;
        mata[M23] = m23;
        mata[M33] = m33;
    }

    public String toString () {
        return "[" + val[M00] + "|" + val[M01] + "|" + val[M02] + "|" + val[M03] + "]\n" //
                + "[" + val[M10] + "|" + val[M11] + "|" + val[M12] + "|" + val[M13] + "]\n" //
                + "[" + val[M20] + "|" + val[M21] + "|" + val[M22] + "|" + val[M23] + "]\n" //
                + "[" + val[M30] + "|" + val[M31] + "|" + val[M32] + "|" + val[M33] + "]\n";
    }

}
