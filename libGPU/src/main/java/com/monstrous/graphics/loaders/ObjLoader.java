package com.monstrous.graphics.loaders;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.Material;
import com.monstrous.graphics.g3d.*;
import com.monstrous.webgpu.WGPUPrimitiveTopology;

import java.util.ArrayList;

public class ObjLoader implements ModelLoader {


    @Override
    public Model loadFromFile(Model model, String filePath) {

        // todo fix if obj has no normal map we dont need tangent and bitangent and we should also not add this in vertex buffer
        ArrayList<MaterialData> mtlData = new ArrayList<>();
        MeshData meshData = ObjParser.load(filePath, mtlData);

        Mesh mesh = new Mesh(meshData);
        model.addMesh(mesh);


        // create a meshPart to cover whole mesh (temp)
        MeshPart meshPart;
        if(mesh.getIndexCount() > 0)
            meshPart = new MeshPart(mesh, "part", WGPUPrimitiveTopology.TriangleList, 0, mesh.getIndexCount());
        else
            meshPart = new MeshPart(mesh, "part", WGPUPrimitiveTopology.TriangleList, 0, mesh.getVertexCount());

        Material defaultmaterial = null;
        for(MaterialData mtl: mtlData) {
            Material material = new Material(mtl);
            model.addMaterial(material);
            if(defaultmaterial == null)                 // use first material for the rootNode (arbitrary)
                defaultmaterial = material;
        }

        Node rootNode = new Node();
        rootNode.nodeParts = new ArrayList<>();
        if(defaultmaterial == null)
            defaultmaterial = new Material( Color.WHITE );   // fallback
        rootNode.nodeParts.add( new NodePart(meshPart, defaultmaterial ));
        model.addRootNode(rootNode);
        return model;
    }


}
