package com.monstrous.graphics.g3d.shapeBuilder;

import com.monstrous.graphics.VertexAttribute;
import com.monstrous.graphics.VertexAttributes;
import com.monstrous.graphics.g3d.Mesh;
import com.monstrous.graphics.g3d.MeshBuilder;
import com.monstrous.webgpu.WGPUVertexFormat;

public class SphereShapeBuilder {
    final static int X_STEPS = 64;
    final static int Y_STEPS = 64;


    public static Mesh buildSphere(float radius) {

        VertexAttributes vertexAttributes = new VertexAttributes();
        vertexAttributes.add(VertexAttribute.Usage.POSITION, "position", WGPUVertexFormat.Float32x4, 0);
        vertexAttributes.add(VertexAttribute.Usage.TEXTURE_COORDINATE, "uv", WGPUVertexFormat.Float32x2, 1);
        vertexAttributes.add(VertexAttribute.Usage.NORMAL, "normal", WGPUVertexFormat.Float32x3, 2);
        // beware: the shaderLocation values have to match the shader

        vertexAttributes.end();

        MeshBuilder mb = new MeshBuilder();
        mb.begin(vertexAttributes, X_STEPS*Y_STEPS, 36);

        double PI = Math.PI;
        for(int xstep = 0; xstep < X_STEPS; xstep++){
            float rho = (float) xstep /X_STEPS;
            for(int ystep = 0; ystep < Y_STEPS; ystep++){
                float phi = (float) ystep / Y_STEPS;

                float x = (float) (Math.cos(rho*2f*PI) * Math.sin(phi*PI));
                float y = (float)  Math.cos(phi*PI);
                float z = (float) (Math.sin(rho*2f*PI) * Math.sin(phi*PI));

                mb.setNormal(x, y, z);
                mb.setTextureCoordinate(rho, phi);
                mb.addVertex(x, y, z);
            }
        }

        // todo add indices for a triangle strip

        return mb.end();
    }
}
