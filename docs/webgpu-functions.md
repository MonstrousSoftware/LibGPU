# Calling WebGPU functions directly

The framework hides the details of WebGPU.  However, it is possible to call WebGPU functions directly if so desired.

## Functions
The WebGPU functions are defined in the interface WGPU.java and are a direct Java counterpart of the functions from the C header file `webgpu.h`.
For example: the C function `wgpuDeviceCreateBuffer()` is available as Java method `DeviceCreateBuffer` in the `WGPU` interface.
Most but not all functions have been translated.

## Calling WebGPU functions
The functions can be called via LibGPU.wgpu which provides an interface to the native WebGPU library.  There are two native implementations available. 
- wgpu-native, exposing a native interface to the wgpu Rust library developed for Firefox.
- Google’s Dawn, developed for Chrome.

We are making use of Dawn.


There are a few things to keep in mind.  Most WebGPU functions use a single descriptor parameter to pass in the necessary input, rather than many individual parameters.  
This descriptor is a C-style `struct`.  So calling a WebGPU function generally goes like this:
1.	Create a descriptor struct for the type of function you want to call
2.	Fill in the descriptor values
3.	Call the WebGPU function with the descriptor as parameter.

Here is an example in C to create a 16 byte GPU buffer:
```C
  // create a buffer descriptor
  WGPUBufferDescriptor bufferDesc = {};
  // fill in the details of the descriptor
  bufferDesc.nextInChain = nullptr;
  bufferDesc.label = "Some GPU-side data buffer";
  bufferDesc.usage = WGPUBufferUsage_CopyDst | WGPUBufferUsage_CopySrc;
  bufferDesc.size = 16;
  bufferDesc.mappedAtCreation = false;
  // create a buffer
  WGPUBuffer buffer1 = wgpuDeviceCreateBuffer(device, &bufferDesc);
```

For WebGPU to be able to use the descriptor it must be placed in native memory. We cannot use a Java object for this, because the memory layout of a Java object is not defined.
Therefore there are many descriptor types defined as special Java classes that can be placed in native memory use the `createDirect()` method and will access this memory via getters and setters.

```java
        // create a buffer descriptor
        WGPUBufferDescriptor bufferDesc = WGPUBufferDescriptor.createDirect();
        // fill in the details of the descriptor
        bufferDesc.setLabel("Some GPU-side data buffer");
        bufferDesc.setUsage( WGPUBufferUsage.CopyDst | WGPUBufferUsage.CopySrc );
        bufferDesc.setSize( 16 );
        bufferDesc.setMappedAtCreation(0L);
        // create a buffer
        Pointer handle = LibGPU.wgpu.DeviceCreateBuffer(LibGPU.device, bufferDesc);
```


Another thing to note is that objects directly managed by WebGPU are represented by a handle. For example, if you create a GPU buffer the function will return a value that you can later use to refer to this buffer.
In the C header file there are `typedef`s defined to give each type of handle a meaningful type name, for example you would store a texture handle in a variable of type WGPUTexture.

In the Java implementation these handles are all of the same type (`Pointer`) without distinction of the type of handle.
It relies on you not to mix up different objects (e.g. passing a device Pointer instead of an adapter Pointer), because the compiler will not warn you.


To read from or to write to GPU buffers we will also need a block of native memory.
For example you could use `WgpuJava.createFloatArrayPointer` or use a ByteBuffer.

```java
      // working buffer in native memory to use as input to WriteBuffer
      float[] floats = new float[256];
      floatData = WgpuJava.createFloatArrayPointer(floats); 
      // write data to GPU buffer 
      LibGPU.wgpu.QueueWriteBuffer(LibGPU.queue, buffer, 0, floatData, 0);
```

## Convenience Classes
There are also a number of convenience classes which encapsulate a WebGPU concept in a more user friendly Java class. For example:
-	UniformBuffer
-	Pipeline


Note: the WebGPU specification is still under active development and the WebGPU API is not yet fully stable. For example you may encounter versions of the webgpu header file with slight differences.

References:

A step wise tutorial for developing WebGPU applications in C++: 
[WebGPU C++ guide](https://eliemichel.github.io/LearnWebGPU/introduction.html)


