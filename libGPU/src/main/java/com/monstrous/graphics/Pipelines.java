package com.monstrous.graphics;

import com.monstrous.utils.Disposable;
import jnr.ffi.Pointer;

import java.util.ArrayList;


// Cache for pipelines

public class Pipelines implements Disposable {
    private ArrayList<Pipeline> pipelines;      // todo or use map?

    public Pipelines() {
        pipelines = new ArrayList<>();
    }

    public Pipeline getPipeline(VertexAttributes vertexAttributes, Pointer bindGroupLayout, ShaderProgram shader, boolean depth){
        for(Pipeline pipeline : pipelines){
            if(pipeline.canRender(vertexAttributes, depth))
                return pipeline;
        }
        Pipeline pipeline = new Pipeline(vertexAttributes, bindGroupLayout, shader, depth);
        pipelines.add(pipeline);
        return pipeline;
    }

    @Override
    public void dispose() {
        for(Pipeline pipeline : pipelines)
            pipeline.dispose();
    }
}
