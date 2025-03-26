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

import com.monstrous.Files;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.Material;
import com.monstrous.graphics.VertexAttribute;
import com.monstrous.graphics.VertexAttributes;
import com.monstrous.graphics.loaders.*;
import com.monstrous.graphics.loaders.gltf.*;
import com.monstrous.math.Vector2;
import com.monstrous.math.Vector3;
import com.monstrous.utils.Disposable;
import com.monstrous.webgpu.WGPUPrimitiveTopology;
import com.monstrous.webgpu.WGPUVertexFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// A model combines meshpart(s) and material(s)
public class Model implements Disposable {
    public String filePath;
    public ArrayList<Mesh> meshes;
    private boolean ownsMeshes = true;
    public ArrayList<Node> rootNodes;
    public ArrayList<Material> materials;
    private final Map<GLTFPrimitive, Mesh> meshMap = new HashMap<>();
    private final Map<Integer, Boolean> hasNormalMap = new HashMap<>();

    public Model(String filePath) {
        this.filePath = filePath.toLowerCase();
        meshes = new ArrayList<>();
        materials = new ArrayList<>();

        // check file extension to choose loader

        if (this.filePath.endsWith("obj")) {
            readObj(filePath);
        } else if (this.filePath.endsWith("gltf")) {
            readGLTF(filePath);
        } else if (this.filePath.endsWith("glb")) {
            readGLB(filePath);
        }else
            throw new RuntimeException("Model: file name extension not supported : "+filePath);

    }

    public Model(Mesh mesh, Material material){
        this(mesh, WGPUPrimitiveTopology.TriangleList, material);
    }

    public Model(Mesh mesh, WGPUPrimitiveTopology topology, Material material){
        meshes = new ArrayList<>();
        meshes.add(mesh);
        ownsMeshes = false;

        materials = new ArrayList<>();
        materials.add(material);

        // create a meshPart to cover whole mesh
        MeshPart meshPart;
        if(mesh.getIndexCount() > 0)
            meshPart = new MeshPart(mesh, "part", topology, 0, mesh.getIndexCount());
        else
            meshPart = new MeshPart(mesh, "part", topology, 0, mesh.getVertexCount());
        Node rootNode = new Node(new NodePart(meshPart, material ));
        rootNodes = new ArrayList<>();
        rootNodes.add(rootNode);
    }

    public Model( MeshPart meshPart, Material material){
        meshes = new ArrayList<>();
        meshes.add(meshPart.getMesh());
        ownsMeshes = false;

        materials = new ArrayList<>();
        materials.add(material);

        Node rootNode = new Node(new NodePart(meshPart, material ));
        rootNodes = new ArrayList<>();
        rootNodes.add(rootNode);
    }


    private void readGLB(String filePath) {
        meshMap.clear();
        GLTF gltf = GLBLoader.load(filePath);
        processGLTF(gltf);
    }

    private void readGLTF(String filePath) {
        meshMap.clear();
        GLTF gltf = GLTFLoader.load(filePath);      // TMP
        processGLTF(gltf);
    }

    private byte[] readImageData( GLTF gltf, int textureId )  {
        byte[] bytes;

        GLTFImage image = gltf.images.get( gltf.textures.get(textureId).source );
        if(image.uri != null){
            bytes = Files.internal(image.uri).readAllBytes();
        } else {
            GLTFBufferView view = gltf.bufferViews.get(image.bufferView);
            if(view.buffer != 0)
                throw new RuntimeException("GLTF can only support buffer 0");

            bytes = new byte[view.byteLength];

            gltf.rawBuffer.byteBuffer.position(view.byteOffset);
            gltf.rawBuffer.byteBuffer.get(bytes);
        }
        return bytes;
    }

    private void processGLTF(GLTF gltf){

        ArrayList<MaterialData> mtlData = new ArrayList<>();
        int index = 0;
        for(GLTFMaterial gltfMat :  gltf.materials){
            MaterialData mat = new MaterialData();

            mat.name = gltfMat.name != null ? gltfMat.name : "mat"+index;   // copy name or generate one as a debugging aid
            index++;

            if(gltfMat.pbrMetallicRoughness.baseColorFactor != null)
                mat.diffuse = gltfMat.pbrMetallicRoughness.baseColorFactor;
            if(gltfMat.pbrMetallicRoughness.roughnessFactor >= 0)
                mat.roughnessFactor = gltfMat.pbrMetallicRoughness.roughnessFactor;
            if(gltfMat.pbrMetallicRoughness.metallicFactor >= 0)
                mat.metallicFactor = gltfMat.pbrMetallicRoughness.metallicFactor;
            if(gltfMat.pbrMetallicRoughness.baseColorTexture >= 0)
                mat.diffuseMapData = readImageData(gltf, gltfMat.pbrMetallicRoughness.baseColorTexture);
            if(gltfMat.pbrMetallicRoughness.metallicRoughnessTexture >= 0)
                mat.metallicRoughnessMapData = readImageData(gltf, gltfMat.pbrMetallicRoughness.metallicRoughnessTexture);
            if(gltfMat.normalTexture >= 0)
                mat.normalMapData = readImageData(gltf, gltfMat.normalTexture);
            if(gltfMat.emissiveTexture >= 0)
                mat.emissiveMapData =  readImageData(gltf, gltfMat.emissiveTexture);
            if(gltfMat.occlusionTexture >= 0)
                mat.occlusionMapData =  readImageData(gltf, gltfMat.occlusionTexture);

            mtlData.add(mat);
        }

        long startLoad = System.currentTimeMillis();


        for(MaterialData mtl: mtlData) {
            Material material = new Material(mtl);
            materials.add(material);
        }
        long endLoad = System.currentTimeMillis();
        System.out.println("Material loading/generation time (ms): "+(endLoad - startLoad));

        startLoad = System.currentTimeMillis();
        for(GLTFMesh gltfMesh : gltf.meshes){
            // build a mesh for each primitive
            for(GLTFPrimitive primitive : gltfMesh.primitives){
                Mesh mesh = buildMesh(gltf,  gltf.rawBuffer, primitive );
                meshes.add(mesh);
                meshMap.put(primitive, mesh);
            }
        }

        endLoad = System.currentTimeMillis();
        System.out.println("Mesh loading time (ms): "+(endLoad - startLoad));

//        for(GLTFScene scene : gltf.scenes ){
//
//        }

        rootNodes = new ArrayList<>();
        GLTFScene scene = gltf.scenes.get(gltf.scene);
        for( int nodeId : scene.nodes ) {
            GLTFNode gltfNode = gltf.nodes.get(nodeId);

            Node rootNode = addNode(gltf, gltfNode);     // recursively add the node hierarchy
            rootNode.updateMatrices(true);
            rootNodes.add(rootNode);
        }
        //System.out.println("loaded "+filePath);
    }

    private Node addNode(GLTF gltf, GLTFNode gltfNode){
        Node node = new Node();
        node.name = gltfNode.name;

        // optional transforms
        if(gltfNode.matrix != null){
            gltfNode.matrix.getTranslation(node.translation);
            gltfNode.matrix.getScale(node.scale);
            gltfNode.matrix.getRotation(node.rotation);
        }
        if(gltfNode.translation != null)
            node.translation.set(gltfNode.translation);
        if(gltfNode.scale != null)
            node.scale.set(gltfNode.scale);
        if(gltfNode.rotation != null)
            node.rotation.set(gltfNode.rotation);

        if(gltfNode.mesh >= 0){ // this node refers to a mesh
            node.nodeParts = new ArrayList<>();
            GLTFMesh gltfMesh = gltf.meshes.get(gltfNode.mesh);
            for( GLTFPrimitive primitive : gltfMesh.primitives) {
                GLTFAccessor indexAccessor = gltf.accessors.get(primitive.indices);
                GLTFBufferView view = gltf.bufferViews.get(indexAccessor.bufferView);
                if (!indexAccessor.type.contentEquals("SCALAR"))
                    throw new RuntimeException("GLTF: Expect primitive.indices to refer to SCALAR accessor");

                Mesh m = meshMap.get(primitive);
                MeshPart meshPart = new MeshPart(m, "part", WGPUPrimitiveTopology.TriangleList, 0, indexAccessor.count);
                node.nodeParts.add( new NodePart(meshPart, materials.get(primitive.material)) );
            }
        }
        // now add any children
        for(int j : gltfNode.children ){
            GLTFNode gltfChild = gltf.nodes.get(j);
            Node child = addNode(gltf, gltfChild);
            node.addChild(child);
        }
        return node;
    }


    private Mesh buildMesh(GLTF gltf, GLTFRawBuffer rawBuffer, GLTFPrimitive primitive){

        int indexAccessorId = primitive.indices;
        GLTFAccessor indexAccessor = gltf.accessors.get(indexAccessorId);

        GLTFBufferView view = gltf.bufferViews.get(indexAccessor.bufferView);
        if(view.buffer != 0)
            throw new RuntimeException("GLTF: Can only support buffer 0");
        int offset = view.byteOffset;
        offset += indexAccessor.byteOffset;

        boolean hasNormalMap = gltf.materials.get(primitive.material).normalTexture >= 0;

        MeshData meshData = new MeshData();

        // todo adjust this based on the file contents:
        meshData.vertexAttributes = new VertexAttributes();
        int location = 0;
        meshData.vertexAttributes.add(VertexAttribute.Usage.POSITION, "position", WGPUVertexFormat.Float32x3, location++);
        meshData.vertexAttributes.add(VertexAttribute.Usage.TEXTURE_COORDINATE, "uv", WGPUVertexFormat.Float32x2, location++);
        meshData.vertexAttributes.add(VertexAttribute.Usage.NORMAL, "normal", WGPUVertexFormat.Float32x3, location++);
        if(hasNormalMap) {
            meshData.vertexAttributes.add(VertexAttribute.Usage.TANGENT, "tangent", WGPUVertexFormat.Float32x3, location++);
            meshData.vertexAttributes.add(VertexAttribute.Usage.BITANGENT, "bitangent", WGPUVertexFormat.Float32x3, location++);
        }
        meshData.vertexAttributes.end();

        if(indexAccessor.componentType != GLTF.USHORT16 && indexAccessor.componentType != GLTF.UINT32 )
            throw new RuntimeException("GLTF: Can only support short or integer index");

        rawBuffer.byteBuffer.position(offset);

        int max = -1;
        if(indexAccessor.componentType == GLTF.USHORT16){
            meshData.indexSizeInBytes = 2; // 16 bit index
            for(int i = 0; i < indexAccessor.count; i++){
                meshData.indexValues.add( (int)rawBuffer.byteBuffer.getShort());
            }
        } else {
            meshData.indexSizeInBytes = 4; // 32 bit index
            for(int i = 0; i < indexAccessor.count; i++){
                int index = rawBuffer.byteBuffer.getInt();
                if(index > max)
                    max = index;
                meshData.indexValues.add( index);
            }
            //System.out.println("max index "+max); // TMP
        }

        //boolean hasNormalMap = meshData.vertexAttributes.hasUsage(VertexAttribute.Usage.TANGENT);

        int positionAccessorId = -1;
        int normalAccessorId = -1;
        int uvAccessorId = -1;
        int tangentAccessorId = -1;
        ArrayList<GLTFAttribute> attributes = primitive.attributes;
        for(GLTFAttribute attribute : attributes){
            if(attribute.name.contentEquals("POSITION")){
                positionAccessorId = attribute.value;
            } else if(attribute.name.contentEquals("NORMAL")){
                normalAccessorId = attribute.value;
            } else  if(attribute.name.contentEquals("TEXCOORD_0")){
                uvAccessorId = attribute.value;
            } else if(attribute.name.contentEquals("TANGENT")){
                tangentAccessorId = attribute.value;
            }
        }
        if(positionAccessorId < 0)
            throw new RuntimeException("GLTF: need POSITION attribute");
        GLTFAccessor positionAccessor = gltf.accessors.get(positionAccessorId);
        view = gltf.bufferViews.get(positionAccessor.bufferView);
        if(view.buffer != 0)
            throw new RuntimeException("GLTF: Can only support buffer 0");
        offset = view.byteOffset;
        offset += positionAccessor.byteOffset;

        //System.out.println("Position offset: "+offset);
        //System.out.println("Position count: "+positionAccessor.count);

        if(positionAccessor.componentType != GLTF.FLOAT32 || !positionAccessor.type.contentEquals("VEC3"))
            throw new RuntimeException("GLTF: Can only support float positions as VEC3");

        ArrayList<Vector3> positions = new ArrayList<>();
        rawBuffer.byteBuffer.position(offset);
        for(int i = 0; i < positionAccessor.count; i++){
            // assuming float32
            float f1 = rawBuffer.byteBuffer.getFloat();
            float f2 = rawBuffer.byteBuffer.getFloat();
            float f3 = rawBuffer.byteBuffer.getFloat();
            //System.out.println("float  "+f1 + " "+ f2 + " "+f3);
            positions.add(new Vector3(f1, f2, f3));
        }

        ArrayList<Vector3> normals = new ArrayList<>();
        if(normalAccessorId >= 0) {
            GLTFAccessor normalAccessor = gltf.accessors.get(normalAccessorId);
            view = gltf.bufferViews.get(normalAccessor.bufferView);
            if (view.buffer != 0)
                throw new RuntimeException("GLTF: Can only support buffer 0");
            offset = view.byteOffset;
            offset += normalAccessor.byteOffset;


            if (normalAccessor.componentType != GLTF.FLOAT32 || !positionAccessor.type.contentEquals("VEC3"))
                throw new RuntimeException("GLTF: Can only support float normals as VEC3");

            rawBuffer.byteBuffer.position(offset);
            for (int i = 0; i < normalAccessor.count; i++) {
                // assuming float32
                float f1 = rawBuffer.byteBuffer.getFloat();
                float f2 = rawBuffer.byteBuffer.getFloat();
                float f3 = rawBuffer.byteBuffer.getFloat();
                //System.out.println("float  "+f1 + " "+ f2 + " "+f3);
                normals.add(new Vector3(f1, f2, f3));
            }
        }

        ArrayList<Vector3> tangents = new ArrayList<>();
        if(tangentAccessorId >= 0) {
            GLTFAccessor tangentAccessor = gltf.accessors.get(tangentAccessorId);
            view = gltf.bufferViews.get(tangentAccessor.bufferView);
            if (view.buffer != 0)
                throw new RuntimeException("GLTF: Can only support buffer 0");
            offset = view.byteOffset;
            offset += tangentAccessor.byteOffset;

            if (tangentAccessor.componentType != GLTF.FLOAT32 || !positionAccessor.type.contentEquals("VEC3"))
                throw new RuntimeException("GLTF: Can only support float tangents as VEC3");

            rawBuffer.byteBuffer.position(offset);
            for (int i = 0; i < tangentAccessor.count; i++) {
                // assuming float32
                float f1 = rawBuffer.byteBuffer.getFloat();
                float f2 = rawBuffer.byteBuffer.getFloat();
                float f3 = rawBuffer.byteBuffer.getFloat();
                //System.out.println("float  "+f1 + " "+ f2 + " "+f3);
                tangents.add(new Vector3(f1, f2, f3));
            }
        }

        ArrayList<Vector2> textureCoordinates = new ArrayList<>();
        if(uvAccessorId >= 0) {

            GLTFAccessor uvAccessor = gltf.accessors.get(uvAccessorId);
            view = gltf.bufferViews.get(uvAccessor.bufferView);
            if (view.buffer != 0)
                throw new RuntimeException("GLTF: Can only support buffer 0");
            offset = view.byteOffset;
            offset += uvAccessor.byteOffset;

            //System.out.println("UV offset: " + offset);

            if (uvAccessor.componentType != GLTF.FLOAT32 || !uvAccessor.type.contentEquals("VEC2"))
                throw new RuntimeException("GLTF: Can only support float positions as VEC2");


            rawBuffer.byteBuffer.position(offset);
            for (int i = 0; i < uvAccessor.count; i++) {
                // assuming float32
                float f1 = rawBuffer.byteBuffer.getFloat();
                float f2 = rawBuffer.byteBuffer.getFloat();
                //System.out.println("float  "+f1 + " "+ f2 );
                textureCoordinates.add(new Vector2(f1, f2));
            }
        }


        ArrayList<Vector3> bitangents = new ArrayList<>();
        // if the material has a normal map and tangents are not provided we need to calculate them
        if(hasNormalMap && (tangents.size() == 0  || bitangents.size() == 0))
            addTBN(meshData, positions, textureCoordinates, normals, tangents, bitangents);

        // x y z   u v   nx ny nz (tx ty tz   bx by bz)
        meshData.objectName = gltf.nodes.get(0).name;
        Vector3 normal = new Vector3(0, 1, 0);
        Vector2 uv = new Vector2();
        for(int i = 0; i < positions.size(); i++){
            Vector3 pos = positions.get(i);
            meshData.vertFloats.add(pos.x);
            meshData.vertFloats.add(pos.y);
            meshData.vertFloats.add(pos.z);

            if(!textureCoordinates.isEmpty())
                uv =  textureCoordinates.get(i);
            meshData.vertFloats.add(uv.x);
            meshData.vertFloats.add(uv.y);

            if(!normals.isEmpty())
                normal = normals.get(i);
            meshData.vertFloats.add(normal.x);
            meshData.vertFloats.add(normal.y);
            meshData.vertFloats.add(normal.z);

            if(hasNormalMap) {
                Vector3 tangent = tangents.get(i);
                meshData.vertFloats.add(tangent.x);
                meshData.vertFloats.add(tangent.y);
                meshData.vertFloats.add(tangent.z);

                // calculate bitangent from normal x tangent
                Vector3 bitangent = bitangents.get(i);
                meshData.vertFloats.add(bitangent.x);
                meshData.vertFloats.add(bitangent.y);
                meshData.vertFloats.add(bitangent.z);
            }
        }

        return new Mesh(meshData);
    }

    private static class Vertex {
        Vector3 position;
        Vector3 normal;
        Vector2 uv;
    }

    private void addTBN( final MeshData meshData,
                                final ArrayList<Vector3>positions, final ArrayList<Vector2>textureCoordinates,
                                final ArrayList<Vector3>normals,
                                ArrayList<Vector3>tangents,
                                ArrayList<Vector3>bitangents){

        // add tangent and bitangent to vertices of each triangle
        Vector3 T = new Vector3();
        Vector3 B = new Vector3();
        Vertex[] corners = new Vertex[3];
        for(int i= 0; i < 3; i++)
            corners[i] = new Vertex();

        for (int j = 0; j < meshData.indexValues.size(); j+= 3) {   // for each triangle
            for(int i= 0; i < 3; i++) {                 // for each corner
                int index = meshData.indexValues.get(i);        // assuming we use an indexed mesh

                corners[i].position = positions.get(index);
                corners[i].normal = normals.get(index);
                corners[i].uv = textureCoordinates.get(index);
            }

            calculateBTN(corners, T, B);

            for(int i= 0; i < 3; i++) {
                tangents.add(T);
                bitangents.add(B);
            }
        }
    }

    private static Vector3 Ntmp = new Vector3();
    private static Vector3 N = new Vector3();

    private static Vector3 edge1 = new Vector3();
    private static Vector3 edge2 = new Vector3();
    private static Vector2 eUV1 = new Vector2();
    private static Vector2 eUV2 = new Vector2();



    private static void calculateBTN(Vertex corners[], Vector3 T, Vector3 B) {
        edge1.set(corners[1].position).sub(corners[0].position);
        edge2.set(corners[2].position).sub(corners[0].position);

        eUV1.set(corners[1].uv).sub(corners[0].uv);
        eUV2.set(corners[2].uv).sub(corners[0].uv);

        T.set(edge1.cpy().scl(eUV2.y).sub(edge2.cpy().scl(eUV1.y)));
        B.set(edge2.cpy().scl(eUV1.x).sub(edge1.cpy().scl(eUV2.x)));
        T.scl(-1);
        B.scl(-1);
        N.set(T).crs(B);

        // average normal
        Ntmp.set(corners[0].normal).add(corners[1].normal).add(corners[2].normal).scl(1/3f);

//        if(Ntmp.dot(N) < 0){
//            T.scl(-1);
//            B.scl(-1);
//        }

        float dot = T.dot(Ntmp);
        T.sub(Ntmp.cpy().scl(dot));
        T.nor();
        // T = normalize(T - dot(T, N) * N);
        //B = cross(N,T);
        B.set(Ntmp).crs(T);
    }



    private void readObj(String filePath) {

        // todo fix if obj has no normal map we dont need tangent and bitangent and we should also not add this in vertex buffer
        ArrayList<MaterialData> mtlData = new ArrayList<>();
        MeshData meshData = ObjLoader.load(filePath, mtlData);

        Mesh mesh = new Mesh(meshData);
        meshes.add(mesh);



        //System.out.println("Loaded "+meshData.objectName);

        // create a meshPart to cover whole mesh (temp)
        MeshPart meshPart;
        if(mesh.getIndexCount() > 0)
            meshPart = new MeshPart(mesh, "part", WGPUPrimitiveTopology.TriangleList, 0, mesh.getIndexCount());
        else
            meshPart = new MeshPart(mesh, "part", WGPUPrimitiveTopology.TriangleList, 0, mesh.getVertexCount());

        for(MaterialData mtl: mtlData) {
            Material material = new Material(mtl);
            materials.add(material);
        }

        rootNodes = new ArrayList<>();
        Node rootNode = new Node();
        rootNode.nodeParts = new ArrayList<>();
        Material material;
        if(materials.size() > 0)
            material = materials.get(0);    // todo abritrary
        else
            material = new Material( new Color(1,1,1,1));   // fallback
        rootNode.nodeParts.add( new NodePart(meshPart, material ));
        rootNodes.add(rootNode);

    }

    @Override
    public void dispose() {
        if(ownsMeshes)
            for(Mesh mesh: meshes)
                mesh.dispose();
        for(Material material: materials)
            material.dispose();
//        for(Node node: rootNodes)
//            node.dispose();
    }
}
