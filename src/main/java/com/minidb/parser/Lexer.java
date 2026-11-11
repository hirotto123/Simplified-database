package com.minidb.parser;

import java.util.ArrayList;
import java.util.List;

public class Lexer {

    public List<Token> tokenize(String sql) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int n = sql.length();

        while (i < n) {
            char c = sql.charAt(i);

            if (Character.isWhitespace(c)) { i++; continue; }

            if (c == '(') { tokens.add(new Token(Token.Kind.LPAREN, "(")); i++; continue; }
            if (c == ')') { tokens.add(new Token(Token.Kind.RPAREN, ")")); i++; continue; }
            if (c == ',') { tokens.add(new Token(Token.Kind.COMMA, ",")); i++; continue; }
            if (c == '*') { tokens.add(new Token(Token.Kind.STAR, "*")); i++; continue; }
            if (c == ';') { tokens.add(new Token(Token.Kind.SEMICOLON, ";")); i++; continue; }

            if (c == '=' || c == '<' || c == '>' || c == '!') {
                int start = i;
                i++;
                if (i < n && sql.charAt(i) == '=') i++;
                tokens.add(new Token(Token.Kind.OP, sql.substring(start, i)));
                continue;
            }

            if (c == '\'') {
                int start = i;
                i++;
                while (i < n && sql.charAt(i) != '\'') i++;
                if (i < n) i++;
                tokens.add(new Token(Token.Kind.STRING, sql.substring(start, i)));
                continue;
            }

            if (Character.isDigit(c) || c == '-' || c == '+') {
                int start = i;
                if (c == '-' || c == '+') i++;
                while (i < n && (Character.isDigit(sql.charAt(i)) || sql.charAt(i) == '.')) i++;
                tokens.add(new Token(Token.Kind.NUMBER, sql.substring(start, i)));
                continue;
            }

            if (Character.isLetter(c) || c == '_') {
                int start = i;
                while (i < n && (Character.isLetterOrDigit(sql.charAt(i)) || sql.charAt(i) == '_')) i++;
                tokens.add(new Token(Token.Kind.WORD, sql.substring(start, i)));
                continue;
            }

            throw new IllegalArgumentException("Unexpected char: " + c);
        }

        tokens.add(new Token(Token.Kind.EOF, ""));
        return tokens;
    }
}