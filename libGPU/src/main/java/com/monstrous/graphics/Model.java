package com.monstrous.graphics;

import com.monstrous.graphics.loaders.*;
import com.monstrous.graphics.loaders.gltf.GLTFAccessor;
import com.monstrous.graphics.loaders.gltf.GLTFAttribute;
import com.monstrous.graphics.loaders.gltf.GLTFBufferView;
import com.monstrous.math.Vector2;
import com.monstrous.math.Vector3;
import com.monstrous.utils.Disposable;
import com.monstrous.wgpu.WGPUVertexFormat;

import java.util.ArrayList;

// A model combines meshpart(s) and material
public class Model implements Disposable {
    public String filePath;
    public Mesh mesh;
    public NodePart nodePart;
    public Material material;

    public Model(String filePath) {
        this.filePath = filePath.toLowerCase();

        if (this.filePath.endsWith("obj")) {
            readObj(filePath);
        } else if (this.filePath.endsWith("gltf")) {
            readGLTF(filePath);
        } else
            throw new RuntimeException("Model: file name extension not supported : "+filePath);

    }

    private void readGLTF(String filePath) {
        GLTF gltf = GLTFLoader.load(filePath);      // TMP
        GLTFRawBuffer rawBuffer = new GLTFRawBuffer(gltf.buffers.getFirst().uri);           // assume 1 buffer

        int indexAccessorId = gltf.meshes.getFirst().primitives.getFirst().indices; // assume 1 mesh & 1 primitive
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
            System.out.println("index "+index);
            meshData.indexValues.add(index);
        }

        int positionAccessorId = -1;
        int uvAccessorId = -1;
        ArrayList<GLTFAttribute> attributes = gltf.meshes.getFirst().primitives.getFirst().attributes;
        for(GLTFAttribute attribute : attributes){
            if(attribute.name.contentEquals("POSITION")){
                positionAccessorId = attribute.value;
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
            System.out.println("float  "+f1 + " "+ f2 + " "+f3);
            positions.add(new Vector3(f1, f2, f3));
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
            System.out.println("float  "+f1 + " "+ f2 );
            textureCoordinates.add(new Vector2(f1, f2));
        }

        // x y z nx ny nz r g b u v
        meshData.vertSize = 11; // in floats
        meshData.objectName = gltf.nodes.getFirst().name;

        for(int i = 0; i < positions.size(); i++){
            Vector3 pos = positions.get(i);
            meshData.vertFloats.add(pos.x);
            meshData.vertFloats.add(pos.y);
            meshData.vertFloats.add(pos.z);

            meshData.vertFloats.add(0f);
            meshData.vertFloats.add(0f);
            meshData.vertFloats.add(0f);

            meshData.vertFloats.add(0f);
            meshData.vertFloats.add(0f);
            meshData.vertFloats.add(0f);

            Vector2 uv = textureCoordinates.get(i);
            meshData.vertFloats.add(uv.x);
            meshData.vertFloats.add(uv.y);
            //System.out.println("uv float  "+uv.x + " "+ uv.y );

        }

        MaterialData mat = new MaterialData();
        mat.diffuseMapFilePath = gltf.images.getFirst().uri;
        meshData.materialData = mat;

        meshData.vertexAttributes = new VertexAttributes();
        meshData.vertexAttributes.add("position", WGPUVertexFormat.Float32x3, 0);
        meshData.vertexAttributes.add("normal", WGPUVertexFormat.Float32x3, 1);
        meshData.vertexAttributes.add("color", WGPUVertexFormat.Float32x3, 2);
        meshData.vertexAttributes.add("uv", WGPUVertexFormat.Float32x2, 3);
        meshData.vertexAttributes.end();

        mesh = new Mesh(meshData);

        System.out.println("Loaded "+meshData.objectName);

        // create a meshPart to cover whole mesh (temp)
        MeshPart meshPart;
        if(mesh.getIndexCount() > 0)
            meshPart = new MeshPart(mesh, 0, mesh.getIndexCount());
        else
            meshPart = new MeshPart(mesh, 0, mesh.getVertexCount());

        material = new Material(meshData.materialData);

        nodePart = new NodePart(meshPart, material);


    }

    private void readObj(String filePath) {

        // todo fix if obj has no normal map we dont need tangent and bitangent and we should also not add this in vertex buffer

        MeshData meshData = ObjLoader.load(filePath);
        meshData.vertexAttributes = new VertexAttributes();
        meshData.vertexAttributes.add("position", WGPUVertexFormat.Float32x3, 0);
        meshData.vertexAttributes.add("tangent", WGPUVertexFormat.Float32x3, 1);
        meshData.vertexAttributes.add("bitangent", WGPUVertexFormat.Float32x3, 2);
        meshData.vertexAttributes.add("normal", WGPUVertexFormat.Float32x3, 3);
        meshData.vertexAttributes.add("color", WGPUVertexFormat.Float32x3, 4);
        meshData.vertexAttributes.add("uv", WGPUVertexFormat.Float32x2, 5);
        meshData.vertexAttributes.end();
        meshData.vertexAttributes.hasNormalMap = meshData.materialData != null && meshData.materialData.normalMapFilePath != null;

        mesh = new Mesh(meshData);



        System.out.println("Loaded "+meshData.objectName);

        // create a meshPart to cover whole mesh (temp)
        MeshPart meshPart;
        if(mesh.getIndexCount() > 0)
            meshPart = new MeshPart(mesh, 0, mesh.getIndexCount());
        else
            meshPart = new MeshPart(mesh, 0, mesh.getVertexCount());

        material = new Material(meshData.materialData);

        nodePart = new NodePart(meshPart, material);

    }

    @Override
    public void dispose() {
        mesh.dispose();
        material.dispose();
    }
}
