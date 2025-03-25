package com.monstrous.graphics.g3d;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.VertexAttribute;
import com.monstrous.graphics.VertexAttributes;
import com.monstrous.math.Vector2;
import com.monstrous.math.Vector3;
import com.monstrous.webgpu.WGPUPrimitiveTopology;

import static com.monstrous.graphics.VertexAttribute.Usage.*;

/** Support class to programmatically build a mesh
 *
 */
public class MeshBuilder {

    public static class VertexInfo{
        Vector3 position;
        Vector3 normal;
        Vector2 uv;
        Color color;
    }

    private VertexAttributes vertexAttributes;
    private short[] indices;
    private int numIndices;
    private VertexInfo[] vertices;
    private int numVertices;
    private int stride;     // number of floats per vertex
    private Color color;
    private Vector3 normal;
    private Vector2 textureCoord;
    private Mesh mesh;
    private MeshPart part;
    private WGPUPrimitiveTopology topology;

    public void begin(VertexAttributes vertexAttributes ){
        begin(vertexAttributes, 32000, 32000);
    }

    public void begin(VertexAttributes vertexAttributes, int maxVertices, int maxIndices){
        begin(vertexAttributes, maxVertices, maxIndices, WGPUPrimitiveTopology.Undefined);
    }

    public void begin(VertexAttributes vertexAttributes, int maxVertices, int maxIndices, WGPUPrimitiveTopology topology){
        if(mesh != null) throw new IllegalStateException("MeshBuilder: cannot nest begin(); call end() before another begin()");
        this.vertexAttributes = vertexAttributes;
        this.topology = topology;
        indices = new short[maxIndices];
        numIndices = 0;
        stride = vertexAttributes.getVertexSizeInBytes()/Float.BYTES;
        vertices = new VertexInfo[maxVertices];
        numVertices = 0;
        color = new Color(Color.WHITE);
        normal = new Vector3(0,1,0);
        textureCoord = new Vector2(0,0);
        mesh = new Mesh();
    }

    public Mesh end(){
        if(mesh == null) throw new IllegalStateException("MeshBuilder: must call begin() before end().");
        mesh.setVertexAttributes(vertexAttributes);
        //mesh.setTopology(topology);
        float[] vertexData = new float[stride*numVertices];
        int vindex = 0;
        for(int i = 0; i < numVertices; i++){
            VertexInfo vi = vertices[i];
            int start = vindex;
            for(VertexAttribute attrib : vertexAttributes.attributes){
                switch((int) attrib.usage) {
                    case POSITION:
                        vertexData[vindex++] = vi.position.x;
                        vertexData[vindex++] = vi.position.y;
                        vertexData[vindex++] = vi.position.z;
                        vertexData[vindex++] = 0;   // because it is defined as Float32x4
                        break;
                    case NORMAL:
                        vertexData[vindex++] = vi.normal.x;
                        vertexData[vindex++] = vi.normal.y;
                        vertexData[vindex++] = vi.normal.z;
                        break;
                    case COLOR:
                        vertexData[vindex++] = vi.color.r;
                        vertexData[vindex++] = vi.color.g;
                        vertexData[vindex++] = vi.color.b;
                        vertexData[vindex++] = vi.color.a;
                        break;
                    case TEXTURE_COORDINATE:
                        vertexData[vindex++] = vi.uv.x;
                        vertexData[vindex++] = vi.uv.y;
                        break;
                }
            }
            if(vindex - start != stride)
                throw new RuntimeException("MeshBuilder: missing attributes");
        }
        endPart();
        mesh.setVertices(vertexData);
        mesh.setIndices(indices, numIndices);

        Mesh m = mesh;
        mesh = null;
        return m;
    }

    public MeshPart part(String name, WGPUPrimitiveTopology topology){
        endPart();
        this.topology = topology;
        part = new MeshPart(mesh, name, topology);
        part.setOffset(numIndices);     // support for Mesh without indices?
        System.out.println("Offset for "+name+" : "+numIndices);
        return part;
    }

    // implied by end()
    public void endPart(){
        if(part != null){
            part.setSize(numIndices - part.getOffset());
            part = null;
        }
    }

    public int getVertexCount(){
        return numVertices;
    }

    /** note: color per vertex is not supported in standard shader */
    public void setColor(Color color){
        this.color.set(color);
    }

    public void setNormal(Vector3 N){
        this.normal.set(N);
    }

    public void setNormal(float x, float y, float z) {
        this.normal.set(x, y, z);
    }

    // tricky in combination with addTriangle or addRect
    public void setTextureCoordinate(float u, float v) {
        this.textureCoord.set(u, v);
    }

    public void setTextureCoordinate(Vector2 uv){
        setTextureCoordinate(uv.x, uv.y);
    }

    public int addVertex(Vector3 position){
        return addVertex(position.x, position.y, position.z);
    }

    public int addVertex(float x, float y, float z){
       if(numVertices > vertices.length)
           throw new RuntimeException("MeshBuilder: too many vertices.");
        VertexInfo vert = new VertexInfo();
        vert.position = new Vector3(x,y,z);
        vert.normal = new Vector3(normal);
        vert.uv = new Vector2(textureCoord);
        vert.color = new Color(color);
        vertices[numVertices++] = vert;
        return numVertices - 1;
    }

    public void addIndex(short index){
        if(numIndices >= indices.length)
            throw new RuntimeException("MeshBuilder: too many indices.");
        indices[numIndices++] = index;
    }

    public void addLine(Vector3 c0, Vector3 c1){
        int i0 = addVertex(c0);
        int i1 = addVertex(c1);
        addIndex((short)i0);
        addIndex((short)i1);
    }

    public void addTriangle(Vector3 c0, Vector3 c1, Vector3 c2){
        int i0 = addVertex(c0);
        int i1 = addVertex(c1);
        int i2 = addVertex(c2);
        if(topology == WGPUPrimitiveTopology.TriangleList) {
            addIndex((short) i0);
            addIndex((short) i1);
            addIndex((short) i2);
        } else if(topology == WGPUPrimitiveTopology.LineList) {
            addIndex((short) i0);
            addIndex((short) i1);
            addIndex((short) i1);
            addIndex((short) i2);
            addIndex((short) i2);
            addIndex((short) i0);
        }
    }

    /**
     * Add rectangle: provide corner positions in counter-clockwise order as seen from the front.
     */
    // c00  c10
    // c01  c11
    //
    // triangles (counter clockwise):
    //   c00 c01 c10
    //   c01 c11 c10
    public void addRect(Vector3 c00, Vector3 c10, Vector3 c11, Vector3 c01){
        Vector2 uv = new Vector2(0.5f, 0.5f);
        addRect(c00, c10, c11, c01, uv, uv, uv, uv);
    }

    /**
     * Add rectangle: provide corner positions in counter-clockwise order as seen from the front and texture coordinates in the same order.
     */
    public void addRect(Vector3 c00, Vector3 c10, Vector3 c11, Vector3 c01, Vector2 uv00, Vector2 uv10, Vector2 uv11, Vector2 uv01){
        setTextureCoordinate(uv00);
        int i0 = addVertex(c00);
        setTextureCoordinate(uv10);
        int i1 = addVertex(c10);
        setTextureCoordinate(uv11);
        int i2 = addVertex(c11);
        setTextureCoordinate(uv01);
        int i3 = addVertex(c01);
        if(topology == WGPUPrimitiveTopology.TriangleList) {
            addIndex((short) i0);
            addIndex((short) i3);
            addIndex((short) i1);

            addIndex((short) i3);
            addIndex((short) i2);
            addIndex((short) i1);
        } else if(topology == WGPUPrimitiveTopology.LineList) {
            addIndex((short) i0);            addIndex((short) i1);
            addIndex((short) i1);            addIndex((short) i2);
            addIndex((short) i2);            addIndex((short) i3);
            addIndex((short) i3);            addIndex((short) i0);
        }
    }
}
