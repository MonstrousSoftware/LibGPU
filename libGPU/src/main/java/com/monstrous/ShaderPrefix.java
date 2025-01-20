package com.monstrous;

import com.monstrous.graphics.VertexAttribute;
import com.monstrous.graphics.VertexAttributes;

public class ShaderPrefix {
    private static StringBuffer sb = new StringBuffer();

    public static String buildPrefix(VertexAttributes vertexAttributes ){
        sb.setLength(0);
        if(vertexAttributes.hasUsage(VertexAttribute.Usage.TEXTURE_COORDINATE)){
            sb.append("#define TEXTURE_COORDINATE\n");
        }
        if(vertexAttributes.hasUsage(VertexAttribute.Usage.COLOR)){
            sb.append("#define COLOR\n");
        }
        if(vertexAttributes.hasUsage(VertexAttribute.Usage.NORMAL)){
            sb.append("#define NORMAL\n");
        }
        return sb.toString();
    }
}
