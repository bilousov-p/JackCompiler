package me.bilousov.engine;

import me.bilousov.tokenizer.JackTokenizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompilationEngine {

    private JackTokenizer tokenizer;
    private List<String> compiledLines;
    private StringBuilder indentation;
    private List<String> statementTokens;
    private List<String> opTokens;

    public CompilationEngine(JackTokenizer tokenizer){
        this.tokenizer = tokenizer;
        this.compiledLines = new ArrayList<>();
        this.indentation = new StringBuilder();
        this.statementTokens = new ArrayList<>(Arrays.asList("let", "if", "while", "do", "return"));
        this.opTokens = new ArrayList<>(Arrays.asList("+", "-", "*", "/", "&amp;", "|", "&lt;", "&gt;", "="));
        this.tokenizer.advance();
    }

    public List<String> compileClass(){
        writeOpenTag("class");
        increaseIndentLevel();

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType());
        writeTokenToXML(tokenizer.advance(), tokenizer.tokenType());
        writeTokenToXML(tokenizer.advance(), tokenizer.tokenType());

        String nextToken = tokenizer.advance();
        if(nextToken.equals("static") || nextToken.equals("field")){
            while(tokenizer.getCurrentToken().equals("static") || tokenizer.getCurrentToken().equals("field")) {
                compileClassVarDec();
            }
        }

        if(tokenizer.getCurrentToken().equals("constructor") || tokenizer.getCurrentToken().equals("function") || tokenizer.getCurrentToken().equals("method")){
            while(tokenizer.getCurrentToken().equals("constructor") || tokenizer.getCurrentToken().equals("function") || tokenizer.getCurrentToken().equals("method")) {
                compileSubroutineDec();
            }
        }

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType());
        decreaseIndentLevel();
        writeCloseTag("class");
        return compiledLines;
    }

    public void compileClassVarDec(){
        writeOpenTag("classVarDec");
        increaseIndentLevel();

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType());
        writeTokenToXML(tokenizer.advance(), tokenizer.tokenType());
        writeTokenToXML(tokenizer.advance(), tokenizer.tokenType());

        while(tokenizer.advance().equals(",")){
            writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType());
            writeTokenToXML(tokenizer.advance(), tokenizer.tokenType());
        }

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType());
        tokenizer.advance();
        decreaseIndentLevel();
        writeCloseTag("classVarDec");
    }

    public void compileSubroutineDec(){
        writeOpenTag("subroutineDec");
        increaseIndentLevel();

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType());
        writeTokenToXML(tokenizer.advance(), tokenizer.tokenType());
        writeTokenToXML(tokenizer.advance(), tokenizer.tokenType());
        writeTokenToXML(tokenizer.advance(), tokenizer.tokenType());

        writeOpenTag("parameterList");
        if(!tokenizer.advance().equals(")")){
            compileParamList();
        }
        writeCloseTag("parameterList");

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType());

        compileSubroutineBody();

        tokenizer.advance();
        decreaseIndentLevel();
        writeCloseTag("subroutineDec");
    }

    public void compileParamList(){
        increaseIndentLevel();

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType());
        writeTokenToXML(tokenizer.advance(), tokenizer.tokenType());

        while(tokenizer.advance().equals(",")){
            writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType());
            writeTokenToXML(tokenizer.advance(), tokenizer.tokenType());
            writeTokenToXML(tokenizer.advance(), tokenizer.tokenType());
        }

        decreaseIndentLevel();
    }

    public void compileSubroutineBody(){
        writeOpenTag("subroutineBody");
        increaseIndentLevel();

        writeTokenToXML(tokenizer.advance(), tokenizer.tokenType());

        if(!tokenizer.advance().equals("}")){
            while(tokenizer.getCurrentToken().equals("var")){
                compileVarDec();
            }

            if(statementTokens.contains(tokenizer.getCurrentToken())){
                compileStatements();
            }
        }

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType());

        decreaseIndentLevel();
        writeCloseTag("subroutineBody");
    }

    public void compileVarDec(){
        writeOpenTag("varDec");
        increaseIndentLevel();

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType());
        writeTokenToXML(tokenizer.advance(), tokenizer.tokenType());
        writeTokenToXML(tokenizer.advance(), tokenizer.tokenType());

        while(tokenizer.advance().equals(",")){
            writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType());
            writeTokenToXML(tokenizer.advance(), tokenizer.tokenType());
        }

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType());
        tokenizer.advance();

        decreaseIndentLevel();
        writeCloseTag("varDec");
    }

    public void compileStatements(){
        writeOpenTag("statements");
        increaseIndentLevel();

        while(statementTokens.contains(tokenizer.getCurrentToken())){
            switch (tokenizer.getCurrentToken()){
                case "let":
                    compileLet();
                    break;
                case "if":
                    compileIf();
                    break;
                case "while":
                    compileWhile();
                    break;
                case "do":
                    compileDo();
                    break;
                case "return":
                    compileReturn();
                    break;
                default:
                    System.out.println("Could not map statement name to call");
            }
        }

        decreaseIndentLevel();
        writeCloseTag("statements");
    }

    public void compileLet(){
        writeOpenTag("letStatement");
        increaseIndentLevel();

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType());
        writeTokenToXML(tokenizer.advance(), tokenizer.tokenType());

        if(tokenizer.advance().equals("[")){
            writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType());
            compileExpression(false);
            writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType());
            tokenizer.advance();
        }

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType());
        compileExpression(false);
        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType());

        tokenizer.advance();

        decreaseIndentLevel();
        writeCloseTag("letStatement");
    }

    public void compileIf(){
        writeOpenTag("ifStatement");
        increaseIndentLevel();

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // if

        writeTokenToXML(tokenizer.advance(), tokenizer.tokenType()); // (
        compileExpression(false);
        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // )

        writeTokenToXML(tokenizer.advance(), tokenizer.tokenType()); // {
        tokenizer.advance();
        compileStatements();
        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // }

        tokenizer.advance();

        if(tokenizer.getCurrentToken().equals("else")){
            writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // else
            writeTokenToXML(tokenizer.advance(), tokenizer.tokenType()); // {
            tokenizer.advance();
            compileStatements();
            writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // }
            tokenizer.advance();
        }

        decreaseIndentLevel();
        writeCloseTag("ifStatement");
    }

    public void compileWhile(){
        writeOpenTag("whileStatement");
        increaseIndentLevel();

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // while
        writeTokenToXML(tokenizer.advance(), tokenizer.tokenType()); // (
        compileExpression(false);
        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // )
        writeTokenToXML(tokenizer.advance(), tokenizer.tokenType()); // {
        tokenizer.advance();
        compileStatements();
        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // }

        tokenizer.advance();

        decreaseIndentLevel();
        writeCloseTag("whileStatement");
    }

    public void compileDo(){
        writeOpenTag("doStatement");
        increaseIndentLevel();

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // do

        String firstToken = tokenizer.advance();
        String firstTokenType = tokenizer.tokenType();

        if(tokenizer.advance().equals("(")){
            writeTokenToXML(firstToken, firstTokenType); // varName
            writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // (
            tokenizer.advance();
            compileExpressionList();
            writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // )
            tokenizer.advance();
        } else {
            writeTokenToXML(firstToken, firstTokenType); // className | varName
            writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // .
            writeTokenToXML(tokenizer.advance(), tokenizer.tokenType()); // subroutineName
            writeTokenToXML(tokenizer.advance(), tokenizer.tokenType()); // (
            tokenizer.advance();
            compileExpressionList();
            writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // )
            tokenizer.advance();
        }

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // ;

        tokenizer.advance();

        decreaseIndentLevel();
        writeCloseTag("doStatement");
    }

    public void compileReturn(){
        writeOpenTag("returnStatement");
        increaseIndentLevel();

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // return

        if(!tokenizer.advance().equals(";")){
            compileExpression(true);
        }

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // ;

        tokenizer.advance();

        decreaseIndentLevel();
        writeCloseTag("returnStatement");
    }

    private void compileExpression(boolean operateWithCurrentToken){
        writeOpenTag("expression");
        increaseIndentLevel();

        compileTerm(operateWithCurrentToken);

        while(opTokens.contains(tokenizer.getCurrentToken())){
            writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // op
            compileTerm(false);
        }

        decreaseIndentLevel();
        writeCloseTag("expression");
    }

    private void compileTerm(boolean operateWithCurrentToken){
        writeOpenTag("term");
        increaseIndentLevel();

        String firstTermToken = operateWithCurrentToken? tokenizer.getCurrentToken() : tokenizer.advance();
        String firstTermTokenType = tokenizer.tokenType();

        if(firstTermToken.equals("(")){
            writeTokenToXML(firstTermToken, firstTermTokenType); // (
            compileExpression(false);
            writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // )

            tokenizer.advance();
        } else if(firstTermToken.equals("-") || firstTermToken.equals("~")){
            writeTokenToXML(firstTermToken, firstTermTokenType); // unaryOp
            tokenizer.advance();
            compileTerm(true);

        } else {
            String secondTermToken = tokenizer.advance();
             if(secondTermToken.equals("[")){
                 writeTokenToXML(firstTermToken, firstTermTokenType); // varName
                 writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // [
                 compileExpression(false);
                 writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // ]

                 tokenizer.advance();
             } else if(secondTermToken.equals("(")){
                 writeTokenToXML(firstTermToken, firstTermTokenType); // varName
                 writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // (
                 tokenizer.advance();
                 compileExpressionList();
                 writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // )
                 tokenizer.advance();
             } else if(secondTermToken.equals(".")){
                 writeTokenToXML(firstTermToken, firstTermTokenType); // className | varName
                 writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // .
                 writeTokenToXML(tokenizer.advance(), tokenizer.tokenType()); // subroutineName
                 writeTokenToXML(tokenizer.advance(), tokenizer.tokenType()); // (
                 tokenizer.advance();
                 compileExpressionList();
                 writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // )
                 tokenizer.advance();
             }
             else {
                 writeTokenToXML(firstTermToken, firstTermTokenType); // constant
             }

        }

        decreaseIndentLevel();
        writeCloseTag("term");
    }

    private void compileExpressionList(){
        writeOpenTag("expressionList");
        increaseIndentLevel();

        if(!tokenizer.getCurrentToken().equals(")")){
            compileExpression(true);

            while (tokenizer.getCurrentToken().equals(",")){
                writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // ,
                compileExpression(false);
            }
        }

        decreaseIndentLevel();
        writeCloseTag("expressionList");
    }

    private void increaseIndentLevel(){
        indentation.append("\t");
    }

    private void decreaseIndentLevel(){
        indentation.deleteCharAt(0);
    }

    private void writeTokenToXML(String token, String tagName){
        compiledLines.add(indentation.toString() + "<" + tagName + "> " + token + " </" + tagName + ">");
    }

    private void writeOpenTag(String tagName){
        compiledLines.add(indentation.toString() + "<" + tagName + ">");
    }

    private void writeCloseTag(String tagName){
        compiledLines.add(indentation.toString() + "</" + tagName + ">");
    }
}
