package com.monstrous.graphics.loaders;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

// to load a bin file
public class GLTFRawBuffer {
    public String path;
    public byte[] data;
    //public int byteSize;
    public ByteBuffer byteBuffer;

    public GLTFRawBuffer(String filePath) {
        this.path = filePath;

        try {
            data = Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            throw new RuntimeException("Could not read binary file "+filePath);
        }
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
