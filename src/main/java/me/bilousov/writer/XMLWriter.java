package me.bilousov.writer;

import me.bilousov.tokenizer.JackTokenizer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class XMLWriter {

    private static final String INPUT_FILE_EXTENSION = ".jack";
    private static final String OUTPUT_FILE_EXTENSION = ".xml";
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public static void writeXMLForTokenizer(JackTokenizer tokenizer, String path) throws IOException {
        List<String> xmlLines = new ArrayList<>();
        xmlLines.add("<tokens>");

        while (tokenizer.hasMoreTokens()){
            tokenizer.advance();
            xmlLines.add("<" + tokenizer.tokenType() + ">" + " " + tokenizer.getCurrentToken() + " </" + tokenizer.tokenType() + ">");
        }

        xmlLines.add("</tokens>");
        writeXMLFileWithLines(xmlLines, path);
    }

    public static void writeXMLFileWithLines(List<String> binaryLines, String path) throws IOException {
        File vmFile = new File(path);
        String outputFilePath;

        if(vmFile.isDirectory()){
            outputFilePath = path + "\\" + vmFile.getName() + OUTPUT_FILE_EXTENSION;
        } else {
            outputFilePath = path.replace(INPUT_FILE_EXTENSION, OUTPUT_FILE_EXTENSION);
        }

        FileOutputStream fos = new FileOutputStream(outputFilePath);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

        for (String line : binaryLines) {
            bw.write(line + LINE_SEPARATOR);
        }

        bw.close();
    }
}
