package com.monstrous.graphics.g3d;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.Material;
import com.monstrous.graphics.VertexAttribute;
import com.monstrous.graphics.VertexAttributes;
import com.monstrous.math.Vector3;
import com.monstrous.webgpu.WGPUPrimitiveTopology;
import com.monstrous.webgpu.WGPUVertexFormat;

public class ModelBuilder {


    /** creates XYZ axes.
     * To do: lines are too thin to see and colours are subject to lighting conditions
     */
    public static Model createXYZCoordinates (float axisLength) {

        VertexAttributes vertexAttributes = new VertexAttributes();
        vertexAttributes.add(VertexAttribute.Usage.POSITION, "position", WGPUVertexFormat.Float32x4, 0);
        vertexAttributes.add(VertexAttribute.Usage.COLOR, "color", WGPUVertexFormat.Float32x4, 1);
        // beware: the shaderLocation values have to match the shader
        vertexAttributes.end();

        Vector3 origin = new Vector3(0,0,0);
        MeshBuilder mb = new MeshBuilder();
        mb.begin(vertexAttributes, 60, 60);
        MeshPart part = mb.part("XYZ Axes", WGPUPrimitiveTopology.LineList);
        mb.addLine(origin, new Vector3(axisLength, 0, 0));
        mb.setColor(Color.GREEN);
        mb.addLine(origin, new Vector3(0, axisLength, 0));
        mb.setColor(Color.BLUE);
        mb.addLine(origin, new Vector3(0,0,axisLength));
        mb.end();

        return new Model(part, new Material(Color.WHITE));
    }
}
