package com.monstrous.graphics.loaders;

import com.monstrous.FileInput;
import com.monstrous.graphics.Color;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

// Read .mtl file related to the .obj format

//        newmtl Pallette
//        Ns 712.387333
//        Ka 1.000000 1.000000 1.000000
//        Kd 0.800000 0.800000 0.800000
//        Ks 0.268293 0.268293 0.268293
//        Ke 0.000000 0.000000 0.000000
//        Ni 1.450000
//        d 0.632850
//        illum 9
//        map_Kd C:\\Coding\\Blender Files\\palette.png
//        map_Bump models\fourareen2K_normals.png

public class MtlLoader {

    public static MaterialData load(String fileName) {
        String name = fileName;
        MaterialData material = new MaterialData();

        FileInput input = new FileInput(fileName);

        for (int lineNr = 0; lineNr < input.size(); lineNr++) {
            String line = input.get(lineNr).strip();
            if (line.startsWith("#"))
                continue;
            if (line.isEmpty())
                continue;
            if (line.startsWith("K")) {
                String[] words = line.split("[ \t]+");
                if (words.length != 4)
                    System.out.println("Expected " + 3 + " floats per color : " + line);
                float r = Float.parseFloat(words[1]);
                float g = Float.parseFloat(words[2]);
                float b = Float.parseFloat(words[3]);
                Color col = new Color(r, g, b, 1f);
                if(words[0].contentEquals("Ka")){
                    material.ambient = col;
                } else if(words[0].contentEquals("Kd")){
                    material.diffuse = col;
                } else if(words[0].contentEquals("Ks")){
                    material.specular = col;
                } else if(words[0].contentEquals("Ke")){
                    material.emissive = col;
                }
            } else if (line.startsWith("d")) {
                String[] words = line.split("[ \t]+");
                float d = Float.parseFloat(words[1]);
                material.transparency = d;
            } else if (line.startsWith("illum")) {
                String[] words = line.split("[ \t]+");
                material.illuminationModel = Integer.parseInt(words[1]);
           } else if (line.startsWith("map_Kd")) {
                material.diffuseMapData = readImageData( line.substring(7).trim() );
            } else if (line.startsWith("map_Bump")) {                                       // note: also map_Kn or norm are in use
                material.normalMapData = readImageData( line.substring(9).trim() );
            }else if (line.startsWith("newmtl")) {
                String[] words = line.split("[ \t]+");
                material.name = words[1];
            }
        }
        return material;
    }

    private static byte[] readImageData(String fileName )  {
            try {
                return Files.readAllBytes(Paths.get(fileName));
            } catch (IOException e) {
                throw new RuntimeException("Texture file not found: "+fileName);
            }
    }
}
