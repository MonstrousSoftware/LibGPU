package com.monstrous.graphics.loaders;

import com.monstrous.graphics.loaders.gltf.*;

import java.util.ArrayList;

public class GLTF {

    public static int SBYTE8 = 5120;
    public static int UBYTE8 = 5121;
    public static int USHORT16 = 5123;
    public static int UINT32 = 5125;
    public static int FLOAT32 = 5126;


    int scene;
    public ArrayList<GLTFTexture> textures;
    public ArrayList<GLTFMaterial> materials;
    public ArrayList<GLTFImage> images;
    public ArrayList<GLTFSampler> samplers;
    public ArrayList<GLTFMesh> meshes;
    public ArrayList<GLTFBuffer> buffers;
    public ArrayList<GLTFBufferView> bufferViews;
    public ArrayList<GLTFAccessor> accessors;
    public ArrayList<GLTFNode> nodes;
    public ArrayList<GLTFScene> scenes;

    public GLTF() {
        textures = new ArrayList<>();
        materials = new ArrayList<>();
        images = new ArrayList<>();
        samplers = new ArrayList<>();
        meshes = new ArrayList<>();
        buffers = new ArrayList<>();
        bufferViews = new ArrayList<>();
        accessors = new ArrayList<>();
        nodes = new ArrayList<>();
        scenes = new ArrayList<>();
    }
}
