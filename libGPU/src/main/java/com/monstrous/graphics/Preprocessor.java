package com.monstrous.graphics;

import java.util.HashMap;
import java.util.Map;

public class Preprocessor {

    private Map<String,String> defineMap = new HashMap<>();

    public void test(){
        String out = process(
      "abcd\n#define A\n#define B 456\n" +
              "#ifdef A\nyes1\n" +
                "#ifdef B\nyes2\n#else\nno\n#endif\nyes3\n" +
              "#else\nno, inside main else\n" +
                "#ifdef B\nno\n#else\nno\n#endif\nno\n" +
              "#endif\n" +
              "yes4\n");
        System.out.println(out);
    }

    public String process(String input){
        defineMap.clear();
        StringBuffer output = new StringBuffer();
        int nestDepth = 0;
        int ignoreLevel = 0;

        String lines[] = input.split("\n");
        for(String line : lines){
            String trimmed = line.trim();

            if(trimmed.startsWith("#ifdef")){
                nestDepth++;
                if(ignoreLevel == 0) {
                    String words[] = trimmed.split("[ \t]");
                    if(!defined(words[1]))
                        ignoreLevel = nestDepth;
                }
            } else if(trimmed.startsWith("#ifndef")){
                nestDepth++;
                if(ignoreLevel == 0) {
                    String words[] = trimmed.split("[ \t]");
                    if(defined(words[1]))
                        ignoreLevel = nestDepth;
                }
            } else if(trimmed.startsWith("#else")){
                if (ignoreLevel == nestDepth)
                    ignoreLevel = 0;
                else if (ignoreLevel == 0)
                    ignoreLevel = nestDepth;

            } else if(trimmed.startsWith("#endif")){
                if(ignoreLevel == nestDepth)
                    ignoreLevel = 0;
                nestDepth--;
            }
            else if(ignoreLevel == 0) {
                if (trimmed.startsWith("#define")) {
                    String words[] = trimmed.split("[ \t]");
                    if(words.length == 2)
                        define(words[1], null);
                    else
                        define(words[1], words[2]);
                }
                else
                    output.append(line).append('\n');

            }
            //output.append(nestDepth).append(':').append(branchTaken).append(line).append('\n');

        }
        return output.toString();
    }
    /*
    if(true)
        y
        if(false)
            n
        else
            y
        endif
     else
        n
        if(true)
            n
        else
            n
        endif
     endif
*/
    // b may be null, e.g. #define DEBUG
    private void define(String a, String b){
        defineMap.put(a, b);
        System.out.print("#define "+a);
        if(b != null)
            System.out.print(" := "+b);
        System.out.println();
    }

    private boolean defined(String name){
        return defineMap.containsKey(name);
    }

    private boolean evaluate(String expr){
        if(expr.contentEquals("true"))
            return true;
        else
            return false;
    }
}
