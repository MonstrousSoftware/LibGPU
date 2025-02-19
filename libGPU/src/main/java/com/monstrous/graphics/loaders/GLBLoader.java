package com.monstrous.graphics.loaders;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;


// Parser of the GLB format (binary GLTF) into a GLTF object

public class GLBLoader {

    public static GLTF load(String filePath) {
        int slash = filePath.lastIndexOf('/');
        String path = filePath.substring(0, slash + 1);
        String name = filePath.substring(slash + 1);

        byte[] contents;
        try {
            contents = Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            throw new RuntimeException("Could not read GLB file "+filePath);
        }
        return parseBinaryFile( filePath, path, contents);
    }

    private static GLTF parseBinaryFile( String name, String path, byte[] contents ){
        ByteBuffer bb = ByteBuffer.wrap( contents );
        bb.order(ByteOrder.LITTLE_ENDIAN);

        bb.rewind();

        // parse header
        int magic = bb.getInt();
        int version = bb.getInt();
        int len = bb.getInt();

//        System.out.println("Magic: "+Integer.toHexString(magic)); //  0x46546C67
//        System.out.println("Version: "+version);
//        System.out.println("Length: "+len);

        if(magic != 0x46546C67)
            throw new RuntimeException("GLB file invalid: "+name);
        if(version != 2)
            System.out.println("Warning: GLB version unsupported (!=2) : "+name);
        if(len != contents.length)
            throw new RuntimeException("GLB file length invalid: "+name);

        // read chunk
        int chunkLength = bb.getInt();
        int chunkType = bb.getInt();

//        System.out.println("Chunk length: "+chunkLength+" type: "+Integer.toHexString(chunkType));
        if(chunkType != 0x4e4f534a) // "JSON"
            throw new RuntimeException("GLB file invalid, first chunk must be type JSON: "+name);


        Charset charset = StandardCharsets.US_ASCII;
        ByteBuffer chunkData = bb.slice();
        chunkData.limit(chunkLength);
        CharBuffer charBuffer = charset.decode(chunkData);
        char[] jsonArray = new char[chunkLength];
        charBuffer.get(jsonArray);
        String json = new String(jsonArray);

        //System.out.println("JSON ["+json+"]");

        bb.position(bb.position() + chunkLength);

        chunkLength = bb.getInt();
        chunkType = bb.getInt();
        if(chunkType != 0x4E4942) // "BIN"
            throw new RuntimeException("GLB file invalid, second chunk must be type BIN: "+name);

        //System.out.println("Chunk length: "+chunkLength+" type: "+Integer.toHexString(chunkType));

        ByteBuffer chunkBinaryData = bb.slice();
        chunkBinaryData.limit(chunkLength);

        GLTF gltf = GLTFLoader.parseJSON(json, path);
        gltf.rawBuffer = new GLTFRawBuffer(chunkBinaryData);
        return gltf;
    }
}
