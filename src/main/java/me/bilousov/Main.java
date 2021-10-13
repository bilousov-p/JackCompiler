package me.bilousov;

import me.bilousov.engine.CompilationEngine;
import me.bilousov.tokenizer.JackTokenizer;
import me.bilousov.writer.XMLWriter;

import java.io.*;


public class Main {

    public static void main(String[] args) throws IOException {
        File jackFileOrDir = new File(args[0]);

        if (jackFileOrDir.isDirectory()) {
            for (File file : jackFileOrDir.listFiles()) {
                if (file.getName().endsWith(".jack")) {
                    JackTokenizer tokenizer = new JackTokenizer(file);
                    CompilationEngine compilationEngine = new CompilationEngine(tokenizer);
                    XMLWriter.writeXMLFileWithLines(compilationEngine.compileClass(), file);
                }
            }
        } else {
            JackTokenizer tokenizer = new JackTokenizer(jackFileOrDir);
            CompilationEngine compilationEngine = new CompilationEngine(tokenizer);
            XMLWriter.writeXMLFileWithLines(compilationEngine.compileClass(), jackFileOrDir);
        }
    }
}
