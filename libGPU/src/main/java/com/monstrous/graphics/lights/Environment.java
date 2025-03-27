package com.monstrous.graphics.lights;

import com.monstrous.graphics.Camera;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.g3d.SkyBox;

import java.util.ArrayList;

public class Environment {
    public ArrayList<Light> lights;
    public float ambientLightLevel;         // 0 .. 1
    public Texture cubeMap;                 // 6 layered texture
    public Texture irradianceMap;           // 6 layered texture
    public Texture radianceMap;             // 6 layered texture with LOD levels
    public Texture brdfLUT;                 // 2d BRDF lookup table
    public SkyBox skybox;
    public boolean useImageBasedLighting;
    public String shaderSourcePath;         // force a shader for model instances

    public boolean depthPass = false;
    public boolean renderShadows = false;
    public Texture shadowMap;               // may be null
    public Camera shadowCamera;             // may be null

    public Environment() {
        lights = new ArrayList<>();
        useImageBasedLighting = false;
    }

    public void add(Light light){
        lights.add(light);
    }

    public void setShadowMap(Camera shadowCamera, Texture map ){
        this.shadowMap = map;
        this.shadowCamera = shadowCamera;
    }

    public void setCubeMap(Texture texture){
        cubeMap = texture;
    }

    public void setBRDFLookUpTable(Texture texture){
        brdfLUT = texture;
    }



    public void setRadianceMap(Texture texture){
        radianceMap = texture;
    }

    public void setIrradianceMap(Texture texture){
        irradianceMap = texture;
    }

    public void setSkybox( SkyBox skybox ){
        this.skybox = skybox;
    }
}
