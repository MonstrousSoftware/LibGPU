
Performance metric of SpriteBatch

8000 sprites (all the same texture) of 32x32 pixels on a 1200x800 window

    LibGDX 1.13.1: 1100 fps
    
    WebGPU: 1500 fps

Potential further improvements:
- implement color packing instead of sending 4 floats per vertex send one packed float

