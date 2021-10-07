package me.bilousov.engine;

import me.bilousov.tokenizer.JackTokenizer;

import java.util.ArrayList;
import java.util.List;

public class CompilationEngine {

    private JackTokenizer tokenizer;
    private List<String> compiledLines;

    public CompilationEngine(JackTokenizer tokenizer){
        this.tokenizer = tokenizer;
        this.compiledLines = new ArrayList<>();
        this.tokenizer.advance();
    }

    public List<String> compileClass(){
        return compiledLines;
    }
}
