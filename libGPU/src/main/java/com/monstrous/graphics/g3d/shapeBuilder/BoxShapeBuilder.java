package com.monstrous.graphics.g3d.shapeBuilder;

import com.monstrous.graphics.g3d.BoundingBox;
import com.monstrous.graphics.g3d.MeshBuilder;
import com.monstrous.graphics.g3d.MeshPart;
import com.monstrous.math.Vector2;
import com.monstrous.math.Vector3;
import com.monstrous.webgpu.WGPUPrimitiveTopology;

/**
 * Build a box shape using a MeshBuilder.
 *
 * Typical usage:
 *
 *         VertexAttributes vertexAttributes = new VertexAttributes();
 *         vertexAttributes.add(VertexAttribute.Usage.POSITION, "position", WGPUVertexFormat.Float32x4, 0);
 *         vertexAttributes.add(VertexAttribute.Usage.TEXTURE_COORDINATE, "uv", WGPUVertexFormat.Float32x2, 1);
 *         vertexAttributes.end();
 *
 *         MeshBuilder mb = new MeshBuilder();
 *         mb.begin(vertexAttributes, 256, 256);
 *         MeshPart meshPart = BoxShapeBuilder.build(mb, 4, 4, 4);
 *         mb.end();
 */

public class BoxShapeBuilder {

    public static MeshPart build(MeshBuilder mb, float w, float h, float d) {
        return build(mb, w, h, d, WGPUPrimitiveTopology.TriangleList);
    }

    public static MeshPart build(MeshBuilder mb, float w, float h, float d, WGPUPrimitiveTopology topology) {
        w /= 2f;
        h /= 2f;
        d /= 2f;
        Vector3[] corners = {
                new Vector3(-w, h, -d), new Vector3(w, h, -d), new Vector3(w, -h, -d), new Vector3(-w, -h, -d),// front
                new Vector3(-w, h, d), new Vector3(w, h, d), new Vector3(w, -h, d), new Vector3(-w, -h, d),// back
        };
        return buildFromCorners(mb, corners, topology);
    }

    public static MeshPart build(MeshBuilder mb, BoundingBox bbox) {
        Vector3[] corners = new Vector3[8];
        corners[0] = new Vector3(bbox.min.x, bbox.max.y, bbox.min.z);
        corners[1] = new Vector3(bbox.max.x, bbox.max.y, bbox.min.z);
        corners[2] = new Vector3(bbox.max.x, bbox.min.y, bbox.min.z);
        corners[3] = new Vector3(bbox.min.x, bbox.min.y, bbox.min.z);
        corners[4] = new Vector3(bbox.min.x, bbox.max.y, bbox.max.z);
        corners[5] = new Vector3(bbox.max.x, bbox.max.y, bbox.max.z);
        corners[6] = new Vector3(bbox.max.x, bbox.min.y, bbox.max.z);
        corners[7] = new Vector3(bbox.min.x, bbox.min.y, bbox.max.z);
        return buildFromCorners(mb, corners, WGPUPrimitiveTopology.LineList);
    }

    private static MeshPart buildFromCorners(MeshBuilder mb, Vector3[] corners, WGPUPrimitiveTopology topology) {
        Vector2[] texCoords = {new Vector2(0, 0), new Vector2(1, 0), new Vector2(1, 1), new Vector2(0, 1)};

        int numIndices;
        switch(topology) {
            case TriangleList:    numIndices = 6 * 6; break;
            case LineList:        numIndices = 8 * 6; break;
            case PointList:       numIndices = 8 * 6; break;
            default:
                throw new IllegalArgumentException("buildBox: topology unsupported "+topology);
        }
        //mb.begin(vertexAttributes, 6 * 4, numIndices);
        MeshPart part = mb.part("box", topology);

        mb.setNormal(0, 0, -1);
        mb.addRect(corners[0], corners[3], corners[2], corners[1], texCoords[1], texCoords[2], texCoords[3], texCoords[0]); // front

        mb.setNormal(0, 0, 1);
        mb.addRect(corners[4], corners[5], corners[6], corners[7], texCoords[0], texCoords[1], texCoords[2], texCoords[3]); // back

        mb.setNormal(0, 1, 0);
        mb.addRect(corners[0], corners[1], corners[5], corners[4], texCoords[0], texCoords[1], texCoords[2], texCoords[3]); // top

        mb.setNormal(0, -1, 0);
        mb.addRect(corners[3], corners[7], corners[6], corners[2], texCoords[0], texCoords[1], texCoords[2], texCoords[3]); // bottom

        mb.setNormal(-1, 0, 0);
        mb.addRect(corners[0], corners[4], corners[7], corners[3], texCoords[0], texCoords[1], texCoords[2], texCoords[3]); // left

        mb.setNormal(1, 0, 0);
        mb.addRect(corners[1], corners[2], corners[6], corners[5], texCoords[1], texCoords[2], texCoords[3], texCoords[0]); // right

        mb.endPart();
        return part;
    }


}
