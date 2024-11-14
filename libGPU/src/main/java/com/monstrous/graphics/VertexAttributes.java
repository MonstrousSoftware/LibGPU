package com.monstrous.graphics;

import com.monstrous.utils.Disposable;
import com.monstrous.wgpu.WGPUVertexAttribute;
import com.monstrous.wgpu.WGPUVertexBufferLayout;
import com.monstrous.wgpu.WGPUVertexFormat;
import com.monstrous.wgpu.WGPUVertexStepMode;

import java.util.ArrayList;

public class VertexAttributes implements Disposable {

    public ArrayList<VertexAttribute> attributes;
    private WGPUVertexBufferLayout vertexBufferLayout;

    public VertexAttributes() {
        attributes = new ArrayList<>();
    }

    public void add(String name, WGPUVertexFormat format, int shaderLocation){
        VertexAttribute va = new VertexAttribute(name, format, shaderLocation);
        attributes.add(va);
    }

    public void end(){
        WGPUVertexAttribute[] attribs = new WGPUVertexAttribute[attributes.size()];

        int offset = 0;
        int index = 0;
        for(VertexAttribute va : attributes) {

            attribs[index] =  WGPUVertexAttribute.createDirect();
            attribs[index].setFormat(va.format);
            attribs[index].setOffset(offset);
            attribs[index].setShaderLocation(va.shaderLocation);

            offset += va.getSize();
            index++;
        }

        vertexBufferLayout = WGPUVertexBufferLayout.createDirect();
        vertexBufferLayout.setAttributeCount(attributes.size());
        vertexBufferLayout.setAttributes(attribs);
        vertexBufferLayout.setArrayStride(offset);
        vertexBufferLayout.setStepMode(WGPUVertexStepMode.Vertex);
    }

    public WGPUVertexBufferLayout getVertexBufferLayout(){
        if(vertexBufferLayout == null)
            end();
        return vertexBufferLayout;
    }


    @Override
    public void dispose() {
        // todo free layout?
    }
}
