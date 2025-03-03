/*******************************************************************************
 * Copyright 2025 Monstrous Software.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.monstrous.graphics.webgpu;

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

    // may be useful for hot-loading shaders
    public void clear(){
        dispose();
        pipelines.clear();
    }

    @Override
    public void dispose() {
        for(Pipeline pipeline : pipelines)
            pipeline.dispose();
    }
}
