package com.monstrous.math;

// heavily borrowed from LibGDX

public class Matrix3 {
    // logical view:
    // M00 M01 M02
    // M10 M11 M12
    // M20 M21 M22
    //
    // translation vector in the matrix is M03, M13, M23
    public static final int M00 = 0;
    public static final int M01 = 3;
    public static final int M02 = 6;
    public static final int M10 = 1;
    public static final int M11 = 4;
    public static final int M12 = 7;
    public static final int M20 = 2;
    public static final int M21 = 5;
    public static final int M22 = 8;
    public float[] val = new float[9];



    /**
     * Constructs an identity matrix
     */
    public Matrix3() {
        idt();
    }

    public Matrix3 idt () {
        float[] val = this.val;
        val[M00] = 1;
        val[M10] = 0;
        val[M20] = 0;
        val[M01] = 0;
        val[M11] = 1;
        val[M21] = 0;
        val[M02] = 0;
        val[M12] = 0;
        val[M22] = 1;
        return this;
    }

    /** Constructs a matrix from the given float array. The array must have at least 16 elements; the first 16 will be copied.
     * @param values The float array to copy. Remember that this matrix is in
     *           <a href="http://en.wikipedia.org/wiki/Row-major_order">column major</a> order. (The float array is not
     *           modified) */
    public Matrix3(float[] values) {
        set(values);
    }


    /** Sets the matrix to the given matrix.
     * @param matrix The matrix that is to be copied. (The given matrix is not modified)
     * @return This matrix for the purpose of chaining methods together. */
    public Matrix3 set (Matrix3 matrix) {
        return set(matrix.val);
    }


    public Matrix3 set (float[] values) {
        System.arraycopy(values, 0, val, 0, val.length);
        return this;
    }

    public Matrix3( Vector3 a, Vector3 b, Vector3 c) {
        set(a,b,c);
    }

    public Matrix3 set( Vector3 a, Vector3 b, Vector3 c){
        float[] val = this.val;
        val[M00] = a.x;         // makes sense?
        val[M10] = a.y;
        val[M20] = a.z;
        val[M01] = b.x;
        val[M11] = b.y;
        val[M21] = b.z;
        val[M02] = c.x;
        val[M12] = c.y;
        val[M22] = c.z;
        return this;
    }

//    public Vector3 get(int col){
//
//    }

    public String toString () {
        float[] val = this.val;
        return "[" + val[M00] + "|" + val[M01] + "|" + val[M02] + "]\n" //
                + "[" + val[M10] + "|" + val[M11] + "|" + val[M12] + "]\n" //
                + "[" + val[M20] + "|" + val[M21] + "|" + val[M22] + "]";
    }

}
