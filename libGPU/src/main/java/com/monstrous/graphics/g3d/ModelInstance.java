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

package com.monstrous.graphics.g3d;

import com.monstrous.graphics.Renderable;
import com.monstrous.graphics.RenderablePool;
import com.monstrous.math.Matrix4;

import java.util.ArrayList;

public class ModelInstance {
    public Model model;
    public final Matrix4 transform;
    public final BoundingBox boundingBox;

    public ModelInstance(Model model){
        this(model, new Matrix4());
    }

    public ModelInstance(Model model, float x, float y, float z) {
        this(model,new Matrix4().translate(x,y,z) );
    }

    public ModelInstance(Model model, Matrix4 transform) {
        if(model == null)
            throw new RuntimeException("ModelInstance: model is null");
        this.model = model;
        this.transform = transform;        // note: not a copy, so that the caller can modify the transform
        this.boundingBox = new BoundingBox();
        update();
    }

    /** update bounding boxes to match the instance transform. Call this after changing the transform. */
    public void update(){
        boundingBox.set(model.getMeshes().get(0).boundingBox);   // todo assumes only one mesh per model
        boundingBox.transform(transform);
    }

    public void getRenderables(ArrayList<Renderable> renderables, RenderablePool pool ){
        for(Node rootNode : model.getNodes())
            rootNode.getRenderables(renderables, transform, pool);
    }
}
