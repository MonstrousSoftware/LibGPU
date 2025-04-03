

Adding shadows to the rendering means we now have to make multiple render passes on the same objects. Once to create a depth maps, and once to render the colours.
ModelBatch.begin() create a render pass and ModelBatch.end() ends it.  (In the future we may want to encapsulate this in something like SceneManager, where the multiple
passes are hidden in render()).

On the one hand we want to allow a ShadowProgram to be passed as input parameter to SpriteBatch.  On the other hand we want to adapt the standard shader
to the provided renderables. (The same applies to ModelBatch).

Let the Pipeline constructor compile the shader if it was not already provided in the pipeline spec.   The shader in this case will be owned by the pipeline spec.
    
    public Pipeline(Pointer pipelineLayout, PipelineSpecification spec) {
        this.specification = new PipelineSpecification(spec);

        this.pipelineLayout = pipelineLayout;

        // if the specification does not already have a shader, create one from the source file, customized to the vertex attributes.
        if(spec.shader == null){
            String prefix = ShaderPrefix.buildPrefix(spec.vertexAttributes);
            spec.shader = new ShaderProgram(spec.shaderFilePath, prefix);
        }


We also aim to adapt the shader to the actual vertex attributes and material attributes with an uber-shader approach.  The idea is to generate a shader prefix with constants
to direct the conditional compilation of the shader code.

SpriteBatch can perhaps serve as a simplified testing ground. Imagine some rectangles with position&color, some with position&color&texture and some with position&texture.
The shader and vertex attributes could be adapted to these cases.

SpriteBatch was updated to adapt the shader and the vertex buffer contents to the value of VertexAttributes (which is however still hardcoded in the constructor).
There is now a method setVertexAttributes() which allows to change the format on the fly (between begin and end).
Not a very useful or user-friendly method, but it demonstrates a proof of concept: changing pipelines and shaders during a batch.

Shader prefix is constructed from vertex attributes and environment.  Shader is compiled in the Pipeline constructor.  Because pipelines are cached in Pipelines this needs
only be performed once.



Clear Screen
------------
In OpenGL there is a specific call to clear the screen, in WebGPU this is part of the render pass construction.  If a frame is rendered in multiple passes, it is necessary to
indicate per pass if the screen needs to be cleared or not.  E.g. imagine a 3D scene with a GUI overlaid.  The 3D scene is rendered by ModelBatch with a background colour, but
the GUI is rendered via SpriteBatch without clearing the screen.

Creation of a render pass takes place in SpriteBatch.begin() and ModelBatch.begin().

There are now two ways to set the background colour:

ScreenUtils.clear(Color clear);

This sets the default clear colour.  A null value indicates not to clear the screen.

SpriteBatch.begin(Color clear) takes a parameter Color clearColor to provide the background colour, a null indicates to use the default clear color.

If ScreenUtils.clear() is not called and no color value is provided to SpriteBatxh.begin() the screen will be cleared to black.

An extra parameter was also added to ModelBatch.begin().


MSAA
====

Multisamping can be enabled by setting the application configuration numSamples to 4.  This lets Application create a multisampling texture.
ModelBatch will create a render pass that does multi-sampling.  Pipelines will adapt to the RenderPass.



## Using HWND
There is some interaction between GLFW and WebGPU in order to get a surface corresponding to the window created by GLFW.
Elie Michel the following function for the conversion.
```/**
 * Get a WGPUSurface from a GLFW window.
 */
WGPUSurface glfwGetWGPUSurface(WGPUInstance instance, GLFWwindow* window);
```

This didn't really work from the Java layer by regarding HWND as a pointer.  It is better to regard it as an integer, get the HWND
for the GLFW Window at Java level and pass that to a C util function to get a surface.



## GLTF support

We can load and display GLTF models now.
Not supported: skins, animation, camera.
GLTF Separate and GLB formats are supported, not GTLF embedded.
Normal maps and Emissive maps are supported.


