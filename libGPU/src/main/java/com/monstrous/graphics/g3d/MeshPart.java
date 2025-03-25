package com.monstrous.graphics.g3d;

import com.monstrous.webgpu.WGPUPrimitiveTopology;

public class MeshPart {
    private final Mesh mesh;
    public final String id;
    private final WGPUPrimitiveTopology topology;
    private int offset;         // offset in vertex buffer (in number of vertices) or in case of an indexed mesh, offset in index buffer  (in number of indices)
    private int size;           // size in vertices or indices

    public MeshPart(Mesh mesh, String id, WGPUPrimitiveTopology topology) {
        this.mesh = mesh;
        this.id = id;
        this.topology = topology;
    }

    public MeshPart(Mesh mesh, String id, WGPUPrimitiveTopology topology, int offset, int size) {
        this(mesh, id, topology);
        this.offset = offset;
        this.size = size;
    }

    public Mesh getMesh(){
        return mesh;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public WGPUPrimitiveTopology getTopology() {
        return topology;
    }

    // todo to calc the bbox we need to get vertex data out of the buffer again
//    public void calculateBounds(){
//        int offset = mesh.vertexAttributes.getOffset(VertexAttribute.Usage.POSITION);
//        if(offset < 0)
//            throw new RuntimeException("Mesh has no POSITION information.");
//
//        boundingBox.clear();
//        for(int i = 0; i < size; i++){
//            mesh.getIndexBuffer().
//        }
//    }
}
