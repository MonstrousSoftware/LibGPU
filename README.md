# LibGPU

Java wrapper for WebGPU with a libGDX style API.


## Modules
- ```assets```  Assets folder used by tests
- ```docs```  Some documentation
- ```libGPU``` Java library to call WebGPU
- ```tests```   Tests and samples of using LibGPU

This project depends on the library `java-to-webgpu` to provide a Java native interface to WebGPU functions from Google DAWN.

## Introduction
The objective of this project is to provide a proof of concept of using native WebGPU functions from a Java API layer similar to that provided by libGDX, a popular library for game development.
It is focused only on the graphics aspect, i.e. no support for network, audio, maths, Tiled maps, etc.
Only desktop on Windows is supported, no Android, IOS or web backends.    

Warning:  This project is under active development and breaking changes may be introduced at any time.

## Tests

The tests folder contains a number of demos and tests of various aspects and gives examples of LibGPU applications. (See [show case](docs/user documentation/showcase.md)).
Typically, the application's main class is the Launcher, which sets the application configuration, creates an ApplicationAdapter and launches it.
The Menu class is convenient to call demo applications.  By convention, the Escape key usually closes the demo application and returns to the menu.

## Features
The following features are supported:
- SpriteBatch
- Sprite
- ShapeRenderer (only few shapes)
- BitmapFont, including support for SDF fonts
- Texture, TextureRegion
- Viewports (except FillViewport)
- Camera, PerspectiveCamera, OrthographicCamera
- Basic camera controller
- Game / Screen interface
- Simple preprocessor for shader sources (note: shaders are written in WGSL).
- GLTF loader (also GLB format) (no animations, no extensions)
- OBJ loader
- MeshBuilder to create meshes in code, some shape builders
- PBR rendering
- SkyBox
- Directional lights, point lights.
- Shadows
- GPU timing
- Experimental UI: Scene2d (partial), JLay (flexbox style layout) (very much work in progress)


