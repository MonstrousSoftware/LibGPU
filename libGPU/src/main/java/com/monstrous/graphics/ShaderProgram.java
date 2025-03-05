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

package com.monstrous.graphics;

import com.monstrous.FileHandle;
import com.monstrous.LibGPU;
import com.monstrous.utils.Disposable;
import com.monstrous.webgpu.WGPUSType;
import com.monstrous.webgpu.WGPUShaderModuleDescriptor;
import com.monstrous.webgpu.WGPUShaderModuleWGSLDescriptor;
import jnr.ffi.Pointer;

public class ShaderProgram implements Disposable {

    private String name;
    private Pointer shaderModule;

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

        String processedSource = Preprocessor.process(shaderSource);

        // Create Shader Module
        WGPUShaderModuleDescriptor shaderDesc = WGPUShaderModuleDescriptor.createDirect();
            shaderDesc.setLabel(name);

        WGPUShaderModuleWGSLDescriptor shaderCodeDesc = WGPUShaderModuleWGSLDescriptor.createDirect();
            shaderCodeDesc.getChain().setNext();
            shaderCodeDesc.getChain().setSType(WGPUSType.ShaderModuleWGSLDescriptor);
            shaderCodeDesc.setCode(processedSource);

            shaderDesc.getNextInChain().set(shaderCodeDesc.getPointerTo());

        shaderModule = LibGPU.webGPU.wgpuDeviceCreateShaderModule(LibGPU.device, shaderDesc);
        if(shaderModule == null)
            throw new RuntimeException("ShaderModule: compile failed "+name);

        //System.out.println(name+": "+processedSource);
    }

    public Pointer getHandle(){
        return shaderModule;
    }

    public String getName() {
        return name;
    }

//    //public String getShaderSource() {
//        return shaderSource;
//    }

    @Override
    public void dispose(){
        LibGPU.webGPU.wgpuShaderModuleRelease(shaderModule);
        shaderModule = null;
    }

}
