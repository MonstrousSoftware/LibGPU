package com.monstrous.graphics.g3d.shapeBuilder;

import com.monstrous.graphics.VertexAttribute;
import com.monstrous.graphics.VertexAttributes;
import com.monstrous.graphics.g3d.MeshBuilder;
import com.monstrous.graphics.g3d.MeshPart;
import com.monstrous.math.Frustum;
import com.monstrous.math.Vector3;
import com.monstrous.webgpu.WGPUPrimitiveTopology;
import com.monstrous.webgpu.WGPUVertexFormat;

/** Build a wire frame frustum shape for the given Frustum object */
public class FrustumShapeBuilder {

    public static MeshPart build(Frustum frustum) {

        VertexAttributes vertexAttributes = new VertexAttributes();
        vertexAttributes.add(VertexAttribute.Usage.POSITION, "position", WGPUVertexFormat.Float32x4, 0);
        vertexAttributes.end();

        MeshBuilder mb = new MeshBuilder();
        mb.begin(vertexAttributes, 4*6, 8*6 );
        MeshPart part = mb.part("frustum", WGPUPrimitiveTopology.LineList);

        Vector3[] corners = frustum.corners;

        mb.addRect(corners[0], corners[3], corners[2], corners[1]); // front
        mb.addRect(corners[4], corners[5], corners[6], corners[7]); // back
        mb.addRect(corners[0], corners[1], corners[5], corners[4]); // top
        mb.addRect(corners[3], corners[7], corners[6], corners[2]); // bottom
        mb.addRect(corners[0], corners[4], corners[7], corners[3]); // left
        mb.addRect(corners[1], corners[2], corners[6], corners[5]); // right

        mb.end();
        return part;
    }

}
