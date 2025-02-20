package com.monstrous;

public class Files {

    public enum FileType {
        Internal,
        Classpath
    }



    public static FileHandle internal( String path){
        return new FileHandle(path, FileType.Internal);
    }

    public static FileHandle classpath( String path){
        return new FileHandle(path, FileType.Classpath);
    }


}
