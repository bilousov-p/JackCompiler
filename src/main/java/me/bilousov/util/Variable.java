package me.bilousov.util;

public class Variable {

    private String name;
    private String type;
    private String kind;
    private VariableScope scope;
    private int varOrderNumber;

    public Variable() {
    }

    public Variable(String name, String type, String kind, VariableScope scope, int varOrderNumber) {
        this.name = name;
        this.type = type;
        this.kind = kind;
        this.scope = scope;
        this.varOrderNumber = varOrderNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public VariableScope getScope() {
        return scope;
    }

    public void setScope(VariableScope scope) {
        this.scope = scope;
    }

    public int getVarOrderNumber() {
        return varOrderNumber;
    }

    public void setVarOrderNumber(int varOrderNumber) {
        this.varOrderNumber = varOrderNumber;
    }


    @Override
    public String toString() {
        return "Variable {" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", kind='" + kind + '\'' +
                ", scope=" + scope +
                '}';
    }
}
