package com.monstrous.math;

public class Vector3 {
    public float x;
    public float y;
    public float z;

    public final static Vector3 Zero = new Vector3(0, 0, 0);

    public Vector3(){}

    public Vector3(float x, float y, float z){
        this.set(x, y, z);
    }

    public Vector3(Vector3 v){ this.set(v); }

    public Vector3 set(float x, float y, float z){
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3 set(Vector3 v){
        return set(v.x, v.y, v.z);
    }

    public Vector3 cpy(){
        return new Vector3(this);
    }

    public Vector3 add(Vector3 v){
        return set(this.x + v.x, this.y + v.y, this.z + v.z);
    }

    public Vector3 sub(Vector3 v){
        return set(this.x - v.x, this.y - v.y, this.z - v.z);
    }

    public float len2(){
        return x*x + y*y + z*z;
    }

    public float len(){
        return (float)Math.sqrt( x*x + y*y + z*z);
    }

    static public float len(float x, float y, float z){
        return (float)Math.sqrt( x*x + y*y + z*z);
    }

    public Vector3 scl (float scalar) {
        return this.set(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    public Vector3 crs (float x, float y, float z) {
        return this.set(this.y * z - this.z * y, this.z * x - this.x * z, this.x * y - this.y * x);
    }

    public Vector3 crs (Vector3 v) {
        return this.set(this.y * v.z - this.z * v.y, this.z * v.x - this.x * v.z, this.x * v.y - this.y * v.x);
    }

    /** Multiplies this vector by the given matrix dividing by w, assuming the fourth (w) component of the vector is 1. This is
     * mostly used to project/unproject vectors via a perspective projection matrix.
     *
     * @param matrix The matrix.
     * @return This vector for chaining */
    public Vector3 prj (final Matrix4 matrix) {
        final float[] l_mat = matrix.val;
        final float l_w = 1f / (x * l_mat[Matrix4.M30] + y * l_mat[Matrix4.M31] + z * l_mat[Matrix4.M32] + l_mat[Matrix4.M33]);
        return this.set((x * l_mat[Matrix4.M00] + y * l_mat[Matrix4.M01] + z * l_mat[Matrix4.M02] + l_mat[Matrix4.M03]) * l_w,
                (x * l_mat[Matrix4.M10] + y * l_mat[Matrix4.M11] + z * l_mat[Matrix4.M12] + l_mat[Matrix4.M13]) * l_w,
                (x * l_mat[Matrix4.M20] + y * l_mat[Matrix4.M21] + z * l_mat[Matrix4.M22] + l_mat[Matrix4.M23]) * l_w);
    }

    public Vector3 nor () {
        final float len2 = this.len2();
        if (len2 == 0f || len2 == 1f) return this;
        return this.scl(1f / (float)Math.sqrt(len2));
    }

    public float dot(Vector3 v){
        return x * v.x + y * v.y + z * v.z;
    }

    public static float dot (float x1, float y1, float z1, float x2, float y2, float z2) {
        return x1 * x2 + y1 * y2 + z1 * z2;
    }

    public static float dot (Vector3 a, Vector3 b) {
        return a.x * b.x + a.y * b.y + a.z * b.z;
    }

    /** Left-multiplies the vector by the given matrix, assuming the fourth (w) component of the vector is 1.
     * @param matrix The matrix
     * @return This vector for chaining */
    public Vector3 mul (final Matrix4 matrix) {
        final float[] l_mat = matrix.val;
        return this.set(x * l_mat[Matrix4.M00] + y * l_mat[Matrix4.M01] + z * l_mat[Matrix4.M02] + l_mat[Matrix4.M03],
                x * l_mat[Matrix4.M10] + y * l_mat[Matrix4.M11] + z * l_mat[Matrix4.M12] + l_mat[Matrix4.M13],
                x * l_mat[Matrix4.M20] + y * l_mat[Matrix4.M21] + z * l_mat[Matrix4.M22] + l_mat[Matrix4.M23]);
    }

    @Override
    public String toString () {
        return "(" + x + "," + y + "," + z + ")";
    }
}
