package com.monstrous.graphics.loaders;

import com.monstrous.FileInput;
import com.monstrous.math.Vector2;
import com.monstrous.math.Vector3;

import java.util.ArrayList;


//
//@location(0) position: vec3f,
//@location(1) tangent: vec3f,
//@location(2) bitangent: vec3f,
//@location(3) normal: vec3f,
//@location(4) color: vec3f,
//@location(5) uv: vec2f,

public class ObjLoader {

    static class Vertex {
        Vector3 position;
        Vector3 normal;
        Vector2 uv;
    }

    public static MeshData load(String filePath) {
        int slash = filePath.lastIndexOf('/');
        String path = filePath.substring(0,slash+1);
        String name = filePath.substring(slash+1);
        MaterialData materialData = null;

        FileInput input = new FileInput(filePath);
        // x y z tx ty tz bx by bz nx ny nz r g b u v
        int vertSize = 17; // in floats
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

                    vertFloats.add(0f); // placeholder for T
                    vertFloats.add(0f);
                    vertFloats.add(0f);

                    vertFloats.add(0f); // placeholder for B
                    vertFloats.add(0f);
                    vertFloats.add(0f);

                    if(indices.length > 1) {
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

                    // dummy color
                    vertFloats.add(0f);
                    vertFloats.add(0f);
                    vertFloats.add(0f);

                    if(indices.length > 2) {
                        int uvindex = Integer.parseInt(indices[1]) - 1;
                        Vector2 tc = uv.get(uvindex);
                        vertFloats.add(tc.x);
                        vertFloats.add(1.0f - tc.y);
                    } else {
                        vertFloats.add(0f);
                        vertFloats.add(0f);
                    }
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
                materialData = MtlLoader.load(path + materialFileName);
            } else
                continue;   // ignore
        }

        MeshData data = new MeshData();
        data.vertSize = vertSize;
        data.vertFloats = vertFloats;
        data.indexValues = indexValues;
        data.objectName = name;
        data.materialData = materialData;

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

        for(int tri = 0; tri < data.indexValues.size(); tri += 3) {
            for (int j = 0; j < 3; j++) {
                int index = data.indexValues.get(tri+j);
                corners[j].position.x = data.vertFloats.get(index * data.vertSize + 0);
                corners[j].position.y = data.vertFloats.get(index * data.vertSize + 1);
                corners[j].position.z = data.vertFloats.get(index * data.vertSize + 2);

                corners[j].normal.x = data.vertFloats.get(index * data.vertSize + 9);
                corners[j].normal.y = data.vertFloats.get(index * data.vertSize + 10);
                corners[j].normal.z = data.vertFloats.get(index * data.vertSize + 11);

                corners[j].uv.x = data.vertFloats.get(index * data.vertSize + 15);
                corners[j].uv.y = data.vertFloats.get(index * data.vertSize + 16);
            }
            calculateBTN(corners, T, B);

            for (int j = 0; j < 3; j++) {
                int index = data.indexValues.get(tri+j);
                data.vertFloats.set(index*data.vertSize + 3, T.x);
                data.vertFloats.set(index*data.vertSize + 4, T.y);
                data.vertFloats.set(index*data.vertSize + 5, T.z);

                data.vertFloats.set(index*data.vertSize + 6, B.x);
                data.vertFloats.set(index*data.vertSize + 7, B.y);
                data.vertFloats.set(index*data.vertSize + 8, B.z);
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
