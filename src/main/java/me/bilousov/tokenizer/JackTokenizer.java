package me.bilousov.tokenizer;

import me.bilousov.util.TokenType;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JackTokenizer {

    private static final String tokenRegex = "\"/\\=|\\{|\\}|\\.|\\(|\\)|\\[|\\]|,|;|\\+|-|\\*|\\/|&|\\||<|>|=|~|\\\".+\\\"|\\w+|\\d\"gm";
    private static final List<String> keywords = new ArrayList<>(Arrays.asList("class", "constructor", "function", "method", "field", "static", "var", "int",
            "char", "boolean", "void", "true", "false", "null", "this", "let", "do", "if",
            "else", "while", "return"));
    private static final List<String> symbols = new ArrayList<>(Arrays.asList("{", "}", "(", ")", "[", "]", ".", ",", ";", "+", "-", "*", "/", "&", "|",
            "<", ">", "=", "~"));

    private File currentFile;
    private BufferedReader bufferedFileReader;
    private List<String> tokens;
    private Iterator<String> tokenIterator;
    private String currentToken;

    public JackTokenizer(File jackFile) throws IOException {
        this.currentFile = jackFile;
        this.tokens = new ArrayList<>();
        this.bufferedFileReader = new BufferedReader(new FileReader(currentFile));
        setUpTokens();
        this.tokenIterator = tokens.iterator();
    }

    private void setUpTokens() throws IOException {
        String nextLine = bufferedFileReader.readLine();

        while (nextLine != null) {
            if(!lineIsComment(nextLine)) {
                if(nextLine.contains("//")){
                    nextLine = nextLine.split("//")[0];
                }

                Matcher m = Pattern.compile(tokenRegex).matcher(nextLine);
                while (m.find()){
                    tokens.add(m.group());
                }
            }

            nextLine = bufferedFileReader.readLine();
        }
    }

    private boolean lineIsComment(String line){
        return line.trim().startsWith("/") || line.trim().startsWith("*");
    }

    private boolean isInteger(String token){
        try {
            Integer.parseInt(token);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }

    public boolean hasMoreTokens(){
        return tokenIterator.hasNext();
    }

    public String advance(){
        if(tokenIterator.hasNext()){
            this.currentToken = tokenIterator.next();
        }

        return getCurrentToken();
    }

    public String getCurrentToken(){
        if(tokenType().equals(TokenType.SYMBOL.getXmlLabel())){
            return symbol();
        }

        if(tokenType().equals(TokenType.STRING_CONST.getXmlLabel())){
            return stringVal();
        }

        return currentToken;
    }

    public String tokenType(){
        if(keywords.contains(currentToken)){
            return TokenType.KEYWORD.getXmlLabel();
        }

        if(symbols.contains(currentToken)){
            return TokenType.SYMBOL.getXmlLabel();
        }

        if(isInteger(currentToken)){
            return TokenType.INT_CONST.getXmlLabel();
        }

        if(currentToken.contains("\"")){
            return TokenType.STRING_CONST.getXmlLabel();
        }

        return TokenType.IDENTIFIER.getXmlLabel();
    }

    public String keyWord(){
        return currentToken;
    }

    public String symbol(){
        if(currentToken.equals("<")){
            return "&lt;";
        }

        if(currentToken.equals(">")){
            return "&gt;";
        }

        if(currentToken.equals("\"")){
            return "&quot;";
        }

        if(currentToken.equals("&")){
            return "&amp;";
        }

        return currentToken;
    }

    public String identifier(){
        return currentToken;
    }

    public int intVal(){
        return Integer.parseInt(currentToken);
    }

    public String stringVal(){
        String stringVal = currentToken;

        return stringVal.replace("\"", "");
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
    }
}
