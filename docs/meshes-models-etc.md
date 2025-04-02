# Models, Meshes, MeshParts


## Model
A Model is a three-dimensional graphical object.  It consists of a mesh (a polygonal shape) and a material (such as a color or a texture).  
It is used as a template for a ModelInstance which puts a Model in a particular location in the game world. For example, you may have one model of a palm tree,
and 50 model instances of palm trees in your game. They are all the same model, but appear in different locations and are scaled and rotated differently.

You can create a Model in code by constructing it from a mesh and a material.

```java
	Mesh = // see below					// build a mesh
	Material material = new Material( Color.GREEN );	// make a material
	Model model = new Model( mesh, material );		// create a model
```

It is also possible to build a model from a mesh part instead of a mesh. A mesh part is, as the name suggests, a subset of a mesh.
If you imagine a mesh consists of 1000 vertices, perhaps one mesh part refers to the first 200 vertices and another mesh part refers to the 
remaining 800 vertices.  A mesh part refers to a contiguous segment of the mesh.  If the mesh is indexed, it refers to a segment of the index list. 
If the mesh is not indexed, it refers to a segment of the vertex list. 
Different mesh parts can use different topology even though they share the same mesh.  One mesh part may use a triangle list (the most common topology), another mesh part
may use a line list or a triangle strip.  The topology is defined by the mesh part, not by the mesh.  The mesh is just a list of vertices.
Using mesh parts in models, allows different models to share the same mesh (just different parts of it).  This means the GPU can render the 
different models without switching vertex buffer. 

If you build a model from a mesh it will automatically create a mesh part corresponding to the entire mesh. 
It will assume a topology of triangle list or you can use the more extended constructor to define the topology:
```
	public Model(Mesh mesh, WGPUPrimitiveTopology topology, Material material);
```


### Load from file

Instead of building a model in code, the more usual approach is to load it from a file.  The following file formats are supported:
- GLTF  Graphics Library Transmission Format 2.0 by the Khronos group
- GLB (binary version of GLTF)
- OBJ	Wavefront OBJ format

(G3DB and G3DJ formats are not supported at this moment).
(GLTF support does not include animations, morphing, extensions).

Loading a model from a file, which has to be located in the assets folder, is as simple as the following example:

```java
	Model bunnyModel = new Model("bunny.gltf");
```

### Model with multiple meshes, materials
A model can have multiple meshes and multiple materials. They can be added as follows:

```java
	model.addMesh( mesh );
	model.addMaterial( material );
```

(Note that using `addMesh()` will *not* create a corresponding mesh part, this only happens when the mesh is provided to the model constructor).

A model's meshes and materials are only useful when they are used by a mesh part.  Adding a mesh part to a model, is done by adding via a node and a node part:

```java
	NodePart nodePart = new NodePart( meshPart, material);
	Node node = new Node( nodePart );
	model.addRootNode( node );
```

Nodes are used in models to define a hierarchy of transforms as a tree structure with mesh parts located at the leaf nodes (NodePart). 
Imagine you have a car model with four wheels.  The wheels could be the four times the same mesh part with different transformations from the car's origin. 

### Disposal

Note that models needs to be disposed when they are no longer needed. For example, a mesh will have a vertex buffer and possibly an index buffer allocated on the GPU which are released
when the model and its meshes are disposed.

```java
		model.dispose();
```
 
A model should not be disposed as long as a model instance that was derived from it is still in use.  The model needs to outlive its model instances.



## Mesh
A mesh doesn't know about topology but it does have to know about which data to store per vertex: the vertex attributes. One vertex attribute is mandatory: the position.
Other potential vertex attributes are:
- color
- normal vector
- texture coordinate 

### VertexAttributes
Which attributes you want in a mesh depends on the application.  Since there are usually many vertices, it makes sense to store only the vertex attributes you need.

It is possible to have a index list as well as a vertex list in a mesh.  In this case, the index list gives the order of vertices to use when rendering the mesh.
The advantage is that it is more compact when the same vertex is used multiple times; you don't have to repeat the vertex information, just provide the same index. 

An index can be 16 bit (short) or 32 bit (int).  A 16 bit index is more compact (less memory, faster to send to the GPU), but 32 bit allows for more complex meshes.


Vertex attributes are defined using the VertexAttributes class. [subject to change].  Different attributes are defined using the `add()` method.
Once all attributes have been defined use the `end()` method to finalize the definition.

For each attribute you can define:
- the usage, which is defined using one of the values from VertexAttribute.Usage (see below)
- a label, which is a free format string for debugging convenience
- a data format using the enum WGPUVertexFormat
- the bind location which must correspond to the bind location in the shader.

```
VertexAttribute.Usage:
        static public final int POSITION = 1;
        static public final int COLOR = 2;
        static public final int COLOR_PACKED = 4;
        static public final int TEXTURE_COORDINATE = 8;
        static public final int NORMAL= 16;
        static public final int TANGENT = 32;
        static public final int BITANGENT = 64;
```



```java
        VertexAttributes vertexAttributes = new VertexAttributes();
	vertexAttributes.add(VertexAttribute.Usage.POSITION, "position", WGPUVertexFormat.Float32x4, 0);
	vertexAttributes.add(VertexAttribute.Usage.TEXTURE_COORDINATE,"uv", WGPUVertexFormat.Float32x2, 1);
        vertexAttributes.end();
```

### Mesh construction

A mesh can be constructed using from a float array. For example to define three vertices (and no index list):

```java
	float[]  vertexData = {
           // float4 position, float2 uv,
            1, -1, 1, 1,	 0, 1,
           -1, -1, 1, 1,	 1, 1,
           -1, -1, -1, 1, 	 1, 0,
	};
        Mesh mesh = new Mesh();
        mesh.setVertexAttributes(vertexAttributes);	// see above
        mesh.setVertices(vertexData);
```

A mesh can also be constructed using the utility class MeshBuilder.  For example, to build a mesh containing one triangle.

```java
        VertexAttributes vertexAttributes = new VertexAttributes();
	vertexAttributes.add(VertexAttribute.Usage.POSITION, "position", WGPUVertexFormat.Float32x4, 0); [or Float32x3?]
        vertexAttributes.end();

        MeshBuilder mb = new MeshBuilder();
        mb.begin(vertexAttributes, 3, 3);		// 3 vertices, 3 indices
	mb.addVertex( 1, 0 , 0 );  
	mb.addVertex( -1, 0 , 0 );  
	mb.addVertex( 0, 0 , 1 );  
        Mesh mesh = mb.end();
```

The `begin()` method takes a VertexAttributes object, an upper limit for the number of vertices and a maximum number of indices. (needed?)
The upper limit values are only used during construction, the resulting mesh will contain only the number of vertices that were defined.

## MeshPart

To define mesh parts, we can use the `part()` method of MeshBuilder.  It means all the following vertices will be used in the mesh part until 
the end of the mesh or the next mesh part.  The `part()` method allows to define a label and the topology (recall that the mesh itself does not have a topology).

Also note that there are convenience methods for defining lines, triangles or rectangles.

```java
	Vector3[] corners = { .. };
        
	VertexAttributes vertexAttributes = new VertexAttributes();
	vertexAttributes.add(VertexAttribute.Usage.POSITION, "position", WGPUVertexFormat.Float32x4, 0);
	vertexAttributes.add(VertexAttribute.Usage.NORMAL, "normal", WGPUVertexFormat.Float32x3, 2);
	vertexAttributes.add(VertexAttribute.Usage.COLOR_PACKED,"color", WGPUVertexFormat.Unorm8x4, 5);
        vertexAttributes.end();
		
        MeshBuilder mb = new MeshBuilder();
        mb.begin(vertexAttributes, 18, 18);
        MeshPart part = mb.part("pyramid", WGPUPrimitiveTopology.TriangleList);

        mb.setNormal(0,-1,0);     mb.setColor(Color.BLUE); mb.addRect(corners[0], corners[1], corners[2], corners[3]); 

        // sides
        mb.setColor(Color.RED);    mb.setNormal(0,1,1);  mb.addTriangle(corners[0], corners[1], corners[4]);
        mb.setColor(Color.ORANGE); mb.setNormal(1,1,0);  mb.addTriangle(corners[1], corners[2], corners[4]);
        mb.setColor(Color.YELLOW); mb.setNormal(0,1,-1); mb.addTriangle(corners[2], corners[3], corners[4]);
        mb.setColor(Color.WHITE);  mb.setNormal(-1,1,0); mb.addTriangle(corners[3], corners[0], corners[4]);
        mb.end();
		
	Material material = new Material( Color.WHITE );
        Model pyramidModel = new Model(part, material);
```

For common shapes there are a few shape builder classes to define a mesh. For example, to construct a box of 2 by 2 by 2 world units:

```java
	VertexAttributes vertexAttributes = new VertexAttributes();
	vertexAttributes.add(VertexAttribute.Usage.POSITION, "position", WGPUVertexFormat.Float32x4, 0);
	vertexAttributes.add(VertexAttribute.Usage.NORMAL, "normal", WGPUVertexFormat.Float32x3, 2);
        vertexAttributes.end();
		
        MeshBuilder mb = new MeshBuilder();
        mb.begin(vertexAttributes, 100, 100);
		
        MeshPart meshPart = BoxShapeBuilder.build(mb, 2, 2, 2,  WGPUPrimitiveTopology.TriangleList);
        Material material = new Material( texture );

        Model boxModel = new Model(meshPart, material);
```




## Materials

A simple material can be created from a color. This sets the material's base color:

```java
	Material material = new Material( Color.GREEN );
```

It is also possible to create a material from a texture, this sets the material's diffuse texture:	

```java
	Texture texture = new Texture( "image.png" );
	Material material = new Material( texture );
```

It is also possible to construct a material using the MaterialData class for fine control.
Materials can support:
- base color
- diffuse texture
- normal texture			a normal map
- emissive texture				
- metallicFactor			between 0 and 1
- roughnessFactor			between 0 and 1
- metalicRoughness texture	the blue channel is used for metalness and the green channel for roughness

Values that are not defined are set to a default value. E.g. the base color will default to white and the 
diffuse texture will default to a texture of a single white pixel.


## ModelInstance

A ModelInstance is when a Model is placed in the game world. It requires at the very least a model and a position:

For example:
```java
	ModelInstance boxInstance = new ModelInstance( boxModel, 0, 2, 0 );	// place box at position (0,2,0)
```	
	
More generally, instead of a position we can use a transform matrix because the model can also be rotated and scaled.

```java
	Matrix4 transform = new Matrix4();
	transform
		.scl(0.5f)				// scale
		.trn(0, -1, 0);			// translate

	ModelInstance boxInstance = new ModelInstance( boxModel, transform );
```
	


## ModelBatch

To render a 3d scene using a given camera, we use ModelBatch.
For example:

```java
	ModelBatch modelBatch = new ModelBatch();
	modelBatch.begin(camera);
	modelBatch.draw(modelInstance);
	modelBatch.end();
```

The `begin()` method has additional variants:

- begin(Camera camera, Environment environment);
- begin(Camera camera, Environment environment, Color clearColor);
- begin(Camera camera, Environment environment, Color clearColor, Texture outputTexture, Texture depthTexture)
- begin(Camera camera, Environment environment, Color clearColor, Texture outputTexture, Texture depthTexture, RenderPassType passType)

The `environment` is used to define lighting.

The `clearColor` will set the background color if it is defined (a null value means the screen is not cleared).  This is an alternative to using `ScreenUtils.clear()` which avoids an extra render pass.

By provide output textures (color and depth), the scene can be renderered to a texture instead of to the screen.
This technique can be used instead of Frame Buffer Objects for example if you want to perform some screen effects. 

Note: such textures must be defined as render attachment, e.g. by setting renderAttachment to true in the texture constructor:
```java
    public Texture(int width, int height, boolean mipMapping, boolean renderAttachment, WGPUTextureFormat format, int numSamples )
```
The `passType` parameter can be used to defined a specific render pass type, e.g. a shadow pass. (subject to change)
