package com.monstrous.graphics.loaders.gltf;

import com.monstrous.math.Matrix4;
import com.monstrous.math.Quaternion;
import com.monstrous.math.Vector3;

import java.util.ArrayList;

public class GLTFNode {
    public String name;
    public int camera;
    public ArrayList<Integer> children;
    public int skin;
    public Matrix4 matrix;
    public int mesh;
    public Quaternion rotation;
    public Vector3 scale;
    public Vector3 translation;

    public GLTFNode() {
        children = new ArrayList<>();

        matrix = new Matrix4();
        scale = new Vector3(1f, 1f, 1f);
        translation = new Vector3(0,0,0);
        rotation = new Quaternion(0,0,0,1);
    }
}
