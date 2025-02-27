package com.monstrous.graphics;

import com.monstrous.utils.Disposable;
import com.monstrous.webgpu.WGPUVertexAttribute;
import com.monstrous.webgpu.WGPUVertexBufferLayout;
import com.monstrous.webgpu.WGPUVertexFormat;
import com.monstrous.webgpu.WGPUVertexStepMode;

import java.util.ArrayList;

public class VertexAttributes implements Disposable {

    public ArrayList<VertexAttribute> attributes;
    private WGPUVertexBufferLayout vertexBufferLayout;
    private int vertexSize; // in floats
    private long usageFlags;        // bit mask of Usage values

    public VertexAttributes() {
        attributes = new ArrayList<>();
        vertexBufferLayout = null;
        vertexSize = -1;
        usageFlags = 0L;
    }

    public void add(long usage, String label, WGPUVertexFormat format, int shaderLocation){
        VertexAttribute va = new VertexAttribute(usage, label, format, shaderLocation);
        usageFlags |= usage;
        attributes.add(va);
    }

    public void end(){
        vertexSize = 0;
        for(VertexAttribute va : attributes) {
            vertexSize += va.getSize();
        }
    }

    public boolean hasUsage(long usage){
        return (usageFlags & usage) == usage;
    }

    public long getUsageFlags(){
        return usageFlags;
    }


    public int getVertexSizeInBytes() {
        if(vertexSize < 0)
            throw new RuntimeException("getVertexSize: call VertexAttributes.end() first");
        return vertexSize;
    }

    public WGPUVertexBufferLayout getVertexBufferLayout(){
        if(vertexBufferLayout != null)
            return vertexBufferLayout;

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
        return vertexBufferLayout;
    }


    @Override
    public void dispose() {
        // todo free layout?
    }
}
