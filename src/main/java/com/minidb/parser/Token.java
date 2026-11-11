package com.minidb.parser;

public class Token {
    public enum Kind {
        WORD, NUMBER, STRING, OP, COMMA, LPAREN, RPAREN, STAR, SEMICOLON, EOF
    }

    public final Kind kind;
    public final String text;

    public Token(Kind kind, String text) {
        this.kind = kind;
        this.text = text;
    }

    @Override
    public String toString() { return kind + ":" + text; }
}