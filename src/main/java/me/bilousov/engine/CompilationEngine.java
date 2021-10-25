package me.bilousov.engine;

import me.bilousov.tokenizer.JackTokenizer;
import me.bilousov.util.TokenType;
import me.bilousov.util.Variable;
import me.bilousov.util.VariableScope;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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

    private int whileLabelId = 0;
    private int ifLabelId = 0;
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
        return compiledLines;
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
            varWithSameDec.setKind(variable.getKind(false));
            varWithSameDec.setVarOrderNumber(getFieldOrderNumber(variable.getKind(false)));
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
        boolean isConstructor = tokenizer.getCurrentToken().equals("constructor");
        boolean isVoid = tokenizer.advance().equals("void");

        String subroutineName = tokenizer.advance();

        writeTokenToXML(tokenizer.advance(), tokenizer.xmlTokenType());

        writeOpenTag("parameterList");
        if(!tokenizer.advance().equals(")")){
            compileParamList(isMethod);
        }
        writeCloseTag("parameterList");

        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType());

        compileSubroutineBody(subroutineName, isConstructor, isVoid, isMethod);

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

    public void compileSubroutineBody(String subroutineName, boolean isConstructor, boolean isVoid, boolean isMethod){
        tokenizer.advance();

        if(!tokenizer.advance().equals("}")){
            AtomicInteger subroutineVarsCount = new AtomicInteger(0);
            while(tokenizer.getCurrentToken().equals("var")){
                compileVarDec(subroutineVarsCount);
            }

            writeVMCommand("function " + fileName + "." + subroutineName + " " + subroutineVarsCount, TokenType.IDENTIFIER, false);

            if(isConstructor){
                writeVMCommand("push constant " + symbolTable.stream().filter(v -> v.getKind(false).equals("field")).count(), TokenType.IDENTIFIER, false);
                writeVMCommand("call Memory.alloc 1", TokenType.IDENTIFIER, false);
                writeVMCommand("pop pointer 0", TokenType.IDENTIFIER, false);
            }

            if(isMethod){
                writeVMCommand("push argument 0", TokenType.IDENTIFIER, false);
                writeVMCommand("pop pointer 0", TokenType.IDENTIFIER, false);
            }

            if(statementTokens.contains(tokenizer.getCurrentToken())){
                compileStatements(isConstructor, isVoid);
            }
        }
    }

    public void compileVarDec(AtomicInteger subroutineVarsCount){
        Variable variable = new Variable();
        variable.setKind("local");
        variable.setVarOrderNumber(subroutineVarsCount.getAndIncrement());
        variable.setType(tokenizer.advance());
        variable.setName(tokenizer.advance());
        variable.setScope(VariableScope.SUBROUTINE);

        localSymbolTable.add(variable);

        while(tokenizer.advance().equals(",")){
            Variable varWithSameDec = new Variable();
            varWithSameDec.setKind(variable.getKind(false));
            varWithSameDec.setVarOrderNumber(subroutineVarsCount.getAndIncrement());
            varWithSameDec.setType(variable.getType());
            varWithSameDec.setScope(variable.getScope());
            varWithSameDec.setName(tokenizer.advance());

            localSymbolTable.add(varWithSameDec);
        }

        tokenizer.advance();
    }

    public void compileStatements(boolean isConstructor, boolean isVoid){
        writeOpenTag("statements");
        increaseIndentLevel();

        while(statementTokens.contains(tokenizer.getCurrentToken())){
            switch (tokenizer.getCurrentToken()){
                case "let":
                    compileLet();
                    break;
                case "if":
                    compileIf(isConstructor, isVoid);
                    break;
                case "while":
                    compileWhile(isConstructor, isVoid);
                    break;
                case "do":
                    compileDo(isVoid);
                    break;
               case "return":
                   compileReturn(isConstructor, isVoid);
                   break;
                default:
                    System.out.println("Could not map statement name to call");
            }
        }

        decreaseIndentLevel();
        writeCloseTag("statements");
    }

    public void compileLet(){
        String identifier = tokenizer.advance();

        Variable variable = localSymbolTable
                .stream()
                .filter(v -> v.getName().equals(identifier))
                .findFirst()
                .orElse(symbolTable.stream().filter(v -> v.getName().equals(identifier)).findFirst().orElse(null));

        if(tokenizer.advance().equals("[")){
            writeVMCommand("push " + variable.getKind(true) + " " + variable.getVarOrderNumber(), TokenType.IDENTIFIER, false);
            compileExpression(false);
            writeVMCommand("add", TokenType.IDENTIFIER, false);

            tokenizer.advance();

            compileExpression(false);
            writeVMCommand("pop temp 0", TokenType.IDENTIFIER, false);
            writeVMCommand("pop pointer 1", TokenType.IDENTIFIER, false);
            writeVMCommand("push temp 0", TokenType.IDENTIFIER, false);
            writeVMCommand("pop that 0", TokenType.IDENTIFIER, false);

        } else {
            compileExpression(false);

            writeVMCommand("pop " + variable.getKind(true) + " " + variable.getVarOrderNumber(), TokenType.IDENTIFIER, false);
        }

        tokenizer.advance();
    }


    public void compileIf(boolean isConstructor, boolean isVoid){
        String ifLabel = getIfLabel();
        String ifEndLabel = getIfLabel();

        tokenizer.advance();

        compileExpression(false);
        writeVMCommand("not", TokenType.IDENTIFIER, false);
        writeVMCommand("if-goto " + ifLabel, TokenType.IDENTIFIER, false);

        tokenizer.advance();
        tokenizer.advance();

        compileStatements(isConstructor, isVoid);
        writeVMCommand("goto " + ifEndLabel, TokenType.IDENTIFIER, false);

        tokenizer.advance();

        writeVMCommand("label " + ifLabel, TokenType.IDENTIFIER, false);

        if(tokenizer.getCurrentToken().equals("else")){
            tokenizer.advance();
            tokenizer.advance();

            compileStatements(isConstructor, isVoid);

            tokenizer.advance();
        }

        writeVMCommand("label " + ifEndLabel, TokenType.IDENTIFIER, false);
    }

    public void compileWhile(boolean isConstructor, boolean isVoid){
        String whileLabel = getWhileLabel();
        String whileEndLabel = getWhileLabel();

        tokenizer.advance();

        writeVMCommand("label " + whileLabel, TokenType.IDENTIFIER, false);
        compileExpression(false);
        writeVMCommand("not", TokenType.IDENTIFIER, false);
        writeVMCommand("if-goto " + whileEndLabel, TokenType.IDENTIFIER, false);

        tokenizer.advance();
        tokenizer.advance();

        compileStatements(isConstructor, isVoid);
        writeVMCommand("goto " + whileLabel, TokenType.IDENTIFIER, false);
        writeVMCommand("label " + whileEndLabel, TokenType.IDENTIFIER, false);

        tokenizer.advance();
    }

    private String getWhileLabel() {
        return "WHILE_" + whileLabelId++;
    }

    private String getIfLabel(){
        return "IF_" + ifLabelId++;
    }

    public void compileDo(boolean isVoid){
        String firstToken = tokenizer.advance();

        if(tokenizer.advance().equals("(")){
            tokenizer.advance();

            writeVMCommand("push pointer 0", TokenType.IDENTIFIER, false);
            int argsCount = compileExpressionList() + 1;
            writeVMCommand("call " + fileName + "." + firstToken + " " + argsCount, TokenType.IDENTIFIER, false);

        } else {
            int argsCount = 0;
            Variable variable = localSymbolTable
                    .stream()
                    .filter(v -> v.getName().equals(firstToken))
                    .findFirst()
                    .orElse(symbolTable.stream().filter(v -> v.getName().equals(firstToken)).findFirst().orElse(null));

            if (variable != null){
                argsCount++;
                writeVMCommand("push " + variable.getKind(true) + " " + variable.getVarOrderNumber(), TokenType.IDENTIFIER, false);
            } else {
                variable = new Variable(firstToken);
                variable.setType(firstToken);
            }

            String subroutineName = tokenizer.advance();
            tokenizer.advance();
            tokenizer.advance();

            argsCount+=compileExpressionList();
            writeVMCommand("call " + variable.getType() + "." + subroutineName + " " + argsCount, TokenType.IDENTIFIER, false);
        }

        if(isVoid){
            writeVMCommand("pop temp 0", TokenType.IDENTIFIER, false);
        }

        tokenizer.advance();
        tokenizer.advance();
    }

//    public void compileReturn(){
//        writeOpenTag("returnStatement");
//        increaseIndentLevel();
//
//        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // return
//
//        if(!tokenizer.advance().equals(";")){
//            compileExpression(true);
//        }
//
//        writeTokenToXML(tokenizer.getCurrentToken(), tokenizer.xmlTokenType()); // ;
//
//        tokenizer.advance();
//
//        decreaseIndentLevel();
//        writeCloseTag("returnStatement");
//    }

    public void compileReturn(boolean isConstructor, boolean isVoid){
        if(!tokenizer.advance().equals(";") && !isConstructor && !isVoid){
            compileExpression(true);
        }

        if(isConstructor){
            writeVMCommand("push pointer 0", TokenType.IDENTIFIER, false);
            tokenizer.advance();
        }

        if(isVoid){
            writeVMCommand("push constant 0", TokenType.IDENTIFIER, false);
        }

        writeVMCommand("return", TokenType.IDENTIFIER, false);

        tokenizer.advance();
    }

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
                String arrName = firstTermToken;
                Variable variable = localSymbolTable
                        .stream()
                        .filter(v -> v.getName().equals(arrName))
                        .findFirst()
                        .orElse(symbolTable.stream().filter(v -> v.getName().equals(arrName)).findFirst().orElse(null));

                writeVMCommand("push " + variable.getKind(true) + " " + variable.getVarOrderNumber(), TokenType.IDENTIFIER, false);
                compileExpression(false);
                writeVMCommand("add", TokenType.IDENTIFIER, false);

                tokenizer.advance();
            } else if(secondTermToken.equals("(")){
                tokenizer.advance();

                writeVMCommand("push pointer 0", TokenType.IDENTIFIER, false);

                int argsCount = compileExpressionList() + 1;
                writeVMCommand("call " + fileName + "." + firstTermToken + " " + argsCount, TokenType.IDENTIFIER, false);

                tokenizer.advance();
            } else if(secondTermToken.equals(".")){
                int argsCount = 0;

                Variable variable = localSymbolTable
                        .stream()
                        .filter(v -> v.getName().equals(firstTermToken))
                        .findFirst()
                        .orElse(symbolTable.stream().filter(v -> v.getName().equals(firstTermToken)).findFirst().orElse(null));

                if (variable != null){
                    argsCount++;
                    writeVMCommand("push " + variable.getKind(true) + " " + variable.getVarOrderNumber(), TokenType.IDENTIFIER, false);
                } else {
                    variable = new Variable(firstTermToken);
                    variable.setType(firstTermToken);
                }

                String subroutineName = tokenizer.advance();
                tokenizer.advance();
                tokenizer.advance();

                argsCount+=compileExpressionList();
                writeVMCommand("call " + variable.getType() + "." + subroutineName + " " + argsCount, TokenType.IDENTIFIER, false);
                tokenizer.advance();
            }
            else {
                if(isNumeric(firstTermToken)){
                    writeVMCommand("push constant " + firstTermToken, TokenType.IDENTIFIER, false); // constant
                } else if (firstTermToken.equals("true") || firstTermToken.equals("false")){
                    if(firstTermToken.equals("true")){
                        writeVMCommand("push constant " + 0, TokenType.IDENTIFIER, false); // constant
                        writeVMCommand("not", TokenType.IDENTIFIER, false); // constant
                    } else {
                        writeVMCommand("push constant " + 0, TokenType.IDENTIFIER, false); // constant
                    }
                }
                else {
                    Variable variable = localSymbolTable
                            .stream()
                            .filter(v -> v.getName().equals(firstTermToken))
                            .findFirst()
                            .orElse(symbolTable.stream().filter(v -> v.getName().equals(firstTermToken)).findFirst().orElse(null));

                    if(firstTermToken.equals("this")){
                        writeVMCommand("push pointer 0", TokenType.IDENTIFIER, false);
                    } else {
                        writeVMCommand("push " + variable.getKind(true) + " " + variable.getVarOrderNumber(), TokenType.IDENTIFIER, false);
                    }
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
                    case "&gt;":
                        commandToWrite = "gt";
                        break;
                    case "<":
                    case "&lt;":
                        commandToWrite = "lt";
                        break;
                    case "|":
                        commandToWrite = "or";
                        break;
                    case "&":
                    case "&amp;":
                        commandToWrite = "and";
                        break;
                    case "~":
                        commandToWrite = "not";
                        break;
                    case "*":
                        commandToWrite = "call Math.multiply 2";
                        break;
                    case "/":
                        commandToWrite = "call Math.divide 2";
                        break;
                    default:
                        commandToWrite = "unknown symbol " + command;
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
