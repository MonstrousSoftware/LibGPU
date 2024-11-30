package com.monstrous.graphics.g3d;

public class MeshPart {
    public Mesh mesh;
    public int offset;      // offset in vertex buffer or, in case of an indexed mesh, offset in index buffer
    public int size;

    public MeshPart(Mesh mesh, int offset, int size) {
        this.mesh = mesh;
        this.offset = offset;
        this.size = size;
    }
}
