package com.monstrous.graphics.g3d.shapeBuilder;

import com.monstrous.graphics.VertexAttribute;
import com.monstrous.graphics.VertexAttributes;
import com.monstrous.graphics.g3d.Mesh;
import com.monstrous.graphics.g3d.MeshBuilder;
import com.monstrous.webgpu.WGPUPrimitiveTopology;
import com.monstrous.webgpu.WGPUVertexFormat;

public class SphereShapeBuilder {


    public static Mesh build(float radius, int steps) {
        return build(radius, steps, WGPUPrimitiveTopology.TriangleStrip);
    }

    /** build a sphere mesh with given radius and number of subdivision steps to use. */
    public static Mesh build(float radius, int steps, WGPUPrimitiveTopology topology) {
        if(steps < 2)
            throw new IllegalArgumentException("buildSphere: steps must be >= 2");
        final int x_steps = steps;
        final int y_steps = steps;

        VertexAttributes vertexAttributes = new VertexAttributes();
        vertexAttributes.add(VertexAttribute.Usage.POSITION, "position", WGPUVertexFormat.Float32x4, 0);
        vertexAttributes.add(VertexAttribute.Usage.TEXTURE_COORDINATE, "uv", WGPUVertexFormat.Float32x2, 1);
        vertexAttributes.add(VertexAttribute.Usage.NORMAL, "normal", WGPUVertexFormat.Float32x3, 2);
        // beware: the shaderLocation values have to match the shader

        vertexAttributes.end();

        int numIndices;
        switch(topology) {
            case TriangleStrip:
            case LineList:
            case PointList:
            case LineStrip:
                                    numIndices = 2*(x_steps +1)* y_steps; break;
            default:
                throw new IllegalArgumentException("buildSphere: topology unsupported "+topology);
        }

        // Algorithm based on Learn OpenGL chapter on PBR

        MeshBuilder mb = new MeshBuilder();
        mb.begin(vertexAttributes,topology, (x_steps +1)*(y_steps +1), numIndices);

        double PI = Math.PI;
        for(int xstep = 0; xstep <= x_steps; xstep++){
            float rho = (float) xstep / x_steps;
            for(int ystep = 0; ystep <= y_steps; ystep++){
                float phi = (float) ystep / y_steps;

                float x = (float) (Math.cos(rho*2f*PI) * Math.sin(phi*PI));
                float y = (float)  Math.cos(phi*PI);
                float z = (float) (Math.sin(rho*2f*PI) * Math.sin(phi*PI));

                mb.setNormal(x, y, z);
                mb.setTextureCoordinate(rho, phi);
                mb.addVertex(x*radius, y*radius, z*radius);
            }
        }

        // add indices for a triangle strip

        boolean oddRow = false;
        for(int ystep = 0; ystep < y_steps; ystep++){
            if(!oddRow){
                for(int xstep = 0; xstep <= x_steps; xstep++){
                    mb.addIndex((short)(ystep * (x_steps +1) + xstep));
                    mb.addIndex((short)((ystep+1) * (x_steps +1) + xstep));
                }
            } else {
                for(int xstep = x_steps; xstep >= 0; xstep--) {
                    mb.addIndex((short) ((ystep + 1) * (x_steps + 1) + xstep));
                    mb.addIndex((short) (ystep * (x_steps + 1) + xstep));
                }
            }
            oddRow = !oddRow;
        }
        return mb.end();
    }
}
