package me.bilousov;

import me.bilousov.engine.CompilationEngine;
import me.bilousov.tokenizer.JackTokenizer;
import me.bilousov.writer.XMLWriter;

import java.io.*;


public class Main {

    public static void main(String[] args) throws IOException {
        File jackFileOrDir = new File(args[0]);
        JackTokenizer tokenizer = new JackTokenizer(jackFileOrDir);

        if(args.length > 1 && args[1].equals("tokens")){
            XMLWriter.writeXMLForTokenizer(tokenizer, args[0]);
        } else {
            CompilationEngine compilationEngine = new CompilationEngine(tokenizer);
            XMLWriter.writeXMLFileWithLines(compilationEngine.compileClass(), args[0]);
        }
    }
}
