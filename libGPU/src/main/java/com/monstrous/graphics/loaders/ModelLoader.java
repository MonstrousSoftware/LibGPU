package com.monstrous.graphics.loaders;

import com.monstrous.graphics.g3d.Model;

public interface ModelLoader {

    public Model loadFromFile(Model model, String filePath);

}
