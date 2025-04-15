## To Do list


- generate mipmaps in a compute shader
- DONE generate IBL textures (env, irradiance, radians) from a single file (but not HDR)
- support HDR textures (32bit float format is tricky in WebGPU)
- DONE replace constant in shader for radiance LOD count by a uniform

- DONE support (packed) color per vertex as option in the standard model batch shader
- DONE sphere builder
- DONE frustum builder
- other shapes
- DONE better support for texture coordinates in mesh builder
- DONE topology per mesh
- DONE packed color (one float) for sprite batch etc.
- remove the need to provide #vertices/#indices to mesh builder

- when textures, materials, meshes, etc. are shared, we need to track ownership to allow correct disposal (i.e. dispose exactly once)
- support full screen mode
- GLTF: support base64 encoded buffers

- 
Model loader:

- DONE Support for animations
- Support for groups/objects on OBJ files, currently it always produces one meshPart
- Support for G3DB format
- DONE Support for skeletal animations
- skeletal animation: fix for other models that don't work
- skeletal animation: fix memory leaks, support multiple animated instances 
- skeletal animation: compress vertex attribute joints (could be 4 bytes instead of 4 floats)



## Bugs to fix
- in Sponza the corners of pillars are too straight, issue with normal map?


## Notes
- animation: check for time before first key frame or after last
- skinning : where to store inv bind mtrices, per mesh? per meshPart? nodePart?  Stored in Model for now and each Renderable has now a ref to its Model.
- 
  https://lisyarus.github.io/blog/posts/gltf-animation.html
- 
