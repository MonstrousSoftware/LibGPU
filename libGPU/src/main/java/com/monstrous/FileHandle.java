package com.monstrous;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FileHandle {
    public File file;
    public Files.FileType type;


    public FileHandle(File file, Files.FileType type) {
        this.file = file;
        this.type = type;
    }

    public FileHandle (String fileName, Files.FileType type) {
        this.type = type;
        file = new File(fileName);
    }


    public InputStream read(){
        //System.out.println("read("+file.getPath()+") type = "+type+ " file exists? "+file.exists());
        if(type == Files.FileType.Classpath || (type == Files.FileType.Internal && !file.exists())){
            String resourcePath = "/" + file.getPath().replace('\\', '/');
            //System.out.println("resource path: "+resourcePath);
            InputStream input = FileHandle.class.getResourceAsStream(resourcePath);
            if (input == null)
                throw new RuntimeException("File resource not found: " + resourcePath + " (" + type + ")");
            return input;

        } else if(type == Files.FileType.Internal){
            try {
                return new FileInputStream(file);
            } catch (Exception ex) {
                throw new RuntimeException("Error reading file: " + file + " (" + type + ")", ex);
            }
        }
        return null;
    }

    public String readString()  {
        InputStream inputStream = read();
        try {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch( Exception ex) {
            throw new RuntimeException("Error reading file.", ex);
        }
    }

    public byte[] readAllBytes(){
        byte[] allBytes;
        InputStream inputStream = read();
        try {
            allBytes = inputStream.readAllBytes();
        } catch (IOException ex) {
            throw new RuntimeException("Error reading file.", ex);
        }
        return allBytes;
    }

}
