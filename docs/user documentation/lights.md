## Lighting and Environment


Lights are place in an Environment instance, which can be passed as a parameter to ModelBatch#begin().

For example:

    // setup:
    Environment environment = new Environment();
    DirectionalLight light = new DirectionalLight(new Color(1,1,1,1), new Vector3(0,-1,0));
    environment.add( light );

    // in render loop:
    modelBatch.begin(camera, environment);
    modelBatch.render(modelInstance);
    modelBatch.end();


Environment supports up to 5 directional lights and an ambient light level.