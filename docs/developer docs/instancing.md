## Instancing

A ModelInstance is defined by a Model and a transform.  A ModelInstance can be defined for example as:

    ModelInstance instance = new ModelInstance(model, 0,0,0);       // at position (0,0,0)


    Matrix4 transform = new Matrix4().translate(0,1,0);             // at position (0,1,0)
    ModelInstance instance2 = new ModelInstance(model, transform);


Obsolete: The following is no longer supported as ModelBatch will recognize instances of the same model and apply instancing automatically:

            It is also possible to define that a ModelInstance needs to be rendered as multiple instances using different transforms:
            For this, provide an ArrayList of Matrix4 transforms as parameter in the constructor. For example:
            
            
                ArrayList<Matrix4> transforms = new ArrayList<>();
                transforms.add( new Matrix4().translate(0,2,0) );
                transforms.add( new Matrix4().translate(0,3,0) );
                transforms.add( new Matrix4().translate(0,4,0) );
                ModelInstance instanceMulti = new ModelInstance(model, transforms);
            
                modelBatch.begin(camera);
                modelBatch.render(instanceMulti);       //  = 3 instances
                modelBatch.end();
            
            ModelBatch will render these instances using a single draw call.

When multiple ModelInstances based on the same Model are rendered via ModelBatch, 
these will automatically be combined in a single draw call, even if the constructor with the ArrayList was not used.
So the following code has similar performance to the code above: 

    ModelInstance instance1 = new ModelInstance(model, 0,2,0 );
    ModelInstance instance2 = new ModelInstance(model, 0,3,0 );
    ModelInstance instance3 = new ModelInstance(model, 0,4,0 );

    modelBatch.begin(camera);
    modelBatch.render(instance1);
    modelBatch.render(instance2);
    modelBatch.render(instance3);
    modelBatch.end();


Where a transform Matrix4 or an ArrayList of Matrix4 is provided to the ModelInstance constructor, the ModelInstance keeps a 
reference to the provided matrix/matrices, it does not make a copy.  So the same matrix should not be used for multiple ModelInstances.

    Matrix4 transform = new Matrix4().translate(0,1,0);           
    ModelInstance instance1 = new ModelInstance(model, transform);
    transform.translate(1,0,0);     // WRONG, this also changes transform for instance 1
    ModelInstance instance2 = new ModelInstance(model, transform);

On the other hand, changes in the transforms will automatically be reflected when the model instances are rendered.

If you are making use of the bounding box of a ModelInstance, for example for visibility culling or for collisions, call update() after modifying a transform so that the bounding box is also updated.