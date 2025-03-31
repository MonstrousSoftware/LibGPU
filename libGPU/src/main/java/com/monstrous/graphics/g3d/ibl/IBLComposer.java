
// from gdx-gltf by MGSX-dev
// https://github.com/mgsx-dev/gdx-gltf/tree/master

package com.monstrous.graphics.g3d.ibl;



import com.monstrous.FileHandle;
import com.monstrous.graphics.Color;
import com.monstrous.graphics.Texture;
import com.monstrous.utils.Disposable;
import com.monstrous.graphics.g3d.ibl.RGBE;
import com.monstrous.graphics.g3d.ibl.RGBE.Header;
import com.monstrous.webgpu.WGPUTextureFormat;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.FloatBuffer;

public class IBLComposer implements Disposable {

	public Header hdrHeader;
	private byte[] hdrData;
	private Texture textureRaw;


	public void loadHDR(FileHandle file) throws IOException{
		DataInputStream in = null;
		try{
			in = new DataInputStream(new BufferedInputStream(file.read()));
			hdrHeader = RGBE.readHeader(in);
			hdrData = new byte[hdrHeader.getWidth() * hdrHeader.getHeight() * 4];
			RGBE.readPixelsRawRLE(in, hdrData, 0, hdrHeader.getWidth(), hdrHeader.getHeight());
		}finally{
			if(in != null) in.close();
		}
	}

	@Override
	public void dispose() {
	}
	
	public Texture getHDRTexture() {
		if(textureRaw == null){
			// convert to pixmap applying optional exposure
	        float [] pixel = new float[3];
	        int imageWidth = hdrHeader.getWidth();
	        int imageHeight = hdrHeader.getHeight();
			int numComponents = 4;

	        // XXX
	        boolean classicMode = true;

	        if(classicMode){

				byte[] pixels = new byte[imageWidth * imageHeight * numComponents];
				int index = 0;

	        	for(int y=0 ; y<imageHeight ; y++){
	        		for(int x=0 ; x<imageWidth ; x++){
	        			int idx = (y*imageWidth+x)*4;
	        			RGBE.rgbe2float(pixel, hdrData, idx); // TODO exposure should be done in this call for best precision.

//	        			for(int i=0 ; i<3 ; i++){
//	        				pixel[i] = (float)Math.pow(pixel[i], 0.5f);
//	        			}
						for(int i=0 ; i<3 ; i++)
							pixel[i] = Math.min(pixel[i], 1.0f);			// clamp to be LDR

						pixels[index++] = (byte)(pixel[0] * 255);
						pixels[index++] = (byte)(pixel[1] * 255);
						pixels[index++] = (byte)(pixel[2] * 255);
						pixels[index++] = (byte)255;
	        		}
	        	}
	        	textureRaw = new Texture(imageWidth, imageHeight, false, false, WGPUTextureFormat.RGBA8Unorm, 1);
				textureRaw.fill(pixels);

	        }
//	        else{
//	        	GLOnlyTextureData data = new GLOnlyTextureData(hdrHeader.getWidth(), hdrHeader.getHeight(), 0, GL30.GL_RGB32F, GL30.GL_RGB, GL30.GL_FLOAT);
//	        	textureRaw = new Texture(data);
//	        	FloatBuffer buffer = BufferUtils.newFloatBuffer(imageWidth * imageHeight * 3);
//	        	for(int i=0 ; i<hdrData.length ; i+=4){
//	        		RGBE.rgbe2float(pixels, hdrData, i);
//	        		buffer.put(pixels);
//	        	}
//	        	buffer.flip();
//	        	textureRaw.bind();
//	        	Gdx.gl.glTexImage2D(textureRaw.glTarget, 0, GL30.GL_RGB32F, hdrHeader.getWidth(), hdrHeader.getHeight(), 0, GL30.GL_RGB, GL30.GL_FLOAT, buffer);
//	        }

		}
		return textureRaw;
	}


}
