## To Do list


- generate mipmaps in a compute shader
- generate IBL textures (env, irradiance, radians) from a single file
- replace constant in shader for radiance LOD count by a uniform

- support color per vertex as option in the standard shader
- DONE sphere builder
- DONE frustum builder
- other shapes
- DONE better support for texture coordinates in mesh builder
- DONE topology per mesh
- DONE packed color (one float) for sprite batch etc.


- when textures, materials, meshes, etc. are shared, we need to track ownership to allow correct disposal (i.e. dispose exactly once)



## Bugs to fix
- post shader demo is broken
- - frustum issues in shadow demo