

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
            spec.shader = new ShaderProgram(spec.shaderSourceFile, prefix);
        }


We also aim to adapt the shader to the actual vertex attributes and material attributes with an uber-shader approach.  The idea is to generate a shader prefix with constants
to direct the conditional compilation of the shader code.

SpriteBatch can perhaps serve as a simplified testing ground. Imagine some rectangles with position&color, some with position&color&texture and some with position&texture.
The shader and vertex attributes could be adapted to these cases.

SpriteBatch was updated to adapt the shader and the vertex buffer contents to the value of VertexAttributes (which is however still hardcoded in the constructor).
There is now a method setVertexAttributes() which allows to change the format on the fly (between begin and end).
Not a very useful or user-friendly method, but it demonstrates a proof of concept: changing pipelines and shaders during a batch.



