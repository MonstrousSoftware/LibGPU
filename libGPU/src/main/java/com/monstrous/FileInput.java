package com.monstrous;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class FileInput {

    public List<String> lines = null;

    public FileInput(String fileName) {
        try {
            lines = Files.readAllLines(Paths.get(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String get(int nr) {
        return lines.get(nr);
    }

    public int size() {
        return lines.size();
    }
}
