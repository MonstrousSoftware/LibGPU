package com.monstrous.graphics;

import com.monstrous.graphics.loaders.MeshData;
import com.monstrous.graphics.loaders.ObjLoader;
import com.monstrous.utils.Disposable;

// A model combines meshpart(s) and material
public class Model implements Disposable {
    public String filePath;
    public Mesh mesh;
    public MeshPart meshPart;       // just one for now
    public Material material;

    public Model(String filePath) {
        this.filePath = filePath;

        MeshData meshData = ObjLoader.load(filePath);
        mesh = new Mesh(meshData);

        System.out.println("Loaded "+meshData.objectName);

        // create a meshPart to cover whole mesh (temp)
        if(mesh.getIndexCount() > 0)
            meshPart = new MeshPart(mesh, 0, mesh.getIndexCount());
        else
            meshPart = new MeshPart(mesh, 0, mesh.getVertexCount());

        material = new Material(meshData.materialData);
    }

    @Override
    public void dispose() {
        mesh.dispose();
        material.dispose();
    }
}
