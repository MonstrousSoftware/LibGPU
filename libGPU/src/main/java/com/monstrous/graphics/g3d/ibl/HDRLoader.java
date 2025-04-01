
// Based on gdx-gltf by MGSX-dev (IBLComposer class)
// https://github.com/mgsx-dev/gdx-gltf/tree/master

package com.monstrous.graphics.g3d.ibl;



import com.monstrous.FileHandle;
import com.monstrous.graphics.Texture;
import com.monstrous.graphics.g3d.ibl.RGBE.Header;
import com.monstrous.webgpu.WGPUTextureFormat;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class HDRLoader {

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

	public Texture getHDRTexture(boolean asLDR) {
		if(textureRaw == null){
			// convert to pixmap applying optional exposure
	        float [] pixel = new float[3];
	        int imageWidth = hdrHeader.getWidth();
	        int imageHeight = hdrHeader.getHeight();
			int numComponents = 4;

	        if(asLDR){

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
	        else{
				float[] pixels = new float[imageWidth * imageHeight * numComponents];
				int index = 0;
				for(int y=0 ; y<imageHeight ; y++){
					for(int x=0 ; x<imageWidth ; x++){
						int idx = (y*imageWidth+x)*4;
						RGBE.rgbe2float(pixel, hdrData, idx); // TODO exposure should be done in this call for best precision.

						pixels[index++] = pixel[0];
						pixels[index++] = pixel[1];
						pixels[index++] = pixel[2];
						pixels[index++] = 1.0f;
					}
				}
				textureRaw = new Texture(imageWidth, imageHeight, false, false, WGPUTextureFormat.RGBA32Float, 1);
				textureRaw.fill(pixels);
	        }
		}
		return textureRaw;
	}

}
