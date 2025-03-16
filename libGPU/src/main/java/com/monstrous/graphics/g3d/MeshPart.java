package com.monstrous.graphics.g3d;

import com.monstrous.graphics.VertexAttribute;

public class MeshPart {
    public Mesh mesh;
    public int offset;      // offset in vertex buffer or, in case of an indexed mesh, offset in index buffer
    public int size;
//    public BoundingBox boundingBox;

    public MeshPart(Mesh mesh, int offset, int size) {
        this.mesh = mesh;
        this.offset = offset;
        this.size = size;
//        boundingBox = new BoundingBox();
    }

//    public void calculateBounds(){
//        int offset = mesh.vertexAttributes.getOffset(VertexAttribute.Usage.POSITION);
//        if(offset < 0)
//            throw new RuntimeException("Mesh has no POSITION information.");
//
//        boundingBox.clear();
//        for(int i = 0; i < size; i++){
//            mesh.getIndexBuffer().
//        }
//
//    }
}
