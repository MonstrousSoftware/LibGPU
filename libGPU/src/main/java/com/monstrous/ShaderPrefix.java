package com.monstrous;

import com.monstrous.graphics.VertexAttribute;
import com.monstrous.graphics.VertexAttributes;
import com.monstrous.graphics.lights.Environment;

public class ShaderPrefix {
    private static StringBuffer sb = new StringBuffer();

    public static String buildPrefix(VertexAttributes vertexAttributes, Environment environment ){
        sb.setLength(0);
        if(vertexAttributes != null) {
            if (vertexAttributes.hasUsage(VertexAttribute.Usage.TEXTURE_COORDINATE)) {
                sb.append("#define TEXTURE_COORDINATE\n");
            }
            if (vertexAttributes.hasUsage(VertexAttribute.Usage.COLOR)) {
                sb.append("#define COLOR\n");
            }
            if (vertexAttributes.hasUsage(VertexAttribute.Usage.NORMAL)) {
                sb.append("#define NORMAL\n");
            }
            if (vertexAttributes.hasUsage(VertexAttribute.Usage.TANGENT)) {   // this is taken as indication that a normal map is used
                sb.append("#define NORMAL_MAP\n");
            }
        }
        if (environment != null && !environment.depthPass && environment.renderShadows) {
            sb.append("#define SHADOWS\n");
        }
        if (environment != null && environment.cubeMap != null) {
            sb.append("#define CUBEMAP\n");
        }
        return sb.toString();
    }
}
