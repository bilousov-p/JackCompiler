package me.bilousov.writer;

import me.bilousov.tokenizer.JackTokenizer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class XMLWriter {

    private static final String INPUT_FILE_EXTENSION = ".jack";
    private static final String OUTPUT_FILE_EXTENSION = ".vm";
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public static void writeXMLForTokenizer(JackTokenizer tokenizer, File file) throws IOException {
        List<String> xmlLines = new ArrayList<>();
        xmlLines.add("<tokens>");

        while (tokenizer.hasMoreTokens()){
            tokenizer.advance();
            xmlLines.add("<" + tokenizer.xmlTokenType() + ">" + " " + tokenizer.getCurrentToken() + " </" + tokenizer.xmlTokenType() + ">");
        }

        xmlLines.add("</tokens>");
        writeXMLFileWithLines(xmlLines, file);
    }

    public static void writeXMLFileWithLines(List<String> binaryLines, File file) throws IOException {
        String outputFilePath = file.getPath().replace(INPUT_FILE_EXTENSION, OUTPUT_FILE_EXTENSION);

        FileOutputStream fos = new FileOutputStream(outputFilePath);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

        for (String line : binaryLines) {
            bw.write(line + LINE_SEPARATOR);
        }

        bw.close();
    }
}
