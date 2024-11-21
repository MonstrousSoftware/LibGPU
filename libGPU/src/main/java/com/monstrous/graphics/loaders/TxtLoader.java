package com.monstrous.graphics.loaders;

import com.monstrous.FileInput;

import java.util.ArrayList;

// loader of ad hoc txt format from Learn WebGPU for C++

public class TxtLoader {


    public static MeshData load(String fileName) {
        int dimensions = 3;
        FileInput input = new FileInput(fileName);
        // x y z nx ny nz r g b u v
        int vertSize = 8 + dimensions; // in floats
        ArrayList<Integer> indexValues = new ArrayList<>();
        ArrayList<Float> vertFloats = new ArrayList<>();
        int mode = 0;
        for (int lineNr = 0; lineNr < input.size(); lineNr++) {
            String line = input.get(lineNr).strip();
            if (line.contentEquals("v")) {
                mode = 1;
                continue;
            }
            if (line.contentEquals("[indices]")) {
                mode = 2;
                continue;
            }
            if (line.startsWith("#"))
                continue;
            if (line.isEmpty())
                continue;
            if (mode == 1) {
                String[] words = line.split("[ \t]+");
                if (words.length != vertSize)
                    System.out.println("Expected " + vertSize + " floats per vertex : " + line);
                for (int i = 0; i < vertSize; i++)
                    vertFloats.add(Float.parseFloat(words[i]));
            } else if (mode == 2) {
                String[] words = line.split("[ \t]+");
                if (words.length != 3)
                    System.out.println("Expected 3 indices per line: " + line);
                for (int i = 0; i < 3; i++)
                    indexValues.add(Integer.parseInt(words[i]));
            } else {
                System.out.println("Unexpected input: " + line);
            }
        }
        MeshData data = new MeshData();
        data.vertFloats = vertFloats;
        data.indexValues = indexValues;
        return data;
    }



}
