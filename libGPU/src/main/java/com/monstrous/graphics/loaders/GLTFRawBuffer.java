package com.monstrous.graphics.loaders;

import com.monstrous.FileHandle;
import com.monstrous.Files;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


// to load a bin file
public class GLTFRawBuffer {
    public String path;
    public byte[] data;
    //public int byteSize;
    public ByteBuffer byteBuffer;

    public GLTFRawBuffer(String filePath) {
        this.path = filePath;

        FileHandle handle = Files.internal(filePath);
        data = handle.readAllBytes();


//        try {
//            data = Files.readAllBytes(Paths.get(filePath));
//        } catch (IOException e) {
//            throw new RuntimeException("Could not read binary file "+filePath);
//        }
        //byteSize = data.length;
        byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public GLTFRawBuffer(ByteBuffer byteBuffer) {
        this.path = "internal";

        this.byteBuffer = byteBuffer;
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }
}
