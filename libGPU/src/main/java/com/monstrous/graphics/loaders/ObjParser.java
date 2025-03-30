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

package com.monstrous.graphics.loaders;

import com.monstrous.FileInput;
import com.monstrous.graphics.VertexAttribute;
import com.monstrous.graphics.VertexAttributes;
import com.monstrous.math.Vector2;
import com.monstrous.math.Vector3;
import com.monstrous.webgpu.WGPUVertexFormat;

import java.util.ArrayList;


//@location(0) position: vec3f,
//@location(1) uv: vec2f,
//@location(2) normal: vec3f,
//        #ifdef NORMAL_MAP
//@location(3) tangent: vec3f,
//@location(4) bitangent: vec3f,
//#endif

public class ObjParser {

    static class Vertex {
        Vector3 position;
        Vector3 normal;
        Vector2 uv;
    }

    public static MeshData load(String filePath){
        ArrayList<MaterialData> materials = new ArrayList<>();
        return load(filePath, materials);
    }

    public static MeshData load(String filePath, ArrayList<MaterialData> materials) {
        int slash = filePath.lastIndexOf('/');
        String path = filePath.substring(0,slash+1);
        String name = filePath.substring(slash+1);

        FileInput input = new FileInput(filePath);
        // x y z u v nx ny nz tx ty tz bx by bz
        int vertSize = 14; // in floats
        ArrayList<Integer> indexValues = new ArrayList<>();
        ArrayList<Float> vertFloats = new ArrayList<>();
        ArrayList<Vector3> positions = new ArrayList<>();
        ArrayList<Vector3> normals = new ArrayList<>();
        ArrayList<Vector2> uv = new ArrayList<>();
        int indexOut = 0;
        for (int lineNr = 0; lineNr < input.size(); lineNr++) {
            String line = input.get(lineNr).strip();
            if (line.startsWith("#"))
                continue;
            if (line.isEmpty())
                continue;
            if (line.startsWith("v ")) {
                String[] words = line.split("[ \t]+");
                if (words.length != 4)
                    System.out.println("Expected " + 3 + " floats per vertex : " + line);
                float x = Float.parseFloat(words[1]);
                float y = Float.parseFloat(words[2]);
                float z = Float.parseFloat(words[3]);
                positions.add( new Vector3(x,y,z));
             } else if (line.startsWith("vn ")) {
                String[] words = line.split("[ \t]+");
                if (words.length != 4)
                    System.out.println("Expected " + 3 + " floats per vn : " + line);
                float x = Float.parseFloat(words[1]);
                float y = Float.parseFloat(words[2]);
                float z = Float.parseFloat(words[3]);
                normals.add( new Vector3(x,y,z));
            } else if (line.startsWith("vt ")) {
                String[] words = line.split("[ \t]+");
                if (words.length != 3)
                    System.out.println("Expected " + 2 + " floats per vt : " + line);
                float x = Float.parseFloat(words[1]);
                float y = Float.parseFloat(words[2]);
                uv.add( new Vector2(x,y));
            } else if (line.startsWith("f ")) {
                // f v1[/vt1][/vn1] v2[/vt2][/vn2] v3[/vt3][/vn3] ...
                String[] faces = line.split("[ \t]+");
                if (faces.length != 4 && faces.length != 5)
                    System.out.println("Expected 3 or 4 indices per face: " + line);
                for(int i = 1; i < faces.length; i++) {
                    String face = faces[i];
                    String[] indices = face.split("/");
                    //indexValues.add(Integer.parseInt(indices[0])-1);
                    int pindex = Integer.parseInt(indices[0])-1;
                    Vector3 v = positions.get(pindex);
                    vertFloats.add(v.x);
                    vertFloats.add(v.y);
                    vertFloats.add(v.z);

                    if(indices.length > 2 && indices[1].length()>0) {
                        int uvindex = Integer.parseInt(indices[1]) - 1;
                        Vector2 tc = uv.get(uvindex);
                        vertFloats.add(tc.x);
                        vertFloats.add(1.0f - tc.y);
                    } else {
                        vertFloats.add(0f);
                        vertFloats.add(0f);
                    }

                    if(indices.length > 3 && indices[2].length()>0) {
                        int nindex = Integer.parseInt(indices[2]) - 1;
                        Vector3 vn = normals.get(nindex);
                        vertFloats.add(vn.x);
                        vertFloats.add(vn.y);
                        vertFloats.add(vn.z);
                    } else {
                        // dummy normal
                        vertFloats.add(0f);
                        vertFloats.add(0f);
                        vertFloats.add(0f);
                    }

                    vertFloats.add(0f); // placeholder for T
                    vertFloats.add(0f);
                    vertFloats.add(0f);

                    vertFloats.add(0f); // placeholder for B
                    vertFloats.add(0f);
                    vertFloats.add(0f);
                }
                if(faces.length == 4){  // triangle
                    indexValues.add(indexOut++);
                    indexValues.add(indexOut++);
                    indexValues.add(indexOut++);
                } else { // quad
                    indexValues.add(indexOut);      // triangle
                    indexValues.add(indexOut+1);
                    indexValues.add(indexOut+3);

                    indexValues.add(indexOut+1);    // triangle
                    indexValues.add(indexOut+2);
                    indexValues.add(indexOut+3);
                    indexOut += 4;
                }
            } else if (line.startsWith("o ")) {
                String[] words = line.split("[ \t]+");
                name = words[1];
            } else if (line.startsWith("mtllib ")) {
                String[] words = line.split("[ \t]+");
                String materialFileName = words[1];
                MaterialData materialData = MtlLoader.load(path + materialFileName);
                materials.add(materialData);
            } else
                continue;   // ignore
        }

        MeshData data = new MeshData();
        data.vertFloats = vertFloats;
        data.indexValues = indexValues;
        data.objectName = name;

        data.vertexAttributes = new VertexAttributes();
        data.vertexAttributes.add(VertexAttribute.Usage.POSITION, "position", WGPUVertexFormat.Float32x3, 0);
        data.vertexAttributes.add(VertexAttribute.Usage.TEXTURE_COORDINATE, "uv", WGPUVertexFormat.Float32x2, 1);
        data.vertexAttributes.add(VertexAttribute.Usage.NORMAL, "normal", WGPUVertexFormat.Float32x3, 2);
        data.vertexAttributes.add(VertexAttribute.Usage.TANGENT, "tangent", WGPUVertexFormat.Float32x3, 3);
        data.vertexAttributes.add(VertexAttribute.Usage.BITANGENT, "bitangent", WGPUVertexFormat.Float32x3, 4);


        data.vertexAttributes.end();
        //meshData.vertexAttributes.hasNormalMap = meshData.materialData != null && meshData.materialData.normalMapFilePath != null;
        data.indexSizeInBytes = 4;

        addTBN(data);
        return data;
    }

    private static void addTBN( MeshData data ){
        // add tangent and bitangent to vertices of each triangle
        Vertex[] corners = new Vertex[3];
        for (int j = 0; j < 3; j++) {
            corners[j] = new Vertex();
            corners[j].position = new Vector3();
            corners[j].normal = new Vector3();
            corners[j].uv = new Vector2();
        }

        Vector3 T = new Vector3();
        Vector3 B = new Vector3();

        int vertSize = data.vertexAttributes.getVertexSizeInBytes()/Float.BYTES;

        // x y z u v nx ny nz tx ty tz bx by bz
        // 0 1 2 3 4 5  6  7  8  9  10 11 12 13

        for(int tri = 0; tri < data.indexValues.size(); tri += 3) {
            for (int j = 0; j < 3; j++) {
                int index = data.indexValues.get(tri+j);
                corners[j].position.x = data.vertFloats.get(index * vertSize + 0);
                corners[j].position.y = data.vertFloats.get(index * vertSize + 1);
                corners[j].position.z = data.vertFloats.get(index * vertSize + 2);

                corners[j].normal.x = data.vertFloats.get(index * vertSize + 6);
                corners[j].normal.y = data.vertFloats.get(index * vertSize + 7);
                corners[j].normal.z = data.vertFloats.get(index * vertSize + 8);

                corners[j].uv.x = data.vertFloats.get(index * vertSize + 3);
                corners[j].uv.y = data.vertFloats.get(index * vertSize + 4);
            }
            calculateBTN(corners, T, B);

            for (int j = 0; j < 3; j++) {
                int index = data.indexValues.get(tri+j);
                data.vertFloats.set(index*vertSize + 7, T.x);
                data.vertFloats.set(index*vertSize + 8, T.y);
                data.vertFloats.set(index*vertSize + 9, T.z);

                data.vertFloats.set(index*vertSize + 10, B.x);
                data.vertFloats.set(index*vertSize + 11, B.y);
                data.vertFloats.set(index*vertSize + 12, B.z);
            }
        }
    }

    private static Vector3 Ntmp = new Vector3();
    private static Vector3 N = new Vector3();

    private static void calculateBTN(Vertex corners[], Vector3 T, Vector3 B) {
        Vector3 edge1 = corners[1].position.sub(corners[0].position);
        Vector3 edge2 = corners[2].position.sub(corners[0].position);

        Vector2 eUV1 = corners[1].uv.sub(corners[0].uv);
        Vector2 eUV2 = corners[2].uv.sub(corners[0].uv);

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
