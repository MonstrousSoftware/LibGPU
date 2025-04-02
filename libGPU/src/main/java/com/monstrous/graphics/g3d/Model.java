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
import com.monstrous.utils.Disposable;
import com.monstrous.webgpu.WGPUPrimitiveTopology;

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


    public Model(){
        meshes = new ArrayList<>();
        ownsMeshes = false;
        materials = new ArrayList<>();
        rootNodes = new ArrayList<>();
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
        }else
            throw new RuntimeException("Model: file name extension not supported : "+filePath);

        loader.loadFromFile(this, filePath);
    }

    public Model(Mesh mesh, Material material){
        this(mesh, WGPUPrimitiveTopology.TriangleList, material);
    }

    public Model(Mesh mesh, WGPUPrimitiveTopology topology, Material material){
        meshes = new ArrayList<>();
        meshes.add(mesh);
        ownsMeshes = false;

        materials = new ArrayList<>();
        materials.add(material);

        // create a meshPart to cover whole mesh
        MeshPart meshPart;
        if(mesh.getIndexCount() > 0)
            meshPart = new MeshPart(mesh, "part", topology, 0, mesh.getIndexCount());
        else
            meshPart = new MeshPart(mesh, "part", topology, 0, mesh.getVertexCount());
        Node rootNode = new Node(new NodePart(meshPart, material ));
        rootNodes = new ArrayList<>();
        rootNodes.add(rootNode);
    }

    public Model( MeshPart meshPart, Material material){
        meshes = new ArrayList<>();
        meshes.add(meshPart.getMesh());
        ownsMeshes = false;

        materials = new ArrayList<>();
        materials.add(material);

        Node rootNode = new Node(new NodePart(meshPart, material ));
        rootNodes = new ArrayList<>();
        rootNodes.add(rootNode);
    }

    public void addMaterial(Material mat ){
        materials.add(mat);
    }

    public ArrayList<Material> getMaterials(){
        return materials;
    }

    public void addMesh( Mesh mesh ){
        meshes.add(mesh);
    }

    public ArrayList<Mesh> getMeshes(){
        return meshes;
    }

    public void addNode(Node node ){
        rootNodes.add(node);
    }

    public ArrayList<Node> getNodes(){
        return rootNodes;
    }


    @Override
    public void dispose() {
        if(ownsMeshes)
            for(Mesh mesh: meshes)
                mesh.dispose();
        for(Material material: materials)
            material.dispose();
//        for(Node node: rootNodes)
//            node.dispose();
    }
}
