/*******************************************************************************
 * Copyright 2025 Monstrous Software.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.monstrous.graphics.g3d;

import com.monstrous.graphics.Material;
import com.monstrous.graphics.loaders.*;
import com.monstrous.graphics.webgpu.Buffer;
import com.monstrous.math.Matrix4;
import com.monstrous.utils.Disposable;
import com.monstrous.utils.JavaWebGPU;
import com.monstrous.webgpu.WGPUBufferUsage;
import com.monstrous.webgpu.WGPUPrimitiveTopology;
import jnr.ffi.Pointer;

import java.util.ArrayList;

/** A Model combines meshes, materials and a node hierarchy containing meshParts.
 *
 */
public class Model implements Disposable {
    public String filePath;
    private ArrayList<Mesh> meshes;
    private boolean ownsMeshes = true;
    private ArrayList<Node> rootNodes;
    private ArrayList<Material> materials;
    public ArrayList<Matrix4> inverseBoneTransforms;
    public ArrayList<Node> joints;                          // list of nodes that act as skeletal joints
    public Buffer inverseBoneBuffer;    // may be null
    private ArrayList<Animation> animations;

    public Model() {
        meshes = new ArrayList<>();
        ownsMeshes = false;
        materials = new ArrayList<>();
        rootNodes = new ArrayList<>();
        animations = new ArrayList<>();
        inverseBoneTransforms = new ArrayList<>();
        joints = new ArrayList<>();
    }

    public Model(String filePath) {
        this();
        this.filePath = filePath.toLowerCase();

        // check file extension to choose loader
        ModelLoader loader = null;
        if (this.filePath.endsWith("obj")) {
            loader = new ObjLoader();
        } else if (this.filePath.endsWith("gltf")) {
            loader = new GLTFLoader();
        } else if (this.filePath.endsWith("glb")) {
            loader = new GLBLoader();
        } else
            throw new RuntimeException("Model: file name extension not supported : " + filePath);

        loader.loadFromFile(this, filePath);
        makeInverseBoneBuffer();
    }

    public Model(Mesh mesh, Material material) {
        this(mesh, WGPUPrimitiveTopology.TriangleList, material);
    }

    public Model(Mesh mesh, WGPUPrimitiveTopology topology, Material material) {
        meshes = new ArrayList<>();
        meshes.add(mesh);
        ownsMeshes = false;

        materials = new ArrayList<>();
        materials.add(material);

        // create a meshPart to cover whole mesh
        MeshPart meshPart;
        if (mesh.getIndexCount() > 0)
            meshPart = new MeshPart(mesh, "part", topology, 0, mesh.getIndexCount());
        else
            meshPart = new MeshPart(mesh, "part", topology, 0, mesh.getVertexCount());
        Node rootNode = new Node(new NodePart(meshPart, material));
        rootNodes = new ArrayList<>();
        rootNodes.add(rootNode);
    }

    public Model(MeshPart meshPart, Material material) {
        meshes = new ArrayList<>();
        meshes.add(meshPart.getMesh());
        ownsMeshes = false;

        materials = new ArrayList<>();
        materials.add(material);

        Node rootNode = new Node(new NodePart(meshPart, material));
        rootNodes = new ArrayList<>();
        rootNodes.add(rootNode);
    }

    public void addMaterial(Material mat) {
        materials.add(mat);
    }

    public ArrayList<Material> getMaterials() {
        return materials;
    }

    public void addMesh(Mesh mesh) {
        meshes.add(mesh);
    }

    public ArrayList<Mesh> getMeshes() {
        return meshes;
    }

    public void addNode(Node node) {
        rootNodes.add(node);
    }

    public ArrayList<Node> getNodes() {
        return rootNodes;
    }

    public void addAnimation(Animation animation) {
        this.animations.add(animation);
    }


    public ArrayList<Animation> getAnimations() {
        return animations;
    }

    /** create and fill GPU buffer for inverseBoneMatrices */
    private void makeInverseBoneBuffer() {
        int numBones = inverseBoneTransforms.size();
        if(numBones == 0)
            return;
        int usage = WGPUBufferUsage.CopyDst | WGPUBufferUsage.Storage;
        int matrixSize = 16*Float.BYTES;
        inverseBoneBuffer = new Buffer("inverse bone matrices",usage, numBones * matrixSize);
        Pointer floatData = JavaWebGPU.createDirectPointer(matrixSize );    // allocate native memory for one matrix
        int offset = 0;
        for(int i = 0; i < numBones; i++) {
            float floats[] = inverseBoneTransforms.get(i).val;
            floatData.put(0, floats, 0, 16);
            inverseBoneBuffer.write(offset, floatData, matrixSize);
            offset += matrixSize;
        }
    }

    @Override
    public void dispose() {
        if(ownsMeshes)
            for(Mesh mesh: meshes)
                mesh.dispose();
        for(Material material: materials)
            material.dispose();
        if(inverseBoneBuffer != null)
            inverseBoneBuffer.dispose();
//        for(Node node: rootNodes)
//            node.dispose();
    }
}
