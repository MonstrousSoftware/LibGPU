package com.monstrous.graphics.loaders;

import com.monstrous.Files;
import com.monstrous.graphics.Material;
import com.monstrous.graphics.VertexAttribute;
import com.monstrous.graphics.VertexAttributes;
import com.monstrous.graphics.g3d.*;
import com.monstrous.graphics.loaders.gltf.*;
import com.monstrous.math.Quaternion;
import com.monstrous.math.Vector2;
import com.monstrous.math.Vector3;
import com.monstrous.webgpu.WGPUPrimitiveTopology;
import com.monstrous.webgpu.WGPUVertexFormat;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GLTFLoader implements ModelLoader {

    private final Map<GLTFPrimitive, Mesh> meshMap = new HashMap<>();
    private final ArrayList<Material> materials = new ArrayList<>();
    //private final Map<Integer, Boolean> hasNormalMap = new HashMap<>();
    private final ArrayList<Node> nodes = new ArrayList<>();

    @Override
    public Model loadFromFile(Model model, String filePath) {

        GLTF gltf = GLTFParser.load(filePath);      // TMP
        return load(model, gltf);
    }

    public Model load(Model model, GLTF gltf){
        meshMap.clear();
        materials.clear();


        long startLoad = System.currentTimeMillis();
        int index = 0;
        for(GLTFMaterial gltfMat :  gltf.materials){
            MaterialData materialData = new MaterialData();

            materialData.name = gltfMat.name != null ? gltfMat.name : "mat"+index;   // copy name or generate one as a debugging aid
            index++;

            if(gltfMat.pbrMetallicRoughness.baseColorFactor != null)
                materialData.diffuse = gltfMat.pbrMetallicRoughness.baseColorFactor;
            if(gltfMat.pbrMetallicRoughness.roughnessFactor >= 0)
                materialData.roughnessFactor = gltfMat.pbrMetallicRoughness.roughnessFactor;
            if(gltfMat.pbrMetallicRoughness.metallicFactor >= 0)
                materialData.metallicFactor = gltfMat.pbrMetallicRoughness.metallicFactor;
            if(gltfMat.pbrMetallicRoughness.baseColorTexture >= 0)
                materialData.diffuseMapData = readImageData(gltf, gltfMat.pbrMetallicRoughness.baseColorTexture);
            if(gltfMat.pbrMetallicRoughness.metallicRoughnessTexture >= 0)
                materialData.metallicRoughnessMapData = readImageData(gltf, gltfMat.pbrMetallicRoughness.metallicRoughnessTexture);
            if(gltfMat.normalTexture >= 0)
                materialData.normalMapData = readImageData(gltf, gltfMat.normalTexture);
            if(gltfMat.emissiveTexture >= 0)
                materialData.emissiveMapData =  readImageData(gltf, gltfMat.emissiveTexture);
            if(gltfMat.occlusionTexture >= 0)
                materialData.occlusionMapData =  readImageData(gltf, gltfMat.occlusionTexture);


            Material material = new Material(materialData);
            materials.add(material);
            model.addMaterial(material);
        }

        long endLoad = System.currentTimeMillis();
        System.out.println("Material loading/generation time (ms): "+(endLoad - startLoad));

        startLoad = System.currentTimeMillis();
        for(GLTFMesh gltfMesh : gltf.meshes){
            // build a mesh for each primitive
            for(GLTFPrimitive primitive : gltfMesh.primitives){
                Mesh mesh = buildMesh(gltf,  gltf.rawBuffer, primitive );
                model.addMesh(mesh);
                meshMap.put(primitive, mesh);
            }
        }

        endLoad = System.currentTimeMillis();
        System.out.println("Mesh loading time (ms): "+(endLoad - startLoad));




//        for(GLTFScene scene : gltf.scenes ){
//
//        }

        nodes.clear();
        for( GLTFNode gltfNode : gltf.nodes ) {
            Node node = addNode(gltf, gltfNode);
            nodes.add(node);
        }

        GLTFScene scene = gltf.scenes.get(gltf.scene);
        for( int nodeId : scene.nodes ) {
            GLTFNode gltfNode = gltf.nodes.get(nodeId);
            Node rootNode = nodes.get(nodeId);

            addNodeHierarchy(gltf, gltfNode, rootNode);     // recursively add the node hierarchy
            rootNode.updateMatrices(true);
            model.addNode(rootNode);
        }

        for(GLTFAnimation gltfAnim : gltf.animations ){
            Animation animation = new Animation();
            animation.name = gltfAnim.name;
            float maxDuration = 0f;
            for(GLTFAnimationChannel gltfChannel : gltfAnim.channels){
                NodeAnimation nodeAnimation = new NodeAnimation();
                nodeAnimation.node = nodes.get(gltfChannel.node);

                int numComponents = 3;
                if(gltfChannel.path.contentEquals("rotation"))
                    numComponents = 4; // 4 floats per quaternion

                GLTFAnimationSampler sampler = gltfAnim.samplers.get(gltfChannel.sampler);
                GLTFAccessor inAccessor = gltf.accessors.get(sampler.input);
                GLTFAccessor outAccessor = gltf.accessors.get(sampler.output);
                // ignore interpolation, we only do linear

                GLTFBufferView inView = gltf.bufferViews.get(inAccessor.bufferView);
                if(inView.buffer != 0)
                    throw new RuntimeException("GLTF can only support buffer 0");
                gltf.rawBuffer.byteBuffer.position(inView.byteOffset+ inAccessor.byteOffset);  // does this carry over to floatbuf?
                FloatBuffer timeBuf = gltf.rawBuffer.byteBuffer.asFloatBuffer();
                float[] times = new float[inAccessor.count];
                timeBuf.get(times, 0, inAccessor.count);

                GLTFBufferView outView = gltf.bufferViews.get(outAccessor.bufferView);
                if(outView.buffer != 0)
                    throw new RuntimeException("GLTF can only support buffer 0");
                gltf.rawBuffer.byteBuffer.position(outView.byteOffset+outAccessor.byteOffset);  // does this carry over to floatbuf?
                FloatBuffer floatBuf = gltf.rawBuffer.byteBuffer.asFloatBuffer();
                float[] floats = new float[numComponents * outAccessor.count];
                floatBuf.get(floats, 0, numComponents * outAccessor.count);


                for(int key = 0; key < inAccessor.count; key++){
                    float time = times[key];
                    if(gltfChannel.path.contentEquals("translation")) {
                        Vector3 tr = new Vector3(floats[3 * key], floats[3*key + 1], floats[3*key + 2]);
                        NodeKeyframe<Vector3> keyFrame = new NodeKeyframe<Vector3>(time, tr);
                        nodeAnimation.addTranslation(keyFrame);
                    } else  if(gltfChannel.path.contentEquals("rotation")) {
                        Quaternion q = new Quaternion(floats[4 * key], floats[key * 4 + 1], floats[key * 4 + 2], floats[key * 4 + 3]);
                        NodeKeyframe<Quaternion> keyFrame = new NodeKeyframe<Quaternion>(time, q);
                        nodeAnimation.addRotation(keyFrame);
                    } else if(gltfChannel.path.contentEquals("scale")) {
                        Vector3 tr = new Vector3(floats[3 * key], floats[3*key + 1], floats[3*key + 2]);
                        NodeKeyframe<Vector3> keyFrame = new NodeKeyframe<Vector3>(time, tr);
                        nodeAnimation.addScaling(keyFrame);
                    }
                }
                maxDuration = Math.max(maxDuration,times[inAccessor.count-1]);
                animation.addNodeAnimation(nodeAnimation);
            }
            animation.duration = maxDuration;

            model.addAnimation(animation);
        }

        //System.out.println("loaded "+filePath);
        return model;
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
        return node;
    }

    private void addNodeHierarchy(GLTF gltf, GLTFNode gltfNode, Node root){
        // now add any children
        for(int j : gltfNode.children ){
            GLTFNode gltfChild = gltf.nodes.get(j);
            Node child = nodes.get(j);
            root.addChild(child);
            addNodeHierarchy(gltf, gltfChild, child);
        }
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
        int vaFlags = VertexAttribute.Usage.POSITION|VertexAttribute.Usage.TEXTURE_COORDINATE|VertexAttribute.Usage.NORMAL;
        if(hasNormalMap)
            vaFlags |= VertexAttribute.Usage.TANGENT|VertexAttribute.Usage.BITANGENT;

        meshData.vertexAttributes = new VertexAttributes(vaFlags);

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


}
