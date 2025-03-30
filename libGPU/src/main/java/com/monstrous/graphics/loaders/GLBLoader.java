package com.monstrous.graphics.loaders;


import com.monstrous.graphics.g3d.Model;

public class GLBLoader implements ModelLoader {

    @Override
    public Model loadFromFile(Model model, String filePath) {

        GLTF gltf = GLBParser.load(filePath);
        GLTFLoader loader = new GLTFLoader();
        return loader.load(model, gltf);
    }

}
