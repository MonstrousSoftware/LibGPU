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
import com.monstrous.math.Quaternion;
import com.monstrous.math.Vector3;

import java.util.ArrayList;

public class Node {
    public Node parent;
    public ArrayList<Node> children;

    public String name;
    public boolean isAnimated;
    public Matrix4 localTransform;
    public Matrix4 globalTransform;
    public Vector3 translation;
    public Vector3 scale;
    public Quaternion rotation;

    public ArrayList<NodePart> nodeParts;

    public Node() {
        parent = null;
        children = new ArrayList<>(2);

        localTransform = new Matrix4();
        isAnimated = false;
        globalTransform = new Matrix4();
        translation = new Vector3(0,0,0);
        scale = new Vector3(1,1,1);
        rotation = new Quaternion(0,0,0,1);
        nodeParts = null;
    }

    public Node( NodePart nodePart ) {
        this();
        nodeParts = new ArrayList<>();
        nodeParts.add( nodePart );
    }

    public void addChild(Node child){
        child.parent = this;
        children.add(child);
    }

    public void updateMatrices(boolean recurse){
        if(!isAnimated)
            localTransform.set(translation, rotation, scale);

        if(parent != null)
            globalTransform.set(parent.globalTransform).mul(localTransform);
        else
            globalTransform.set(localTransform);
        if(recurse){
            for(Node child : children)
                child.updateMatrices(true);
        }
    }

    public void getRenderables(ArrayList<Renderable> renderables, Matrix4 instanceTransform, RenderablePool pool ){
        if(nodeParts != null) {
            for (NodePart nodePart : nodeParts) {
                Renderable renderable = pool.obtain();
                renderable.set(nodePart.meshPart, nodePart.material, instanceTransform);
                // combine globalTransform from node with modelTransform from model instance
                renderable.modelTransform.mul(globalTransform);
                renderables.add(renderable);
            }
        }
        for(Node child : children)
            child.getRenderables(renderables, instanceTransform, pool);
    }


}
