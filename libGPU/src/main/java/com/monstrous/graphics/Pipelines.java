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

    public Pipeline getPipeline(Pointer bindGroupLayout, PipelineSpecification spec){
        for(Pipeline pipeline : pipelines){
            if(pipeline.canRender(spec))
                return pipeline;
        }
        Pipeline pipeline = new Pipeline(bindGroupLayout, spec);
        pipelines.add(pipeline);
        return pipeline;
    }

    @Override
    public void dispose() {
        for(Pipeline pipeline : pipelines)
            pipeline.dispose();
    }
}
