package com.monstrous.graphics;

import com.monstrous.FileHandle;
import com.monstrous.LibGPU;
import com.monstrous.webgpu.WGPUSType;
import com.monstrous.webgpu.WGPUShaderModuleDescriptor;
import com.monstrous.webgpu.WGPUShaderModuleWGSLDescriptor;
import jnr.ffi.Pointer;

public class ShaderProgram {


    private String name;
    private String shaderSource;
    private String processed;
    private Pointer shaderModule;
    private static Preprocessor preprocessor = new Preprocessor();

//    public ShaderProgram(String filePath) {
//        String source = null;
//        try {
//            source = Files.readString(Paths.get(filePath));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        compile(filePath, source);
//    }

    public ShaderProgram(FileHandle fileHandle) {
        this(fileHandle, "");
    }

    public ShaderProgram(FileHandle fileHandle, String prefix) {
        String source = null;
        source = fileHandle.readString();
        compile(fileHandle.file.getName(), prefix+source);
    }

    public ShaderProgram(String name, String shaderSource, String prefix){

        compile(name, prefix+shaderSource);
    }

    private void compile(String name, String shaderSource){
        this.name = name;
        this.shaderSource = shaderSource;

        //Preprocessor preprocessor = new Preprocessor();
        processed = preprocessor.process(shaderSource);

        // Create Shader Module
        WGPUShaderModuleDescriptor shaderDesc = WGPUShaderModuleDescriptor.createDirect();
            shaderDesc.setLabel(name);

        WGPUShaderModuleWGSLDescriptor shaderCodeDesc = WGPUShaderModuleWGSLDescriptor.createDirect();
            shaderCodeDesc.getChain().setNext();
            shaderCodeDesc.getChain().setSType(WGPUSType.ShaderModuleWGSLDescriptor);
            shaderCodeDesc.setCode(processed);

            shaderDesc.getNextInChain().set(shaderCodeDesc.getPointerTo());

        shaderModule = LibGPU.webGPU.wgpuDeviceCreateShaderModule(LibGPU.device, shaderDesc);
        if(shaderModule == null)
            throw new RuntimeException("ShaderModule: compile failed "+name);

        //System.out.println(name+": "+processed);
    }

    public Pointer getShaderModule(){
        return shaderModule;
    }

    public String getName() {
        return name;
    }

//    //public String getShaderSource() {
//        return shaderSource;
//    }

    public void dispose(){
        LibGPU.webGPU.wgpuShaderModuleRelease(shaderModule);
        shaderModule = null;
    }

}
