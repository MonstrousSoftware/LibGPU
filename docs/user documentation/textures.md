# Textures

A texture is an image that can be put on the screen, used as material for an object. It is also possible to use a texture as render output.
Not all texture contain colours, a texture can be used to hold a depth map.  Textures can hold a series of images at decreasing resolution (mip-mapping). 
Textures can also have more than multiple layers, for example to create a cube map.


## Texture

A texture has a width and a height.  A basic way to create a texture and fill it with a solid colour is as follows:

```java
Texture texture = new Texture(1024, 1024);
texture.fill.(Color.RED);
```

To create a texture to use as render output the following constructor may be handy:

```java
  public Texture(int width, int height, boolean mipMapping, boolean renderAttachment, WGPUTextureFormat format, int numSamples );
```

Oftentimes a texture is created using an image file, for example as follows:

```java
Texture texture = new Texture("bunny.png");
```
This supports file types such as jpg, png, tga, bmp, psd, gif, hdr, pic and pnm.

The size and format of the texture is in this case defined by the image file.

It is also possible to indicate that the image file should be mip-mapped.  This is useful to avoid aliasing effects if the texture is going to be viewed at different sizes.

```java
Texture texture = new Texture("dragon.jpg", true);
```

And it is also possible to read a file via a FileHandle:

```java
Texture texture = new Texture(Files.internal("wolfman.png"), true);
```

It is also possible to provide the file contents as a byte array.
```java
    FileHandle file = Files.internal("apple.png");
    byte[] byteArray = file.readAllBytes();
    Texture texture = Texture(byteArray, "picture of an apple", true);
```



A texture can be filled with a solid colour as we've seen before.  It can also be filled with pixel data:

```java
	byte[] pixels = new pixels[32*32*4];
	// ...pixel array is filled...
	Texture texture = new Texture(32, 32);
	texture.fill.(pixels);
```

Note that the byte array contains only uncompressed pixel data. The array needs to contain exactly enough bytes (width x height x bytes per pixel). 
Pixels are defined row by row, top to bottom, with the pixels for each row defined from left to right.

If the texture format is WGPUTextureFormat.RGBA8Unorm (the default) then there are 4 bytes per pixel and they need to be layed out in the order red, green, blue, alpha.

Note: other formats are not supported.


## Texture Array
A texture array can be created by indicating a number of layers.

For example, to create an array of 3 texture layers:
 
```java
	TextureArray array = new TextureArray(1024, 1024, 3);
```

To create a texture array that is mip-mapped:

```java
	TextureArray array = new TextureArray(1024, 1024, true, 3);
```
A texture array can be created from a set of image files by providing an array of file names.

```java
	public TextureArray(String[] fileNames, boolean mipMapping);
```

There is also a constructor to read a texture array from image files with multiple mip levels. The file names
need to follow a convention that they contain the number of the mip level (0, 1, 2, etc.).

For example if you have image files as follows
- grass0.png (128x128)
- grass1.png (64x64)
- grass3.png (32x32)
- stone0.png (128x128)
- stone1.png (64x64)
- stone2.png (32x32)

 
This will append a number and the extension to each filename. E.g. if the first file name is "grass", the extension is ".png" and there are three mip levels,
then it will read "grass0.png", "grass1.png", "grass2.png" for the different mip levels of the first layer.

The following example reads two texture layers with 3 mip levels each:

```java
   String[] names = { "grass", "stone" };
   TextureArray texArray = new TextureArray(names, ".png", 3); 
```


## CubeMap
A cube map can be created as follows:

```java
   CubeMap cubeMap = new CubeMap(1024, 1024);	
```

This will create a cube map with 6 textures, one for each side. A cube map is a texture array with exactly 6 layers. 
As this is a common case, it has its own class.

A cube map can be created from a set of image files. For this it needs an array of file names providing the cube side images 
in the order X+, X-, Y+, Y-, Z+, Z- (left, right, top, bottom, front, back). 

```java
   public CubeMap(String[] fileNames, boolean mipMapping);
```

There is also a constructor to read a cube map from image files with multiple LOD levels (mip levels):

```java
    public CubeMap(String[] fileNames, String extension, int mipLevels); 
```

This will append a number and the extension to each filename. E.g. if the first file name is "xpos-", the extension is ".png" and lodLevels is 3, 
then it will try to read "xpos-0.png", "xpos-1.png", "xpos-2.png" for the different mip levels of the left side of the cube.

Note: there is also a utility class to create a cube map from an equirectangular image.

```java
   ImageBasedLighting ibl = new ImageBasedLighting();
   CubeMap environmentMap = ibl.buildCubeMapFromEquirectangularTexture(textureEquirectangular, 2048);
```

## Texture3D
Not implemented.


