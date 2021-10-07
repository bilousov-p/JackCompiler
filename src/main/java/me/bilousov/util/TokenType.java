package me.bilousov.util;

public enum TokenType {
    KEYWORD("keyword"), SYMBOL("symbol"), IDENTIFIER("identifier"), INT_CONST("integerConstant"), STRING_CONST("stringConstant");

    private String xmlLabel;

    public String getXmlLabel() {
        return this.xmlLabel;
    }

    TokenType(String label) {
        this.xmlLabel = label;
    }
}
