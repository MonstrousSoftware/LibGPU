package com.monstrous.graphics;

import com.monstrous.LibGPU;
import com.monstrous.wgpu.WGPUSType;
import com.monstrous.wgpu.WGPUShaderModuleDescriptor;
import com.monstrous.wgpu.WGPUShaderModuleWGSLDescriptor;
import jnr.ffi.Pointer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ShaderProgram {


    private String name;
    private String shaderSource;
    private Pointer shaderModule;

    public ShaderProgram(String filePath) {
        String source = null;
        try {
            source = Files.readString(Paths.get(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        compile(filePath, source);
    }

    public ShaderProgram(String name, String shaderSource){
         compile(name, shaderSource);
    }

    private void compile(String name, String shaderSource){
        this.name = name;
        this.shaderSource = shaderSource;

        // Create Shader Module
        WGPUShaderModuleDescriptor shaderDesc = WGPUShaderModuleDescriptor.createDirect();
            shaderDesc.setLabel(name);

        WGPUShaderModuleWGSLDescriptor shaderCodeDesc = WGPUShaderModuleWGSLDescriptor.createDirect();
            shaderCodeDesc.getChain().setNext();
            shaderCodeDesc.getChain().setSType(WGPUSType.ShaderModuleWGSLDescriptor);
            shaderCodeDesc.setCode(shaderSource);

            shaderDesc.getNextInChain().set(shaderCodeDesc.getPointerTo());

        shaderModule = LibGPU.wgpu.DeviceCreateShaderModule(LibGPU.device, shaderDesc);
    }

    public Pointer getShaderModule(){
        return shaderModule;
    }

    public String getName() {
        return name;
    }

    public String getShaderSource() {
        return shaderSource;
    }

    public void dispose(){
        LibGPU.wgpu.ShaderModuleRelease(shaderModule);
        shaderModule = null;
    }

}
