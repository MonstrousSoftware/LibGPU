package com.monstrous.graphics.g3d.shapeBuilder;

import com.monstrous.graphics.g3d.MeshBuilder;
import com.monstrous.graphics.g3d.MeshPart;
import com.monstrous.webgpu.WGPUPrimitiveTopology;

public class SphereShapeBuilder {


    public static MeshPart build(MeshBuilder mb, float radius, int steps) {
        return build(mb, radius, steps, WGPUPrimitiveTopology.TriangleStrip);
    }

    /** build a sphere mesh with given radius and number of subdivision steps to use. */
    public static MeshPart build(MeshBuilder mb, float radius, int steps, WGPUPrimitiveTopology topology) {
        if(steps < 2)
            throw new IllegalArgumentException("buildSphere: steps must be >= 2");
        final int x_steps = steps;
        final int y_steps = steps;

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

        //MeshBuilder mb = new MeshBuilder();
        //mb.begin(vertexAttributes, (x_steps +1)*(y_steps +1), numIndices);
        MeshPart part = mb.part("sphere", topology);
        short firstIndex = (short)mb.getVertexCount();  // get current offset in vertex buffer

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
                    mb.addIndex( (short)(firstIndex + ystep * (x_steps +1) + xstep));
                    mb.addIndex((short)(firstIndex + (ystep+1) * (x_steps +1) + xstep));
                }
            } else {
                for(int xstep = x_steps; xstep >= 0; xstep--) {
                    mb.addIndex((short) (firstIndex + (ystep + 1) * (x_steps + 1) + xstep));
                    mb.addIndex((short) (firstIndex + ystep * (x_steps + 1) + xstep));
                }
            }
            oddRow = !oddRow;
        }
        mb.endPart();
        return part;
    }
}
