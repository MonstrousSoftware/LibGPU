## Performance tuning

Reference scene is the GLTF Sponza model viewed with the following config settings:

        config.setSize(1200, 800);
        config.vsyncEnabled = false;
        config.backend = WGPUBackendType.Vulkan; 
        config.numSamples = 4; 

the following camera settings:

        camera = new PerspectiveCamera(120, LibGPU.graphics.getWidth(), LibGPU.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 100f;
        camera.position.set(6.7f, 5, -1.6f);
        camera.direction.set(-0.99f,0f, 0.1f);

and with a single shadow pass.

The model has one mesh and 103 primitives (i.e. mesh parts).

The original frame rate is 595 fps with 40 microseconds on the GPU (for the color pass).
Without shadows, it runs at 1150 fps.

There are 2 pipelines, and 3 pipeline switches. There are 103 draw calls.

### Step 1
An optimisation was made to perform frustum culling on the renderables in ModelBatch#flush().
The frame rate increases to 1200 (no shadows) or 591 fps (with shadow) and draw calls drop to 97.

Not many renderables are culled and not much gain in frame rate.  The culling seems correct though, 
the model just has large overlapping submeshes (primitives) and the reference camera view shows almost all of them. 

### Step 3
Can we speed up by doing the shadow pass only with a depth buffer? (currently we also have a color buffer to help in debugging).

Shadow depth pass is now done without color buffer. Frame rate is now 720 fps with shadow.
This caused some artifacts on the alpha masked vegetation textures. This was solved by defaulting pipeline spec to not-blending.

### Step 4
The ModelBatch sorts renderables on meshPart.  This is supposed to encourage instancing.  However, at least in the Sponza test case, there
is no instancing of mesh parts due to the model design. Meanwhile, there are 90 material switches, which require a bind group change.
By changing the renderable sorting to sort by material, there are only 25 material switches.  Frame rate increases to 830 fps with shadows,
1280 fps without shadows.

### Step 5
Using a Z pre-pass, also known as depth pre-pass. This does a pass from the camera point of view just filling the depth buffer and the color pass
then uses this to not call the fragment shader on occluded fragments.

For the scene in question, it turns out to be a net negative.  The extra pass is more costly than the savings in the fragment shader.  This may be different in
a scene where there is more overdraw or where the fragment shader is more expensive.

No shadows without Z pre-pass: 1280 fps, with Z pre-pass: 850 fps.
With shadows without Z pre-pass: 820 fps, with Z pre-pass: 620 fps.

### Step 6
From looking at the code it seems ModelBatch#render() is spending a lot of time to generate a list of renderables which are all thrown away at ModelBatch#end().
The render method descends into the Node hierarchy to generate the renderables. With multiple passes per frame, it may seem valuable to save the renderables list between
passes. In case of a static scene, the renderables list could even be saved between frames.

Having experimented with that, it appears the savings are almost negligible.  A profile using visualvm shows that the time spent in render() is only 100 ms compared to 11000 ms spent in 
emitting the renderables.  The downside is that it makes the API more complex, e.g. instead of ModelBatch.end() you would have different calls to finalize the list of renderables (sorting and culling),
emit the renderables (draw calls) and clean up (release resources).
So this idea is not pursued.



