/*******************************************************************************
 * Copyright 2025 Monstrous Software.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.monstrous.graphics;

import com.monstrous.FileHandle;
import com.monstrous.Files;
import com.monstrous.graphics.g2d.SpriteBatch;
import com.monstrous.utils.Disposable;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class BitmapFont implements Disposable {

    public static final int MAX_CHARS = 256;

    public FileHandle fontFileHandle;
    public String textureFilePath;
    public Texture fontTexture;
    public Map<Integer, Glyph> glyphMap;
    private int charsCount;
    private int lineHeight;
    private int base;
    private Glyph fallbackGlyph;
    public boolean disableKerning = false;
    private Map<Integer, Integer> kerningMap = new HashMap<>();
    private float scaleX, scaleY;


    public class Glyph {
        int id;
        int x, y, w, h;
        int xoffset, yoffset;
        int xadvance;
        int page;
        int chnl;
        TextureRegion region;
    }

    public BitmapFont(){
        this(Files.classpath("font/lsans-15.fnt"));      // default font
    }

    public BitmapFont(String filePath) {
        this(Files.internal(filePath));
    }

    public BitmapFont(FileHandle fntFileHandle) {

        this.fontFileHandle = fntFileHandle;
        glyphMap = new HashMap<>();
        parseFontFile(fntFileHandle);
        setScale(1f);
    }

    public void draw(SpriteBatch batch, String text, float x, float y){
        byte[] ascii = text.getBytes(StandardCharsets.US_ASCII);
        float gx = x;
        for(int i = 0; i < ascii.length; i++){
            byte k = ascii[i];
            Glyph glyph = glyphMap.get((int)k);
            if(glyph == null)
                glyph = fallbackGlyph;
            batch.draw(glyph.region, gx+glyph.xoffset*scaleX, y - (glyph.h+glyph.yoffset)*scaleY, glyph.w*scaleX, glyph.h*scaleY);
            gx += glyph.xadvance*scaleX;
            if(i < ascii.length-1) {
                byte nextCh = ascii[i + 1];
                gx += getKerning(glyph.id, nextCh)*scaleX;
            }
        }
    }

    public float width(String text){
        byte[] ascii = text.getBytes(StandardCharsets.US_ASCII);
        float gx = 0;
        for(int i = 0; i < ascii.length; i++){
            byte k = ascii[i];
            Glyph glyph = glyphMap.get((int)k);
            if(glyph == null)
                glyph = fallbackGlyph;
            gx += glyph.xadvance*scaleX;
            if(i < ascii.length-1) {
                byte nextCh = ascii[i + 1];
                gx += getKerning(glyph.id, nextCh)*scaleX;
            }
        }
        return gx;
    }

    private void setKerning(int first, int second, int amount){
        int key = (first << 16)| second;
        kerningMap.put(key, amount);
    }

    private int getKerning(int first, int second){
        if(disableKerning)
            return 0;
        int key = (first << 16)| second;
        Integer amount = kerningMap.get(key);
        if(amount == null)
            return 0;
        //System.out.println("kerning: "+(char)id+(char)ch + id+" "+ch+" : "+amount);
        return amount;
    }

    public void setScale(float sx, float sy){
        scaleX = sx;
        scaleY = sy;
    }

    // same scale for X and Y
    public void setScale(float scale){
        setScale(scale, scale);
    }

    public int getLineHeight(){
        return (int) (lineHeight*scaleY);
    }

    private void parseFontFile(FileHandle fileHandle){
        String fileData;
        fileData = fileHandle.readString();
        if(fileData == null)
            throw new RuntimeException("Font file not found: "+fileHandle.file.getPath());

        String filePath = fileHandle.file.getPath();
        int slash = filePath.lastIndexOf('\\');
        String path = filePath.substring(0, slash + 1);

        String[] lines = fileData.split("\n");

        //System.out.println("Fnt lines: "+lines.length);
        for(String line : lines ){
            String trimmed = line.trim();

            //page id=0 file="lsans-15.png"
            if(trimmed.startsWith("page")){
                String[] words = trimmed.split("\"");
                if(words.length < 2)
                    throw new RuntimeException("Invalid page line in fnt file "+path);
                textureFilePath = words[1];
                FileHandle handle = Files.internal(path+textureFilePath);
                fontTexture = new Texture(handle, false);
            } else if(trimmed.startsWith("chars count")){
                // chars count=168
                String[] words = trimmed.split("=");
                if(words.length < 2)
                    throw new RuntimeException("Invalid chars count line in fnt file "+path);
                charsCount = Integer.parseInt(words[1]);
            } else if(trimmed.startsWith("common ")) {
                // common lineHeight=18 base=14 scaleW=256 scaleH=128 pages=1 packed=0
                //
                String[] words = trimmed.split(" ");
                for (String word : words) {
                    String[] vars = word.split("=");
                    if (vars[0].contentEquals("lineHeight"))
                        lineHeight = Integer.parseInt(vars[1]);
                    else if (vars[0].contentEquals("base"))
                        base = Integer.parseInt(vars[1]);
                }
            } else if(trimmed.startsWith("kerning ")) {
                // kerning first=86 second=58 amount=-1
                //
                int first = -1, second = -1, amount = 0;
                String[] words = trimmed.split(" ");
                for (String word : words) {
                    String[] vars = word.split("=");
                    if (vars[0].contentEquals("first"))
                        first = Integer.parseInt(vars[1]);
                    else if (vars[0].contentEquals("second"))
                        second = Integer.parseInt(vars[1]);
                    else if (vars[0].contentEquals("amount"))
                        amount = Integer.parseInt(vars[1]);
                }
                if(first < 0 || second < 0)
                    throw new RuntimeException("Invalid kerning line in fnt file "+path);
                setKerning(first, second, amount);

            } else if(trimmed.startsWith("char ")){
                // char id=33 x=184 y=17 width=5 height=13 xoffset=0 yoffset=2 xadvance=5 page=0 chnl=0
                String[] words = trimmed.split(" ");

                Glyph glyph = new Glyph();
                for(String word : words) {
                    String[] vars = word.split("=");
                    if (vars[0].contentEquals("id"))
                        glyph.id = Integer.parseInt(vars[1]);
                    else if (vars[0].contentEquals("x"))
                        glyph.x = Integer.parseInt(vars[1]);
                    else if (vars[0].contentEquals("y"))
                        glyph.y = Integer.parseInt(vars[1]);
                    else if (vars[0].contentEquals("width"))
                        glyph.w = Integer.parseInt(vars[1]);
                    else if (vars[0].contentEquals("height"))
                        glyph.h = Integer.parseInt(vars[1]);
                    else if (vars[0].contentEquals("xoffset"))
                        glyph.xoffset = Integer.parseInt(vars[1]);
                    else if (vars[0].contentEquals("yoffset"))
                        glyph.yoffset = Integer.parseInt(vars[1]);
                    else if (vars[0].contentEquals("xadvance"))
                        glyph.xadvance = Integer.parseInt(vars[1]);
                    else if (vars[0].contentEquals("page"))
                        glyph.page = Integer.parseInt(vars[1]);
                    else if (vars[0].contentEquals("chnl"))
                        glyph.chnl = Integer.parseInt(vars[1]);
                }
                if(glyph.page != 0)
                    throw new RuntimeException("BitmapFont: multi-page not supported");

                //char id=65 x=80 y=33 width=11 height=13 xoffset=-1 yoffset=2 xadvance=9 page=0 chnl=0
                glyph.region = new TextureRegion(fontTexture,
                            glyph.x,  glyph.y, glyph.w, glyph.h);

                glyphMap.put(glyph.id, glyph);
                if(glyph.id == 0)
                    fallbackGlyph = glyph;

            }
        }

    }

    @Override
    public void dispose() {
        fontTexture.dispose();
    }
}
