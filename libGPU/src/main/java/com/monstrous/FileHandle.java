package com.monstrous;


import java.io.File;
import java.io.FileInputStream;
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
        if(type == Files.FileType.Classpath){
            InputStream input = FileHandle.class.getResourceAsStream("/" + file.getPath().replace('\\', '/'));
            if (input == null)
                throw new RuntimeException("File not found: " + file + " (" + type + ")");
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

}
