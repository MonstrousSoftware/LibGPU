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

package com.monstrous.graphics.g3d.ibl;


import com.monstrous.Files;
import com.monstrous.LibGPU;
import com.monstrous.graphics.*;
import com.monstrous.graphics.g3d.*;
import com.monstrous.graphics.g3d.shapeBuilder.BoxShapeBuilder;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.graphics.webgpu.RenderPassType;
import com.monstrous.math.Vector3;
import com.monstrous.utils.Disposable;
import com.monstrous.webgpu.*;


/** Utility methods to create IBL textures */
public class ImageBasedLighting implements Disposable {

    private final Texture[] textureSides;
    private final ModelBatch modelBatch;
    private final Environment environment;
    private final PerspectiveCamera snapCam;

    public ImageBasedLighting() {

        environment = new Environment();
        modelBatch = new ModelBatch();
        textureSides = new Texture[6];

        snapCam = new PerspectiveCamera(90, 1, 1);
        snapCam.position.set(0,0,-1);
        snapCam.direction.set(0,0,1);
        snapCam.update();
    }

    public Texture buildEnvironmentMapFromEquirectangularTexture(Texture equiRectangular, int size){
        // Convert an equirectangular image to a cube map
        Material material = new Material( equiRectangular );
        Model cube = new Model(buildUnitCube(), material);
        ModelInstance instance = new ModelInstance(cube);

        environment.shaderSourcePath = "shaders/modelbatchEquilateral.wgsl";
        LibGPU.commandEncoder = LibGPU.app.prepareEncoder();
        LibGPU.graphics.passNumber = 0;

        constructSideTextures(instance, size);
        Texture environmentMap = copyTextures(size);
        LibGPU.app.finishEncoder(LibGPU.commandEncoder);
        environment.shaderSourcePath = null;
        cube.dispose();

        return environmentMap;
    }

    public Texture buildIrradianceMap(Texture environmentMap, int size){
        // Convert an environment cube map to an irradiance cube map
        Model cube = new Model(buildUnitCube(), new Material(Color.WHITE));
        ModelInstance instance = new ModelInstance(cube);
        environment.shaderSourcePath = "shaders/modelbatchCubeMapIrradiance.wgsl";
        environment.setCubeMap(environmentMap);
        LibGPU.commandEncoder = LibGPU.app.prepareEncoder();
        LibGPU.graphics.passNumber = 0;

        constructSideTextures(instance, size);
        Texture irradianceMap = copyTextures(size);
        LibGPU.app.finishEncoder(LibGPU.commandEncoder);
        environment.shaderSourcePath = null;
        cube.dispose();
        return irradianceMap;
    }

    public Texture buildRadianceMap(Texture environmentMap, int size, int mipLevels){
        Texture prefilterMap = new Texture(size, size, true, 6 );  // mipmapped cube map
        Model cube = new Model(buildUnitCube(), new Material(Color.WHITE));
        ModelInstance instance = new ModelInstance(cube);
        // Convert an environment cube map to a radiance cube map
        environment.shaderSourcePath = "shaders/modelbatchCubeMapRadiance.wgsl";        // hacky
        environment.setCubeMap(environmentMap);
        LibGPU.commandEncoder = LibGPU.app.prepareEncoder();
        LibGPU.graphics.passNumber = 0;
        for(int mip = 0; mip < mipLevels; mip++) {
            environment.ambientLightLevel = (float)mip/mipLevels;   // hacky; use this to pass roughness level
            constructSideTextures(instance, size);
            copyTextures(prefilterMap, size, mip);
            size /= 2;
        }
        LibGPU.app.finishEncoder(LibGPU.commandEncoder);
        environment.shaderSourcePath = null;
        cube.dispose();
        return prefilterMap;
    }

    public Texture getBRDFLookUpTable(){
        return new Texture(Files.internal("environment/LUT.png"), false);
    }


    // the order of the layers is +X, -X, +Y, -Y, +Z, -Z
    private final Vector3[] directions = { new Vector3(1, 0, 0), new Vector3(-1, 0, 0), new Vector3(0, -1, 0), new Vector3(0, 1, 0),
            new Vector3(0,0,1), new Vector3(0, 0, -1)
    };


    private void constructSideTextures(ModelInstance instance, int size){

        for (int side = 0; side < 6; side++) {

            snapCam.direction.set(directions[side]);
            snapCam.position.set(new Vector3(directions[side]).scl(-1));
            if(side == 3)
                snapCam.up.set(0,0,-1);
            else if (side == 2)
                snapCam.up.set(0,0,1);
            else
                snapCam.up.set(0,1,0);
            snapCam.update();

            textureSides[side] = new Texture(size, size, false, true, WGPUTextureFormat.RGBA8Unorm, 1);


            modelBatch.begin(snapCam, environment, Color.GREEN, textureSides[side], null, RenderPassType.NO_DEPTH );
            modelBatch.render(instance);
            modelBatch.end();
        }

    }



    /** copy 6 textures (textureSides[]) into a new cube map */
    private Texture copyTextures(int size) {
        Texture cube = new Texture(size, size, 6);
        return copyTextures(cube, size, 0);
    }


    private Texture copyTextures(Texture cube, int size, int mipLevel){
        for (int side = 0; side < 6; side++) {

            WGPUImageCopyTexture source = WGPUImageCopyTexture.createDirect()
                    .setTexture(textureSides[side].getHandle())
                    .setMipLevel(0)
                    .setAspect(WGPUTextureAspect.All);
            source.getOrigin().setX(0);
            source.getOrigin().setY(0);
            source.getOrigin().setZ(0);

            WGPUImageCopyTexture destination = WGPUImageCopyTexture.createDirect()
                    .setTexture(cube.getHandle())
                    .setMipLevel(0)
                    .setAspect(WGPUTextureAspect.All);
            destination.getOrigin().setX(0);
            destination.getOrigin().setY(0);
            destination.getOrigin().setZ(side);
            destination.setMipLevel(mipLevel);

            WGPUExtent3D ext = WGPUExtent3D.createDirect()
                    .setWidth(size)
                    .setHeight(size)
                    .setDepthOrArrayLayers(1);

            LibGPU.webGPU.wgpuCommandEncoderCopyTextureToTexture(LibGPU.commandEncoder, source, destination, ext);
        }
        return cube;
    }

    private Mesh buildUnitCube(){

        MeshBuilder mb = new MeshBuilder();
        // vertex attributes are fixed per mesh
        VertexAttributes vertexAttributes = new VertexAttributes();
        vertexAttributes.add(VertexAttribute.Usage.POSITION, "position", WGPUVertexFormat.Float32x4, 0);
        vertexAttributes.add(VertexAttribute.Usage.TEXTURE_COORDINATE, "uv", WGPUVertexFormat.Float32x2, 1);
        vertexAttributes.end();

        mb.begin(vertexAttributes, 32, 36);

        BoxShapeBuilder.build(mb, 1, 1, 1,  WGPUPrimitiveTopology.TriangleList);
        return mb.end();
    }



    @Override
    public void dispose(){
        // cleanup
        modelBatch.dispose();
    }


}
