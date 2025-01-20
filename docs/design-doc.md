

Adding shadows to the rendering means we now have to make multiple render passes on the same objects. Once to create a depth maps, and once to render the colours.
ModelBatch.begin() create a render pass and ModelBatch.end() ends it.  (In the future we may want to encapsulate this in something like SceneManager, where the multiple
passes are hidden in render()).

We also aim to adapt the shader to the actual vertex attributes and material attributes with an uber-shader approach.  The idea is to generate a shader prefix with constants
to direct the conditional compilation of the shader code.


