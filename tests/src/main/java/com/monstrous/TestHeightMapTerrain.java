package com.monstrous;

import com.monstrous.graphics.*;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.graphics.g3d.*;
import com.monstrous.graphics.lights.DirectionalLight;
import com.monstrous.graphics.lights.Environment;
import com.monstrous.math.Vector2;
import com.monstrous.math.Vector3;
import com.monstrous.utils.ScreenUtils;
import com.monstrous.webgpu.WGPUPrimitiveTopology;

import java.util.ArrayList;

/** Test building a terrain mesh from a Perlin noise height map.
 *
 */

public class TestHeightMapTerrain extends ApplicationAdapter {


    private ModelBatch modelBatch;
    private Camera camera;
    private Texture texture;
    private Model modelTerrain;
    private Model modelTerrainDebug;
    private ArrayList<ModelInstance> modelInstances;
    private Environment environment;
    private CameraController camController;
    private SpriteBatch batch;
    private BitmapFont font;
    private final float cellSize = 4f;    // world units per cell;

    public void create() {
        modelInstances = new ArrayList<>();

        texture = new Texture("textures/rocks-diffuse.jpg");

        Material terrainMaterial = new Material(texture);
        terrainMaterial.metallicFactor = 0f;
        terrainMaterial.roughnessFactor = 0.5f;
        modelTerrain = buildTerrainModel(false, terrainMaterial);
        modelTerrainDebug = buildTerrainModel(true, new Material(Color.BLACK));
        modelInstances.add(new ModelInstance(modelTerrain));
        //modelInstances.add(new ModelInstance(modelTerrainDebug, 0, 0.2f, 0));


        camera = new PerspectiveCamera(70, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.position.set(6, 4, -6);
        camera.direction.set(camera.position).scl(-1).nor();
        camera.far = 1500f;
        camera.update();

        camController = new CameraController(camera);
        LibGPU.input.setInputProcessor(camController);

        environment = new Environment();
        DirectionalLight sun = new DirectionalLight( Color.WHITE, new Vector3(.6f,-1,.3f).nor());
        sun.setIntensity(3f);
        environment.add( sun );
        environment.ambientLightLevel = 0.2f;

        camController = new CameraController(camera);
        LibGPU.input.setInputProcessor( camController );

        modelBatch = new ModelBatch();

        batch = new SpriteBatch();
        font = new BitmapFont();
    }



    /** demonstration of building a mesh without shape builder */
    private Model buildTerrainModel(boolean debug, Material material){

        int xoffset = 0;
        int yoffset = 0;

        // dimension are in terms of grid cells, vertex counts are one higher.
        int width = 128;
        int depth = 128;
        float uvScale = 20f/width;



        Noise noise = new Noise();
        // get an extra row and column for the heightmap so we can calculate the normals on the far edge
        float[][] heightMap = noise.generatePerlinMap(xoffset, yoffset, width+1, depth+1,  7, 30);

        Vector3[][] positions = new Vector3[width+1][depth+1];
        Vector3[][] normals = new Vector3[width+1][depth+1];

        for(int z = 0; z <= depth; z++) {
            for (int x = 0; x <= width; x++) {
                positions[z][x] = new Vector3(x * cellSize, heightMap[z][x], z * cellSize);
                normals[z][x] = new Vector3(0, 0, 0);   // create normal vector and initialize to zero
            }
        }

        calculateNormals(width, depth, positions, normals);




        MeshBuilder mb = new MeshBuilder();
        // vertex attributes are fixed per mesh
        VertexAttributes vertexAttributes = new VertexAttributes(VertexAttribute.Usage.POSITION|VertexAttribute.Usage.NORMAL|VertexAttribute.Usage.TEXTURE_COORDINATE);

        mb.begin(vertexAttributes, (width+1)*(depth+1)*2, (width+1)*(depth+1)*(debug?8:2));

        WGPUPrimitiveTopology topology = WGPUPrimitiveTopology.TriangleStrip;
        if(debug)
            topology = WGPUPrimitiveTopology.LineList;
        MeshPart meshPart = mb.part("terrain", topology);


        mb.setNormal(0,1,0);


        if(topology == WGPUPrimitiveTopology.LineList){ // for debug visualization
            int p0 = 0;
            int p1 = 0;
            for(int z = 0; z < depth; z++) {
                for (int x = 0; x <= width; x++) {
                    int i0 = mb.addVertex(positions[z][x]);
                    int i1 = mb.addVertex(positions[z+1][x]);
                    mb.addIndex((short) i0);
                    mb.addIndex((short) i1);
                    if(x > 0) {
                        mb.addIndex((short) i0);
                        mb.addIndex((short) p1);
                        mb.addIndex((short) i0);
                        mb.addIndex((short) p0);
                        mb.addIndex((short) i1);
                        mb.addIndex((short) p1);
                    }
                    p0 = i0;
                    p1 = i1;
                }
            }

        } else {  // TriangleStrip
            int ix = 0;
            for(int z = 0; z <= depth; z++) {
                for (int x = 0; x <= width; x++) {
                    mb.setNormal(normals[z][x].nor());
                    mb.setTextureCoordinate(z*uvScale, x*uvScale);
                    mb.addVertex(positions[z][x]);
                }
                if(z < depth) {
                    for (int x = 0; x <= width; x++) {
                        mb.addIndex(ix);
                        mb.addIndex(ix + width+1);
                        ix++;
                    }
                    // create a degenerate triangle for the transition to the next row
                    mb.addIndex( (ix+width));
                    mb.addIndex( (ix));
                }

            }
        }
        mb.end();

        return new Model(meshPart, material);
    }



    private final Vector3 u = new Vector3();
    private final Vector3 v = new Vector3();
    private final Vector3 n = new Vector3();

    /** calculate smooth normals for all vertices by taking average normal of all 6 triangles the vertex is part of */
    private void calculateNormals(int width, int depth, final Vector3[][]positions, Vector3[][]normals) {
        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                // three corners of the upper triangle of the grid square
                Vector3 p0 = positions[z][x];
                Vector3 px = positions[z][x+1];
                Vector3 pz = positions[z+1][x];
                // take cross product of the edge vectors
                u.set(px).sub(p0);
                v.set(pz).sub(p0);
                n.set(v).crs(u).nor();
                // accumulate normal vector of involved vertices
                normals[z][x].add(n).scl(2);    // double weight for a 90-degree angle
                normals[z][x+1].add(n);
                normals[z+1][x].add(n);

                // now do the lower triangle
                p0 = positions[z+1][x+1];
                pz = positions[z][x+1];
                px = positions[z+1][x];
                // take cross product of the edge vectors
                u.set(px).sub(p0);
                v.set(pz).sub(p0);
                n.set(v).crs(u).nor();
                // accumulate normal vector
                normals[z+1][x+1].add(n).scl(2);
                normals[z][x+1].add(n);
                normals[z+1][x].add(n);
            }
        }
    }



    public void render( ){
        if(LibGPU.input.isKeyPressed(Input.Keys.ESCAPE)){
            LibGPU.app.exit();
            return;
        }

        camController.update();

        ScreenUtils.clear(Color.TEAL);

        modelBatch.begin(camera, environment);
        modelBatch.render(modelInstances);
        modelBatch.end();

        batch.begin();
        font.draw(batch, "Demonstration of heightmap terrain.", 10, 50);
        font.draw(batch, "draw calls "+modelBatch.drawCalls+" emitted: "+modelBatch.numEmitted+" pipelines: "+modelBatch.numPipelines, 10, 20);
        batch.end();
    }

    public void dispose(){
        // cleanup
        modelBatch.dispose();
        modelTerrain.dispose();
        batch.dispose();
        font.dispose();
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
        batch.getProjectionMatrix().setToOrtho2D(0,0,width, height);
    }



    public static class Noise {

        Vector2 a = new Vector2();
        Vector2 d1 = new Vector2();

        /* Create pseudorandom direction vector
         */
        private void randomGradient(int ix, int iy, Vector2 gradient) {
            final float M = 2147483648f;
            final int shift = 16;

            int a = ix;
            int b = iy;
            a *= 348234342;
            b = b ^ ((a >> shift)|(a << shift));
            b *= 933742374;
            a = a^((b >> shift)|(b << shift));
            double rnd = ((float)a/M) * Math.PI;
            gradient.set((float)Math.sin(rnd), (float)Math.cos(rnd));
        }

        private float smoothstep(float a, float b, float w)
        {
            if(w < 0)
                w = 0;
            else if (w > 1.0f)
                w = 1.0f;
            float f = w*w*(3.0f-2.0f*w);
            return a + f*(b-a);
        }


        private float dotDistanceGradient(int ix, int iy, float x, float y){
            randomGradient(ix, iy, a);
            float dx = x - ix;	// distance to corner
            float dy = y - iy;
            d1.set(dx,dy);
            return a.dot(d1);
        }


        public float PerlinNoise(float x, float y) {
            int ix = (int)(x);
            int iy = (int)(y);


            float f1 = dotDistanceGradient(ix, iy, x, y);
            float f2 = dotDistanceGradient(ix+1, iy, x, y);
            float f3 = dotDistanceGradient(ix, iy+1, x, y);
            float f4 = dotDistanceGradient(ix+1, iy+1, x, y);

            float u1 = smoothstep(f1, f2, x-ix);	// interpolate between top corners
            float u2 = smoothstep(f3, f4, x-ix);	// between bottom corners
            float res = smoothstep(u1, u2, y-iy); // between previous two points
            return res;
        }




        public float[][] generatePerlinMap (int xoffset, int yoffset, int width, int height,  float gridscale, float amplitude) {
            float[][] noise = new float[height+1][width+1]; // add one extra to make seamless meshes

            for (int y = 0; y <= height; y++) {
                for (int x = 0; x <= width; x++) {

                    float xf = (xoffset+x)/gridscale;
                    float yf = (yoffset+y)/gridscale;
                    float value = PerlinNoise(xf, yf);
                    noise[y][x] = value * amplitude;
                }
            }
            return noise;
        }
    }


}

