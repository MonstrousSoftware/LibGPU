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

- 
Model loader:

- Support for animations
- Support for groups/objects on OBJ files, currently it always produces one meshPart
- Support for G3DB format


## Bugs to fix
- in Sponza the corners of pillars are too straight, issue with normal map?
