package com.monstrous.graphics;

import com.monstrous.graphics.loaders.*;
import com.monstrous.graphics.loaders.gltf.*;
import com.monstrous.math.Vector2;
import com.monstrous.math.Vector3;
import com.monstrous.utils.Disposable;
import com.monstrous.wgpu.WGPUVertexFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// A model combines meshpart(s) and material(s)
public class Model implements Disposable {
    public String filePath;
    public ArrayList<Mesh> meshes;
    public Node rootNode;
    public ArrayList<Material> materials;
    private Map<GLTFPrimitive, Mesh> meshMap = new HashMap<>();

    public Model(String filePath) {
        this.filePath = filePath.toLowerCase();
        meshes = new ArrayList<>();
        materials = new ArrayList<>();

        // check file extension to choose loader

        if (this.filePath.endsWith("obj")) {
            readObj(filePath);
        } else if (this.filePath.endsWith("gltf")) {
            readGLTF(filePath);
        } else
            throw new RuntimeException("Model: file name extension not supported : "+filePath);

    }


    private void readGLTF(String filePath) {
        meshMap.clear();
        GLTF gltf = GLTFLoader.load(filePath);      // TMP
        GLTFRawBuffer rawBuffer = new GLTFRawBuffer(gltf.buffers.getFirst().uri);           // assume 1 buffer

        for(GLTFMesh gltfMesh : gltf.meshes){
            for(GLTFPrimitive primitive : gltfMesh.primitives){
                Mesh m = loadMesh(gltf, rawBuffer, primitive );
                meshes.add(m);
                meshMap.put(primitive, m);
            }
        }

        ArrayList<MaterialData> mtlData = new ArrayList<>();
        for(GLTFMaterial gltfMat :  gltf.materials){
            MaterialData mat = new MaterialData();
            if(gltfMat.pbrMetallicRoughness.baseColorFactor != null)
                mat.diffuse = gltfMat.pbrMetallicRoughness.baseColorFactor;
            if(gltfMat.pbrMetallicRoughness.baseColorTexture >= 0)
                mat.diffuseMapFilePath = gltf.images.get( gltf.textures.get(gltfMat.pbrMetallicRoughness.baseColorTexture).source).uri;
            mtlData.add(mat);
        }

        for(MaterialData mtl: mtlData) {
            Material material = new Material(mtl);
            materials.add(material);
        }

//        for(GLTFScene scene : gltf.scenes ){
//
//        }
        GLTFScene scene = gltf.scenes.get(gltf.scene);
        int nodeId = scene.nodes.getFirst();
        GLTFNode gltfNode = gltf.nodes.get(nodeId);

        rootNode = addNode(gltf, gltfNode);     // recursively add the node hierarchy
        rootNode.updateMatrices(true);
        System.out.println("loaded "+filePath);
    }

    private Node addNode(GLTF gltf, GLTFNode gltfNode){
        Node node = new Node();
        node.name = gltfNode.name;

        // optional transforms
        if(gltfNode.translation != null)
            node.translation.set(gltfNode.translation);
        if(gltfNode.scale != null)
            node.scale.set(gltfNode.scale);
        if(gltfNode.rotation != null)
            node.rotation.set(gltfNode.rotation);

        if(gltfNode.mesh >= 0){
            node.nodeParts = new ArrayList<>();
            GLTFMesh gltfMesh = gltf.meshes.get(gltfNode.mesh);
            for( GLTFPrimitive primitive : gltfMesh.primitives) {
                GLTFAccessor indexAccessor = gltf.accessors.get(primitive.indices);
                GLTFBufferView view = gltf.bufferViews.get(indexAccessor.bufferView);
                if (!indexAccessor.type.contentEquals("SCALAR"))
                    throw new RuntimeException("GLTF: Expect primitive.indices to refer to SCALAR accessor");

                Mesh m = meshMap.get(primitive);
                MeshPart meshPart = new MeshPart(m, indexAccessor.byteOffset, indexAccessor.count);
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


    private Mesh loadMesh(GLTF gltf, GLTFRawBuffer rawBuffer, GLTFPrimitive primitive){

        int indexAccessorId = primitive.indices;
        GLTFAccessor indexAccessor = gltf.accessors.get(indexAccessorId);
        GLTFBufferView view = gltf.bufferViews.get(indexAccessor.bufferView);
        if(view.buffer != 0)
            throw new RuntimeException("GLTF: Can only support buffer 0");
        int offset = view.byteOffset;
        offset += indexAccessor.byteOffset;

        MeshData meshData = new MeshData();
        if(indexAccessor.componentType != GLTF.USHORT16)
            throw new RuntimeException("GLTF: Can only support short index");

        rawBuffer.byteBuffer.position(offset);
        for(int i = 0; i < indexAccessor.count; i++){
            // assuming ushort
            int index = rawBuffer.byteBuffer.getShort();
            //System.out.println("index "+index);
            meshData.indexValues.add(index);
        }

        int positionAccessorId = -1;
        int normalAccessorId = -1;
        int uvAccessorId = -1;
        ArrayList<GLTFAttribute> attributes = primitive.attributes;
        for(GLTFAttribute attribute : attributes){
            if(attribute.name.contentEquals("POSITION")){
                positionAccessorId = attribute.value;
            }
            if(attribute.name.contentEquals("NORMAL")){
                normalAccessorId = attribute.value;
            }
            if(attribute.name.contentEquals("TEXCOORD_0")){
                uvAccessorId = attribute.value;
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

        System.out.println("Position offset: "+offset);
        System.out.println("Position count: "+positionAccessor.count);

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


        if(uvAccessorId < 0)
            throw new RuntimeException("GLTF: need TEXCOORD_0 attribute");
        GLTFAccessor uvAccessor = gltf.accessors.get(uvAccessorId);
        view = gltf.bufferViews.get(uvAccessor.bufferView);
        if(view.buffer != 0)
            throw new RuntimeException("GLTF: Can only support buffer 0");
        offset = view.byteOffset;
        offset += uvAccessor.byteOffset;

        System.out.println("UV offset: "+offset);

        if(uvAccessor.componentType != GLTF.FLOAT32 || !uvAccessor.type.contentEquals("VEC2"))
            throw new RuntimeException("GLTF: Can only support float positions as VEC2");

        ArrayList<Vector2> textureCoordinates = new ArrayList<>();
        rawBuffer.byteBuffer.position(offset);
        for(int i = 0; i < uvAccessor.count; i++){
            // assuming float32
            float f1 = rawBuffer.byteBuffer.getFloat();
            float f2 = rawBuffer.byteBuffer.getFloat();
            //System.out.println("float  "+f1 + " "+ f2 );
            textureCoordinates.add(new Vector2(f1, f2));
        }

        // x y z nx ny nz u v
        meshData.vertSize = 11; // in floats
        meshData.objectName = gltf.nodes.getFirst().name;

        for(int i = 0; i < positions.size(); i++){
            Vector3 pos = positions.get(i);
            meshData.vertFloats.add(pos.x);
            meshData.vertFloats.add(pos.y);
            meshData.vertFloats.add(pos.z);

            if(normals.size() == 0){
                meshData.vertFloats.add(0f);
                meshData.vertFloats.add(0f);
                meshData.vertFloats.add(0f);
            } else {
                Vector3 normal = normals.get(i);
                meshData.vertFloats.add(normal.x);
                meshData.vertFloats.add(normal.y);
                meshData.vertFloats.add(normal.z);
            }

            Vector2 uv = textureCoordinates.get(i);
            meshData.vertFloats.add(uv.x);
            meshData.vertFloats.add(uv.y);
            //System.out.println("uv float  "+uv.x + " "+ uv.y );

        }

        // todo adjust this based on the file contents:
        meshData.vertexAttributes = new VertexAttributes();
        meshData.vertexAttributes.add("position", WGPUVertexFormat.Float32x3, 0);
        meshData.vertexAttributes.add("normal", WGPUVertexFormat.Float32x3, 1);
        meshData.vertexAttributes.add("uv", WGPUVertexFormat.Float32x2, 2);
        meshData.vertexAttributes.end();

        meshData.indexSize = 2; // 16 bit index
        return new Mesh(meshData);
    }


    private void readObj(String filePath) {

        // todo fix if obj has no normal map we dont need tangent and bitangent and we should also not add this in vertex buffer
        ArrayList<MaterialData> mtlData = new ArrayList<>();
        MeshData meshData = ObjLoader.load(filePath, mtlData);
        meshData.vertexAttributes = new VertexAttributes();
        meshData.vertexAttributes.add("position", WGPUVertexFormat.Float32x3, 0);
        meshData.vertexAttributes.add("tangent", WGPUVertexFormat.Float32x3, 1);
        meshData.vertexAttributes.add("bitangent", WGPUVertexFormat.Float32x3, 2);
        meshData.vertexAttributes.add("normal", WGPUVertexFormat.Float32x3, 3);
        meshData.vertexAttributes.add("uv", WGPUVertexFormat.Float32x2, 4);
        meshData.vertexAttributes.end();
        //meshData.vertexAttributes.hasNormalMap = meshData.materialData != null && meshData.materialData.normalMapFilePath != null;
        meshData.indexSize = 4;
        Mesh mesh = new Mesh(meshData);
        meshes.add(mesh);



        System.out.println("Loaded "+meshData.objectName);

        // create a meshPart to cover whole mesh (temp)
        MeshPart meshPart;
        if(mesh.getIndexCount() > 0)
            meshPart = new MeshPart(mesh, 0, mesh.getIndexCount());
        else
            meshPart = new MeshPart(mesh, 0, mesh.getVertexCount());

        for(MaterialData mtl: mtlData) {
            Material material = new Material(mtl);
            materials.add(material);
        }

        rootNode = new Node();
        rootNode.nodeParts = new ArrayList<>();
        rootNode.nodeParts.add( new NodePart(meshPart, materials.getFirst()) );       // todo arbitrary choice for first material

    }

    @Override
    public void dispose() {
        for(Mesh mesh: meshes)
            mesh.dispose();
        for(Material material: materials)
            material.dispose();
    }
}
