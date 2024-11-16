package com.monstrous.graphics.loaders;

import com.monstrous.FileInput;
import com.monstrous.math.Vector2;
import com.monstrous.math.Vector3;

import java.util.ArrayList;

public class ObjLoader {

    public static MeshData load(String filePath) {
        int slash = filePath.lastIndexOf('/');
        String path = filePath.substring(0,slash+1);
        String name = filePath.substring(slash+1);
        MaterialData materialData = null;

        int dimensions = 3;
        FileInput input = new FileInput(filePath);
        // x y z nx ny nz r g b u v
        int vertSize = 8 + dimensions; // in floats
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
                    int nindex = Integer.parseInt(indices[2])-1;
                    Vector3 vn = positions.get(nindex);
                    vertFloats.add(vn.x);
                    vertFloats.add(vn.y);
                    vertFloats.add(vn.z);

                    // dummy color
                    vertFloats.add(0f);
                    vertFloats.add(0f);
                    vertFloats.add(0f);

                    int uvindex = Integer.parseInt(indices[1])-1;
                    Vector2 tc = uv.get(uvindex);
                    vertFloats.add(tc.x);
                    vertFloats.add(1.0f-tc.y);
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
        return data;
    }

}
