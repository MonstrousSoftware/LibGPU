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
    static final Vector3 l_vez = new Vector3();
    static final Vector3 l_vex = new Vector3();
    static final Vector3 l_vey = new Vector3();
    static final Vector3 tmpVec = new Vector3();
    static final Matrix4 tmpMat = new Matrix4();
    static final Quaternion quat = new Quaternion();



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

    public Matrix4 translate(Vector3 translation) {
        return translate(translation.x, translation.y, translation.z);
    }

    public Vector3 getTranslation (Vector3 position) {
        position.x = val[M03];
        position.y = val[M13];
        position.z = val[M23];
        return position;
    }

    public Matrix4 rotate (Vector3 axis, float degrees) {
        if (degrees == 0) return this;
        quat.set(axis, degrees);
        return rotate(quat);
    }

    public Matrix4 rotate (Quaternion rotation) {
        float x = rotation.x, y = rotation.y, z = rotation.z, w = rotation.w;
        float xx = x * x;
        float xy = x * y;
        float xz = x * z;
        float xw = x * w;
        float yy = y * y;
        float yz = y * z;
        float yw = y * w;
        float zz = z * z;
        float zw = z * w;
        // Set matrix from quaternion
        float r00 = 1 - 2 * (yy + zz);
        float r01 = 2 * (xy - zw);
        float r02 = 2 * (xz + yw);
        float r10 = 2 * (xy + zw);
        float r11 = 1 - 2 * (xx + zz);
        float r12 = 2 * (yz - xw);
        float r20 = 2 * (xz - yw);
        float r21 = 2 * (yz + xw);
        float r22 = 1 - 2 * (xx + yy);
        float m00 = val[M00] * r00 + val[M01] * r10 + val[M02] * r20;
        float m01 = val[M00] * r01 + val[M01] * r11 + val[M02] * r21;
        float m02 = val[M00] * r02 + val[M01] * r12 + val[M02] * r22;
        float m10 = val[M10] * r00 + val[M11] * r10 + val[M12] * r20;
        float m11 = val[M10] * r01 + val[M11] * r11 + val[M12] * r21;
        float m12 = val[M10] * r02 + val[M11] * r12 + val[M12] * r22;
        float m20 = val[M20] * r00 + val[M21] * r10 + val[M22] * r20;
        float m21 = val[M20] * r01 + val[M21] * r11 + val[M22] * r21;
        float m22 = val[M20] * r02 + val[M21] * r12 + val[M22] * r22;
        float m30 = val[M30] * r00 + val[M31] * r10 + val[M32] * r20;
        float m31 = val[M30] * r01 + val[M31] * r11 + val[M32] * r21;
        float m32 = val[M30] * r02 + val[M31] * r12 + val[M32] * r22;
        val[M00] = m00;
        val[M10] = m10;
        val[M20] = m20;
        val[M30] = m30;
        val[M01] = m01;
        val[M11] = m11;
        val[M21] = m21;
        val[M31] = m31;
        val[M02] = m02;
        val[M12] = m12;
        val[M22] = m22;
        val[M32] = m32;
        return this;
    }

    /** Gets the rotation of this matrix.
     * @param rotation The {@link Quaternion} to receive the rotation
     * @return The provided {@link Quaternion} for chaining. */
    public Quaternion getRotation (Quaternion rotation) {
        return rotation.setFromMatrix(this);
    }


    /** @return the squared scale factor on the X axis */
    public float getScaleXSquared () {
        return val[M00] * val[M00] + val[M01] * val[M01] + val[M02] * val[M02];
    }

    /** @return the squared scale factor on the Y axis */
    public float getScaleYSquared () {
        return val[M10] * val[M10] + val[M11] * val[M11] + val[M12] * val[M12];
    }

    /** @return the squared scale factor on the Z axis */
    public float getScaleZSquared () {
        return val[M20] * val[M20] + val[M21] * val[M21] + val[M22] * val[M22];
    }

    /** @return the scale factor on the X axis (non-negative) */
    public float getScaleX () {
        return (MathUtils.isZero(val[M01]) && MathUtils.isZero(val[M02])) ? Math.abs(val[M00])
                : (float)Math.sqrt(getScaleXSquared());
    }

    /** @return the scale factor on the Y axis (non-negative) */
    public float getScaleY () {
        return (MathUtils.isZero(val[M10]) && MathUtils.isZero(val[M12])) ? Math.abs(val[M11])
                : (float)Math.sqrt(getScaleYSquared());
    }

    /** @return the scale factor on the X axis (non-negative) */
    public float getScaleZ () {
        return (MathUtils.isZero(val[M20]) && MathUtils.isZero(val[M21])) ? Math.abs(val[M22])
                : (float)Math.sqrt(getScaleZSquared());
    }

    /** @param scale The vector which will receive the (non-negative) scale components on each axis.
     * @return The provided vector for chaining. */
    public Vector3 getScale (Vector3 scale) {
        return scale.set(getScaleX(), getScaleY(), getScaleZ());
    }


    /** Sets the matrix to a rotation matrix representing the translation and quaternion.
     * @param translationX The X component of the translation that is to be used to set this matrix.
     * @param translationY The Y component of the translation that is to be used to set this matrix.
     * @param translationZ The Z component of the translation that is to be used to set this matrix.
     * @param quaternionX The X component of the quaternion that is to be used to set this matrix.
     * @param quaternionY The Y component of the quaternion that is to be used to set this matrix.
     * @param quaternionZ The Z component of the quaternion that is to be used to set this matrix.
     * @param quaternionW The W component of the quaternion that is to be used to set this matrix.
     * @return This matrix for the purpose of chaining methods together. */
    public Matrix4 set (float translationX, float translationY, float translationZ, float quaternionX, float quaternionY,
                        float quaternionZ, float quaternionW) {
        final float xs = quaternionX * 2f, ys = quaternionY * 2f, zs = quaternionZ * 2f;
        final float wx = quaternionW * xs, wy = quaternionW * ys, wz = quaternionW * zs;
        final float xx = quaternionX * xs, xy = quaternionX * ys, xz = quaternionX * zs;
        final float yy = quaternionY * ys, yz = quaternionY * zs, zz = quaternionZ * zs;

        val[M00] = 1f - (yy + zz);
        val[M01] = xy - wz;
        val[M02] = xz + wy;
        val[M03] = translationX;

        val[M10] = xy + wz;
        val[M11] = 1f - (xx + zz);
        val[M12] = yz - wx;
        val[M13] = translationY;

        val[M20] = xz - wy;
        val[M21] = yz + wx;
        val[M22] = 1f - (xx + yy);
        val[M23] = translationZ;

        val[M30] = 0f;
        val[M31] = 0f;
        val[M32] = 0f;
        val[M33] = 1f;
        return this;
    }

    public Matrix4 set (Vector3 translation, Quaternion quaternion, Vector3 scale){
        return this.set(translation.x, translation.y, translation.z, quaternion.x, quaternion.y, quaternion.z, quaternion.w, scale.x, scale.y, scale.z);
    }

    /** Sets the matrix to a rotation matrix representing the translation and quaternion.
     * @param translationX The X component of the translation that is to be used to set this matrix.
     * @param translationY The Y component of the translation that is to be used to set this matrix.
     * @param translationZ The Z component of the translation that is to be used to set this matrix.
     * @param quaternionX The X component of the quaternion that is to be used to set this matrix.
     * @param quaternionY The Y component of the quaternion that is to be used to set this matrix.
     * @param quaternionZ The Z component of the quaternion that is to be used to set this matrix.
     * @param quaternionW The W component of the quaternion that is to be used to set this matrix.
     * @param scaleX The X component of the scaling that is to be used to set this matrix.
     * @param scaleY The Y component of the scaling that is to be used to set this matrix.
     * @param scaleZ The Z component of the scaling that is to be used to set this matrix.
     * @return This matrix for the purpose of chaining methods together. */
    public Matrix4 set (float translationX, float translationY, float translationZ, float quaternionX, float quaternionY,
                        float quaternionZ, float quaternionW, float scaleX, float scaleY, float scaleZ) {
        final float xs = quaternionX * 2f, ys = quaternionY * 2f, zs = quaternionZ * 2f;
        final float wx = quaternionW * xs, wy = quaternionW * ys, wz = quaternionW * zs;
        final float xx = quaternionX * xs, xy = quaternionX * ys, xz = quaternionX * zs;
        final float yy = quaternionY * ys, yz = quaternionY * zs, zz = quaternionZ * zs;

        val[M00] = scaleX * (1.0f - (yy + zz));
        val[M01] = scaleY * (xy - wz);
        val[M02] = scaleZ * (xz + wy);
        val[M03] = translationX;

        val[M10] = scaleX * (xy + wz);
        val[M11] = scaleY * (1.0f - (xx + zz));
        val[M12] = scaleZ * (yz - wx);
        val[M13] = translationY;

        val[M20] = scaleX * (xz - wy);
        val[M21] = scaleY * (yz + wx);
        val[M22] = scaleZ * (1.0f - (xx + yy));
        val[M23] = translationZ;

        val[M30] = 0f;
        val[M31] = 0f;
        val[M32] = 0f;
        val[M33] = 1f;
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

    public Matrix4 setToYRotation(float angleInRadians) {
        float c = (float) Math.cos(angleInRadians);
        float s = (float) Math.sin(angleInRadians);
        idt();
        val[M00] = c;
        val[M02] = -s;
        val[M20] = s;
        val[M22] = c;
        return this;
    }

    // the clip volume’s Z range is (0,1).
    //. By default, it assumes that it is (-1, 1)
    // because this is the convention that was used by OpenGL, which is different from WebGPU.
    // todo which prob explain the difference with setToProjection

    public Matrix4 setToPerspective(float focalLength, float near, float far, float aspectRatio) {

        //float divides = 1.0f/(focalLength*(far-near));
        idt();
        val[M00] = focalLength;
        val[M11] = focalLength * aspectRatio;
        val[M22] = far/(far-near);
        val[M23] = -far*near/(far-near);
        val[M32] = 1.0f;
        val[M33] = 0f;
        return this;
    }

    /** Set to projection matrix.
     *
     * @param near
     * @param far
     * @param fovy  field of view in height (degrees)
     * @param aspectRatio width over height
     * @return
     */
    public Matrix4 setToProjection (float near, float far, float fovy, float aspectRatio) {

        float focalLength = (float)(1.0/Math.tan( 0.5f*fovy*(Math.PI/180f)));
        idt();
        val[M00] = focalLength/aspectRatio;
        val[M11] = focalLength;
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
    public Matrix4 setToProjectionOri (float near, float far, float fovy, float aspectRatio) {
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


    public Matrix4 setToOrtho2D(float x, float y, float width, float height){
        setToOrtho(x, x+width, y, y+height, -1f, 1f);
        return this;
    }


    // note for WebGPU we cannot use the same as for OpenGL because Z goes from 0 to 1
    // https://math.hws.edu/graphicsbook/source/webgpu/wgpu-matrix.js


    /** Sets the matrix to an orthographic projection like glOrtho (http://www.opengl.org/sdk/docs/man/xhtml/glOrtho.xml) following
     * the OpenGL equivalent
     * @param left The left clipping plane
     * @param right The right clipping plane
     * @param bottom The bottom clipping plane
     * @param top The top clipping plane
     * @param near The near clipping plane
     * @param far The far clipping plane
     * @return This matrix for the purpose of chaining methods together. */
    public Matrix4 setToOrtho (float left, float right, float bottom, float top, float near, float far) {
        float x_orth = 2 / (right - left);
        float y_orth = 2 / (top - bottom);
        float z_orth = -1 / (near - far);           // note the - sign

        float tx = -(right + left) / (right - left);
        float ty = -(top + bottom) / (top - bottom);
        float tz = - near / (far - near);

        val[M00] = x_orth;
        val[M10] = 0;
        val[M20] = 0;
        val[M30] = 0;
        val[M01] = 0;
        val[M11] = y_orth;
        val[M21] = 0;
        val[M31] = 0;
        val[M02] = 0;
        val[M12] = 0;
        val[M22] = z_orth;
        val[M32] = 0;
        val[M03] = tx;
        val[M13] = ty;
        val[M23] = tz;
        val[M33] = 1;
        return this;
    }

    /** Sets the matrix to a look at matrix with a direction and an up vector. Multiply with a translation matrix to get a camera
     * model view matrix.
     * @param direction The direction vector
     * @param up The up vector
     * @return This matrix for the purpose of chaining methods together. */
    public Matrix4 setToLookAt (Vector3 direction, Vector3 up) {
        l_vez.set(direction).nor();
        l_vex.set(direction).crs(up).nor();
        l_vey.set(l_vex).crs(l_vez).nor();
        idt();
        val[M00] = l_vex.x;
        val[M01] = l_vex.y;
        val[M02] = l_vex.z;
        val[M10] = l_vey.x;
        val[M11] = l_vey.y;
        val[M12] = l_vey.z;
        val[M20] = l_vez.x; // not: -l_vez.x
        val[M21] = l_vez.y;
        val[M22] = l_vez.z;
        return this;
    }

    /** Sets this matrix to a look at matrix with the given position, target and up vector.
     * @param position the position
     * @param target the target
     * @param up the up vector
     * @return This matrix */
    public Matrix4 setToLookAt (Vector3 position, Vector3 target, Vector3 up) {
        tmpVec.set(target).sub(position);
        setToLookAt(tmpVec, up);
        translate(-position.x, -position.y, -position.z);
        return this;
    }

    /** Sets this matrix to a translation matrix, overwriting it first by an identity matrix and then setting the 4th column to the
     * translation vector.
     * @param vector The translation vector
     * @return This matrix for the purpose of chaining methods together. */
    public Matrix4 setToTranslation (Vector3 vector) {
        idt();
        val[M03] = vector.x;
        val[M13] = vector.y;
        val[M23] = vector.z;
        return this;
    }

    /** Sets this matrix to a translation matrix, overwriting it first by an identity matrix and then setting the 4th column to the
     * translation vector.
     * @param x The x-component of the translation vector.
     * @param y The y-component of the translation vector.
     * @param z The z-component of the translation vector.
     * @return This matrix for the purpose of chaining methods together. */
    public Matrix4 setToTranslation (float x, float y, float z) {
        idt();
        val[M03] = x;
        val[M13] = y;
        val[M23] = z;
        return this;
    }

    public Matrix4 setTranslation (float x, float y, float z) {
        val[M03] = x;
        val[M13] = y;
        val[M23] = z;
        return this;
    }

    public Matrix4 setTranslation (Vector3 v) {
        val[M03] = v.x;
        val[M13] = v.y;
        val[M23] = v.z;
        return this;
    }

    public Matrix4 scale (float scale) {
        return this.scale(scale, scale, scale);
    }

    public Matrix4 scale (Vector3 sc) {
        return scale( sc.x, sc.y, sc.z );
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


    /** Inverts the matrix. Stores the result in this matrix.
     * @return This matrix for the purpose of chaining methods together.
     * @throws RuntimeException if the matrix is singular (not invertible) */
    public Matrix4 inv () {
        float l_det = val[M30] * val[M21] * val[M12] * val[M03] - val[M20] * val[M31] * val[M12] * val[M03]
                - val[M30] * val[M11] * val[M22] * val[M03] + val[M10] * val[M31] * val[M22] * val[M03]
                + val[M20] * val[M11] * val[M32] * val[M03] - val[M10] * val[M21] * val[M32] * val[M03]
                - val[M30] * val[M21] * val[M02] * val[M13] + val[M20] * val[M31] * val[M02] * val[M13]
                + val[M30] * val[M01] * val[M22] * val[M13] - val[M00] * val[M31] * val[M22] * val[M13]
                - val[M20] * val[M01] * val[M32] * val[M13] + val[M00] * val[M21] * val[M32] * val[M13]
                + val[M30] * val[M11] * val[M02] * val[M23] - val[M10] * val[M31] * val[M02] * val[M23]
                - val[M30] * val[M01] * val[M12] * val[M23] + val[M00] * val[M31] * val[M12] * val[M23]
                + val[M10] * val[M01] * val[M32] * val[M23] - val[M00] * val[M11] * val[M32] * val[M23]
                - val[M20] * val[M11] * val[M02] * val[M33] + val[M10] * val[M21] * val[M02] * val[M33]
                + val[M20] * val[M01] * val[M12] * val[M33] - val[M00] * val[M21] * val[M12] * val[M33]
                - val[M10] * val[M01] * val[M22] * val[M33] + val[M00] * val[M11] * val[M22] * val[M33];
        if (l_det == 0f) throw new RuntimeException("non-invertible matrix");
        float m00 = val[M12] * val[M23] * val[M31] - val[M13] * val[M22] * val[M31] + val[M13] * val[M21] * val[M32]
                - val[M11] * val[M23] * val[M32] - val[M12] * val[M21] * val[M33] + val[M11] * val[M22] * val[M33];
        float m01 = val[M03] * val[M22] * val[M31] - val[M02] * val[M23] * val[M31] - val[M03] * val[M21] * val[M32]
                + val[M01] * val[M23] * val[M32] + val[M02] * val[M21] * val[M33] - val[M01] * val[M22] * val[M33];
        float m02 = val[M02] * val[M13] * val[M31] - val[M03] * val[M12] * val[M31] + val[M03] * val[M11] * val[M32]
                - val[M01] * val[M13] * val[M32] - val[M02] * val[M11] * val[M33] + val[M01] * val[M12] * val[M33];
        float m03 = val[M03] * val[M12] * val[M21] - val[M02] * val[M13] * val[M21] - val[M03] * val[M11] * val[M22]
                + val[M01] * val[M13] * val[M22] + val[M02] * val[M11] * val[M23] - val[M01] * val[M12] * val[M23];
        float m10 = val[M13] * val[M22] * val[M30] - val[M12] * val[M23] * val[M30] - val[M13] * val[M20] * val[M32]
                + val[M10] * val[M23] * val[M32] + val[M12] * val[M20] * val[M33] - val[M10] * val[M22] * val[M33];
        float m11 = val[M02] * val[M23] * val[M30] - val[M03] * val[M22] * val[M30] + val[M03] * val[M20] * val[M32]
                - val[M00] * val[M23] * val[M32] - val[M02] * val[M20] * val[M33] + val[M00] * val[M22] * val[M33];
        float m12 = val[M03] * val[M12] * val[M30] - val[M02] * val[M13] * val[M30] - val[M03] * val[M10] * val[M32]
                + val[M00] * val[M13] * val[M32] + val[M02] * val[M10] * val[M33] - val[M00] * val[M12] * val[M33];
        float m13 = val[M02] * val[M13] * val[M20] - val[M03] * val[M12] * val[M20] + val[M03] * val[M10] * val[M22]
                - val[M00] * val[M13] * val[M22] - val[M02] * val[M10] * val[M23] + val[M00] * val[M12] * val[M23];
        float m20 = val[M11] * val[M23] * val[M30] - val[M13] * val[M21] * val[M30] + val[M13] * val[M20] * val[M31]
                - val[M10] * val[M23] * val[M31] - val[M11] * val[M20] * val[M33] + val[M10] * val[M21] * val[M33];
        float m21 = val[M03] * val[M21] * val[M30] - val[M01] * val[M23] * val[M30] - val[M03] * val[M20] * val[M31]
                + val[M00] * val[M23] * val[M31] + val[M01] * val[M20] * val[M33] - val[M00] * val[M21] * val[M33];
        float m22 = val[M01] * val[M13] * val[M30] - val[M03] * val[M11] * val[M30] + val[M03] * val[M10] * val[M31]
                - val[M00] * val[M13] * val[M31] - val[M01] * val[M10] * val[M33] + val[M00] * val[M11] * val[M33];
        float m23 = val[M03] * val[M11] * val[M20] - val[M01] * val[M13] * val[M20] - val[M03] * val[M10] * val[M21]
                + val[M00] * val[M13] * val[M21] + val[M01] * val[M10] * val[M23] - val[M00] * val[M11] * val[M23];
        float m30 = val[M12] * val[M21] * val[M30] - val[M11] * val[M22] * val[M30] - val[M12] * val[M20] * val[M31]
                + val[M10] * val[M22] * val[M31] + val[M11] * val[M20] * val[M32] - val[M10] * val[M21] * val[M32];
        float m31 = val[M01] * val[M22] * val[M30] - val[M02] * val[M21] * val[M30] + val[M02] * val[M20] * val[M31]
                - val[M00] * val[M22] * val[M31] - val[M01] * val[M20] * val[M32] + val[M00] * val[M21] * val[M32];
        float m32 = val[M02] * val[M11] * val[M30] - val[M01] * val[M12] * val[M30] - val[M02] * val[M10] * val[M31]
                + val[M00] * val[M12] * val[M31] + val[M01] * val[M10] * val[M32] - val[M00] * val[M11] * val[M32];
        float m33 = val[M01] * val[M12] * val[M20] - val[M02] * val[M11] * val[M20] + val[M02] * val[M10] * val[M21]
                - val[M00] * val[M12] * val[M21] - val[M01] * val[M10] * val[M22] + val[M00] * val[M11] * val[M22];
        float inv_det = 1.0f / l_det;
        val[M00] = m00 * inv_det;
        val[M10] = m10 * inv_det;
        val[M20] = m20 * inv_det;
        val[M30] = m30 * inv_det;
        val[M01] = m01 * inv_det;
        val[M11] = m11 * inv_det;
        val[M21] = m21 * inv_det;
        val[M31] = m31 * inv_det;
        val[M02] = m02 * inv_det;
        val[M12] = m12 * inv_det;
        val[M22] = m22 * inv_det;
        val[M32] = m32 * inv_det;
        val[M03] = m03 * inv_det;
        val[M13] = m13 * inv_det;
        val[M23] = m23 * inv_det;
        val[M33] = m33 * inv_det;
        return this;
    }

    /** @return The determinant of this matrix */
    public float det () {
        return val[M30] * val[M21] * val[M12] * val[M03] - val[M20] * val[M31] * val[M12] * val[M03]
                - val[M30] * val[M11] * val[M22] * val[M03] + val[M10] * val[M31] * val[M22] * val[M03]
                + val[M20] * val[M11] * val[M32] * val[M03] - val[M10] * val[M21] * val[M32] * val[M03]
                - val[M30] * val[M21] * val[M02] * val[M13] + val[M20] * val[M31] * val[M02] * val[M13]
                + val[M30] * val[M01] * val[M22] * val[M13] - val[M00] * val[M31] * val[M22] * val[M13]
                - val[M20] * val[M01] * val[M32] * val[M13] + val[M00] * val[M21] * val[M32] * val[M13]
                + val[M30] * val[M11] * val[M02] * val[M23] - val[M10] * val[M31] * val[M02] * val[M23]
                - val[M30] * val[M01] * val[M12] * val[M23] + val[M00] * val[M31] * val[M12] * val[M23]
                + val[M10] * val[M01] * val[M32] * val[M23] - val[M00] * val[M11] * val[M32] * val[M23]
                - val[M20] * val[M11] * val[M02] * val[M33] + val[M10] * val[M21] * val[M02] * val[M33]
                + val[M20] * val[M01] * val[M12] * val[M33] - val[M00] * val[M21] * val[M12] * val[M33]
                - val[M10] * val[M01] * val[M22] * val[M33] + val[M00] * val[M11] * val[M22] * val[M33];
    }

    public String toString () {
        return "[" + val[M00] + "|" + val[M01] + "|" + val[M02] + "|" + val[M03] + "]\n" //
                + "[" + val[M10] + "|" + val[M11] + "|" + val[M12] + "|" + val[M13] + "]\n" //
                + "[" + val[M20] + "|" + val[M21] + "|" + val[M22] + "|" + val[M23] + "]\n" //
                + "[" + val[M30] + "|" + val[M31] + "|" + val[M32] + "|" + val[M33] + "]\n";
    }

}
