
Performance metric of SpriteBatch

8000 sprites (all the same texture) of 32x32 pixels on a 1200x800 window

    LibGDX 1.13.1: 1100 fps
    
    WebGPU: 1500 fps

Potential further improvements:
- implement color packing instead of sending 4 floats per vertex send one packed float



public static float toFloatBits (int r, int g, int b, int a) {
int color = (a << 24) | (b << 16) | (g << 8) | r;
float floatColor = NumberUtils.intToFloatColor(color);
return floatColor;
}