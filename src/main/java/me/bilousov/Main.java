package me.bilousov;

import me.bilousov.tokenizer.JackTokenizer;
import me.bilousov.writer.XMLWriter;

import java.io.*;


public class Main {

    public static void main(String[] args) throws IOException {
        File jackFileOrDir = new File(args[0]);

        JackTokenizer tokenizer = new JackTokenizer(jackFileOrDir);

        XMLWriter.writeXMLForTokenizer(tokenizer, args[0]);
    }
}
