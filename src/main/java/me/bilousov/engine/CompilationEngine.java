package me.bilousov.engine;

import me.bilousov.tokenizer.JackTokenizer;
import me.bilousov.util.TokenType;
import me.bilousov.util.Variable;
import me.bilousov.util.VariableScope;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CompilationEngine {

    private final String fileName;

    private final JackTokenizer tokenizer;
    private final List<String> xmlCompiledLines;
    private final List<String> compiledLines;
    private final StringBuilder indentation;
    private final List<String> statementTokens;
    private final List<String> opTokens;
    private final List<Variable> symbolTable;
    private final List<Variable> localSymbolTable;

    private int fieldCount = 0;
    private int staticFieldCount = 0;

    public CompilationEngine(JackTokenizer tokenizer){
        this.fileName = tokenizer.getCurrentFile().getName().split("\\.")[0];
        this.tokenizer = tokenizer;
        this.xmlCompiledLines = new ArrayList<>();
        this.compiledLines = new ArrayList<>();
        this.indentation = new StringBuilder();
        this.statementTokens = new ArrayList<>(Arrays.asList("let", "if", "while", "do", "return"));
        this.opTokens = new ArrayList<>(Arrays.asList("+", "-", "*", "/", "&amp;", "|", "&lt;", "&gt;", "="));
        this.symbolTable = new ArrayList<>();
        this.localSymbolTable = new ArrayList<>();
        this.tokenizer.advance();
    }

    public List<String> compileClass(){
        writeOpenTag("class");
        increaseIndentLevel();

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType());
        writeTokenToXML(tokenizer.advance(), tokenizer.xmlTokenType());
        writeTokenToXML(tokenizer.advance(), tokenizer.xmlTokenType());

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

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType());
        decreaseIndentLevel();
        writeCloseTag("class");
        return xmlCompiledLines;
    }

    public void compileClassVarDec(){
        Variable variable = new Variable();
        variable.setKind(tokenizer.getCurrentToken());
        variable.setVarOrderNumber(getFieldOrderNumber(tokenizer.getCurrentToken()));
        variable.setType(tokenizer.advance());
        variable.setName(tokenizer.advance());
        variable.setScope(VariableScope.CLASS);

        symbolTable.add(variable);

        while(tokenizer.advance().equals(",")){
            Variable varWithSameDec = new Variable();
            varWithSameDec.setKind(variable.getKind());
            varWithSameDec.setVarOrderNumber(getFieldOrderNumber(variable.getKind()));
            varWithSameDec.setType(variable.getType());
            varWithSameDec.setScope(VariableScope.CLASS);
            varWithSameDec.setName(tokenizer.advance());

            symbolTable.add(varWithSameDec);
        }

        tokenizer.advance();
        System.out.println(Arrays.toString(symbolTable.toArray()));
    }

    public void compileSubroutineDec(){
        writeOpenTag("subroutineDec");
        increaseIndentLevel();

        boolean isMethod = tokenizer.getCurrentToken().equals("method");

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType());
        writeTokenToXML(tokenizer.advance(), tokenizer.xmlTokenType());
        writeTokenToXML(tokenizer.advance(), tokenizer.xmlTokenType());
        writeTokenToXML(tokenizer.advance(), tokenizer.xmlTokenType());

        writeOpenTag("parameterList");
        if(!tokenizer.advance().equals(")")){
            compileParamList(isMethod);
        }
        writeCloseTag("parameterList");

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType());

        compileSubroutineBody();

        localSymbolTable.clear();
        tokenizer.advance();
        decreaseIndentLevel();
        writeCloseTag("subroutineDec");
    }

    public void compileParamList(boolean isMethodParams){
        int argVarsCount = 0;

        if(isMethodParams){
            Variable thisVariable = new Variable();
            thisVariable.setType(fileName);
            thisVariable.setName("this");
            thisVariable.setKind("argument");
            thisVariable.setVarOrderNumber(argVarsCount++);
            thisVariable.setScope(VariableScope.SUBROUTINE);

            localSymbolTable.add(thisVariable);
        }

        Variable variable = new Variable();
        variable.setType(tokenizer.getCurrentToken());
        variable.setName(tokenizer.advance());
        variable.setKind("argument");
        variable.setVarOrderNumber(argVarsCount++);
        variable.setScope(VariableScope.SUBROUTINE);

        localSymbolTable.add(variable);

        while(tokenizer.advance().equals(",")){
            Variable nextVar = new Variable();
            nextVar.setType(tokenizer.advance());
            nextVar.setName(tokenizer.advance());
            nextVar.setKind("argument");
            nextVar.setVarOrderNumber(argVarsCount++);
            nextVar.setScope(VariableScope.SUBROUTINE);

            localSymbolTable.add(nextVar);
        }
    }

    public void compileSubroutineBody(){
        writeOpenTag("subroutineBody");
        increaseIndentLevel();

        writeTokenToXML(tokenizer.advance(), tokenizer.xmlTokenType());

        if(!tokenizer.advance().equals("}")){
            AtomicInteger subroutineVarsCount = new AtomicInteger(0);
            while(tokenizer.getCurrentToken().equals("var")){
                compileVarDec(subroutineVarsCount);
            }

            if(statementTokens.contains(tokenizer.getCurrentToken())){
                compileStatements();
            }
        }

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType());

        decreaseIndentLevel();
        writeCloseTag("subroutineBody");
    }

    public void compileVarDec(AtomicInteger subroutineVarsCount){
        System.out.println(subroutineVarsCount);
        Variable variable = new Variable();
        variable.setKind("local");
        variable.setVarOrderNumber(subroutineVarsCount.getAndIncrement());
        variable.setType(tokenizer.advance());
        variable.setName(tokenizer.advance());
        variable.setScope(VariableScope.SUBROUTINE);

        localSymbolTable.add(variable);

        while(tokenizer.advance().equals(",")){
            Variable varWithSameDec = new Variable();
            varWithSameDec.setKind(variable.getKind());
            varWithSameDec.setVarOrderNumber(subroutineVarsCount.getAndIncrement());
            varWithSameDec.setType(variable.getType());
            varWithSameDec.setScope(variable.getScope());
            varWithSameDec.setName(tokenizer.advance());

            localSymbolTable.add(varWithSameDec);
        }

        tokenizer.advance();
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

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType());
        writeTokenToXML(tokenizer.advance(), tokenizer.xmlTokenType());

        if(tokenizer.advance().equals("[")){
            writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType());
            compileExpression(false);
            writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType());
            tokenizer.advance();
        }

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType());
        compileExpression(false);
        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType());

        tokenizer.advance();

        decreaseIndentLevel();
        writeCloseTag("letStatement");
    }

    public void compileIf(){
        writeOpenTag("ifStatement");
        increaseIndentLevel();

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // if

        writeTokenToXML(tokenizer.advance(), tokenizer.xmlTokenType()); // (
        compileExpression(false);
        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // )

        writeTokenToXML(tokenizer.advance(), tokenizer.xmlTokenType()); // {
        tokenizer.advance();
        compileStatements();
        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // }

        tokenizer.advance();

        if(tokenizer.getCurrentToken().equals("else")){
            writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // else
            writeTokenToXML(tokenizer.advance(), tokenizer.xmlTokenType()); // {
            tokenizer.advance();
            compileStatements();
            writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // }
            tokenizer.advance();
        }

        decreaseIndentLevel();
        writeCloseTag("ifStatement");
    }

    public void compileWhile(){
        writeOpenTag("whileStatement");
        increaseIndentLevel();

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // while
        writeTokenToXML(tokenizer.advance(), tokenizer.xmlTokenType()); // (
        compileExpression(false);
        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // )
        writeTokenToXML(tokenizer.advance(), tokenizer.xmlTokenType()); // {
        tokenizer.advance();
        compileStatements();
        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // }

        tokenizer.advance();

        decreaseIndentLevel();
        writeCloseTag("whileStatement");
    }

    public void compileDo(){
        writeOpenTag("doStatement");
        increaseIndentLevel();

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // do

        String firstToken = tokenizer.advance();
        String firstTokenType = tokenizer.xmlTokenType();

        if(tokenizer.advance().equals("(")){
            writeTokenToXML(firstToken, firstTokenType); // varName
            writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // (
            tokenizer.advance();
            compileExpressionList();
            writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // )
            tokenizer.advance();
        } else {
            writeTokenToXML(firstToken, firstTokenType); // className | varName
            writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // .
            writeTokenToXML(tokenizer.advance(), tokenizer.xmlTokenType()); // subroutineName
            writeTokenToXML(tokenizer.advance(), tokenizer.xmlTokenType()); // (
            tokenizer.advance();
            compileExpressionList();
            writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // )
            tokenizer.advance();
        }

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // ;

        tokenizer.advance();

        decreaseIndentLevel();
        writeCloseTag("doStatement");
    }

    public void compileReturn(){
        writeOpenTag("returnStatement");
        increaseIndentLevel();

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // return

        if(!tokenizer.advance().equals(";")){
            compileExpression(true);
        }

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // ;

        tokenizer.advance();

        decreaseIndentLevel();
        writeCloseTag("returnStatement");
    }

//    private void compileExpression(boolean operateWithCurrentToken){
//        writeOpenTag("expression");
//        increaseIndentLevel();
//
//        compileTerm(operateWithCurrentToken);
//
//        while(opTokens.contains(tokenizer.getCurrentToken())){
//            writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // op
//            compileTerm(false);
//        }
//
//        decreaseIndentLevel();
//        writeCloseTag("expression");
//    }

    private void compileExpression(boolean operateWithCurrentToken){
        compileTerm(operateWithCurrentToken);

        while(opTokens.contains(tokenizer.getCurrentToken())){
            String opToken = tokenizer.getCurrentToken();
            TokenType opTokenType = tokenizer.tokenType();

            compileTerm(false);
            writeVMCommand(opToken, opTokenType, false);
        }
    }

//    private void compileTerm(boolean operateWithCurrentToken){
//        writeOpenTag("term");
//        increaseIndentLevel();
//
//        String firstTermToken = operateWithCurrentToken? tokenizer.getCurrentToken() : tokenizer.advance();
//        String firstTermTokenType = tokenizer.tokenType();
//
//        if(firstTermToken.equals("(")){
//            writeTokenToXML(firstTermToken, firstTermTokenType); // (
//            compileExpression(false);
//            writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // )
//
//            tokenizer.advance();
//        } else if(firstTermToken.equals("-") || firstTermToken.equals("~")){
//            writeTokenToXML(firstTermToken, firstTermTokenType); // unaryOp
//            tokenizer.advance();
//            compileTerm(true);
//
//        } else {
//            String secondTermToken = tokenizer.advance();
//             if(secondTermToken.equals("[")){
//                 writeTokenToXML(firstTermToken, firstTermTokenType); // varName
//                 writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // [
//                 compileExpression(false);
//                 writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // ]
//
//                 tokenizer.advance();
//             } else if(secondTermToken.equals("(")){
//                 writeTokenToXML(firstTermToken, firstTermTokenType); // varName
//                 writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // (
//                 tokenizer.advance();
//                 compileExpressionList();
//                 writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // )
//                 tokenizer.advance();
//             } else if(secondTermToken.equals(".")){
//                 writeTokenToXML(firstTermToken, firstTermTokenType); // className | varName
//                 writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // .
//                 writeTokenToXML(tokenizer.advance(), tokenizer.tokenType()); // subroutineName
//                 writeTokenToXML(tokenizer.advance(), tokenizer.tokenType()); // (
//                 tokenizer.advance();
//                 compileExpressionList();
//                 writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.tokenType()); // )
//                 tokenizer.advance();
//             }
//             else {
//                 writeTokenToXML(firstTermToken, firstTermTokenType); // constant
//             }
//
//        }
//
//        decreaseIndentLevel();
//        writeCloseTag("term");
//    }

    private void compileTerm(boolean operateWithCurrentToken){
        String firstTermToken = operateWithCurrentToken? tokenizer.getCurrentToken() : tokenizer.advance();
        String firstTermTokenTypeXMl = tokenizer.xmlTokenType();
        TokenType firstTermTokenType = tokenizer.tokenType();

        if(firstTermToken.equals("(")){
            compileExpression(false);

            tokenizer.advance();
        } else if(firstTermToken.equals("-") || firstTermToken.equals("~")){
            tokenizer.advance();
            compileTerm(true);
            writeVMCommand(firstTermToken, firstTermTokenType, true);

        } else {
            String secondTermToken = tokenizer.advance();
            if(secondTermToken.equals("[")){
                // work with array ???
                // writeTokenToXML(firstTermToken, firstTermTokenTypeXMl); // varName
                // writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // [
                // compileExpression(false);
                // writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // ]

                // tokenizer.advance();
            } else if(secondTermToken.equals("(")){
                tokenizer.advance();
                int argsCount = compileExpressionList();
                writeVMCommand("call " + fileName + "." + firstTermToken + " " + argsCount, TokenType.IDENTIFIER, false);

                tokenizer.advance();
            } else if(secondTermToken.equals(".")){
                // work with objects
                // writeTokenToXML(firstTermToken, firstTermTokenTypeXMl); // className | varName
                // writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // .
                // writeTokenToXML(tokenizer.advance(), tokenizer.xmlTokenType()); // subroutineName
                // writeTokenToXML(tokenizer.advance(), tokenizer.xmlTokenType()); // (
                // tokenizer.advance();
                // compileExpressionList();
                // writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // )
                // tokenizer.advance();
            }
            else {
                if(isNumeric(firstTermToken)){
                    writeVMCommand("push constant " + firstTermToken, TokenType.IDENTIFIER, false); // constant
                } else {
                    Variable variable = localSymbolTable
                            .stream()
                            .filter(v -> v.getName().equals(firstTermToken))
                            .findFirst()
                            .orElse(symbolTable.stream().filter(v -> v.getName().equals(firstTermToken)).findFirst().orElse(null));
                    writeVMCommand("push " + variable.getKind() + " " + variable.getVarOrderNumber(), TokenType.IDENTIFIER, false); // variable
                }

            }

        }
    }

//    private void compileExpressionList(){
//        writeOpenTag("expressionList");
//        increaseIndentLevel();
//
//        if(!tokenizer.getCurrentToken().equals(")")){
//            compileExpression(true);
//
//            while (tokenizer.getCurrentToken().equals(",")){
//                writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // ,
//                compileExpression(false);
//            }
//        }
//
//        decreaseIndentLevel();
//        writeCloseTag("expressionList");
//    }

    private int compileExpressionList(){
        int argsCount = 0;

        if(!tokenizer.getCurrentToken().equals(")")){
            argsCount++;
            compileExpression(true);

            while (tokenizer.getCurrentToken().equals(",")){
                argsCount++;
                compileExpression(false);
            }
        }

        return argsCount;
    }

    private int getFieldOrderNumber(String fieldKind){
        return fieldKind.equals("field") ? fieldCount++ : staticFieldCount++;
    }

    private void increaseIndentLevel(){
        indentation.append("\t");
    }

    private void decreaseIndentLevel(){
        indentation.deleteCharAt(0);
    }

    private void writeVMCommand(String command, TokenType tokenType, boolean unary){
        String commandToWrite;

        switch (tokenType){
            case SYMBOL:
                switch (command){
                    case "-":
                        commandToWrite = unary ? "neg" : "sub";
                        break;
                    case "+":
                        commandToWrite = "add";
                        break;
                    case "=":
                        commandToWrite = "eq";
                        break;
                    case ">":
                        commandToWrite = "gt";
                        break;
                    case "<":
                        commandToWrite = "lt";
                        break;
                    case "|":
                        commandToWrite = "or";
                        break;
                    case "&":
                        commandToWrite = "and";
                        break;
                    case "~":
                        commandToWrite = "not";
                        break;
                    case "*":
                        commandToWrite = "call Math.multiply 2";
                        break;
                    default:
                        commandToWrite = "unknown symbol";
                }
                break;
            default:
                commandToWrite = command;
        }
        compiledLines.add(commandToWrite);
    }

    private void writeTokenToXML(String token, String tagName){
        xmlCompiledLines.add(indentation.toString() + "<" + tagName + "> " + token + " </" + tagName + ">");
    }

    private void writeOpenTag(String tagName){
        xmlCompiledLines.add(indentation.toString() + "<" + tagName + ">");
    }

    private void writeCloseTag(String tagName){
        xmlCompiledLines.add(indentation.toString() + "</" + tagName + ">");
    }

    public static boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }
}
