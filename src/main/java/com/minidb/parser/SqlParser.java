package com.minidb.parser;

import com.minidb.model.Column;
import com.minidb.model.DataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SqlParser {
    private final List<Token> tokens;
    private int pos = 0;

    public SqlParser(List<Token> tokens) { this.tokens = tokens; }

    public Query parse() {
        String cmd = expectWord().toUpperCase();
        switch (cmd) {
            case "INSERT": return parseInsert();
            case "SELECT": return parseSelect();
            case "UPDATE": return parseUpdate();
            case "DELETE": return parseDelete();
            case "CREATE": return parseCreate();
            case "DROP":   return parseDrop();
            default: throw new IllegalArgumentException("Unknown command: " + cmd);
        }
    }

    private Query.Insert parseInsert() {
        expectWord("INTO");
        String table = expectWord();

        List<String> columns = null;
        if (peek().kind == Token.Kind.LPAREN) {
            int saved = pos;
            List<String> ids = parseIdentifierList();
            if (peekWordEquals("VALUES")) {
                columns = ids;
            } else {
                pos = saved;
            }
        }

        expectWord("VALUES");
        List<Object> values = parseValueList();
        optionalSemicolon();
        return new Query.Insert(table, columns, values);
    }

    private Query.Select parseSelect() {
        List<String> cols = parseSelectColumns();
        expectWord("FROM");
        String table = expectWord();
        Expression where = null;
        if (peekWordEquals("WHERE")) {
            advance();
            where = parseExpression();
        }
        optionalSemicolon();
        return new Query.Select(table, cols, where);
    }

    private List<String> parseSelectColumns() {
        List<String> cols = new ArrayList<>();
        if (peek().kind == Token.Kind.STAR) {
            advance();
            cols.add("*");
            return cols;
        }
        cols.add(expectWord());
        while (peek().kind == Token.Kind.COMMA) {
            advance();
            cols.add(expectWord());
        }
        return cols;
    }

    private Query.Update parseUpdate() {
        String table = expectWord();
        expectWord("SET");
        Map<String, Object> assigns = new LinkedHashMap<>();
        assigns.put(expectWord(), parseAssignValue());
        while (peek().kind == Token.Kind.COMMA) {
            advance();
            assigns.put(expectWord(), parseAssignValue());
        }
        Expression where = null;
        if (peekWordEquals("WHERE")) {
            advance();
            where = parseExpression();
        }
        optionalSemicolon();
        return new Query.Update(table, assigns, where);
    }

    private Object parseAssignValue() {
        expect("=");
        return parseValue();
    }

    private Query.Delete parseDelete() {
        expectWord("FROM");
        String table = expectWord();
        Expression where = null;
        if (peekWordEquals("WHERE")) {
            advance();
            where = parseExpression();
        }
        optionalSemicolon();
        return new Query.Delete(table, where);
    }

    private Query parseCreate() {
        String what = expectWord().toUpperCase();
        if (what.equals("TABLE")) return parseCreateTable();
        if (what.equals("INDEX")) return parseCreateIndex();
        throw new IllegalArgumentException("Unknown CREATE target: " + what);
    }

    private Query.CreateTable parseCreateTable() {
        String table = expectWord();
        expect("(");
        List<Column> schema = new ArrayList<>();
        schema.add(parseColumnDef());
        while (peek().kind == Token.Kind.COMMA) {
            advance();
            schema.add(parseColumnDef());
        }
        expect(")");
        optionalSemicolon();
        return new Query.CreateTable(table, schema);
    }

    private Column parseColumnDef() {
        String name = expectWord();
        String type = expectWord().toUpperCase();
        DataType dt = DataType.valueOf(type);
        boolean pk = false;
        if (peekWordEquals("PRIMARY")) {
            advance();
            expectWord("KEY");
            pk = true;
        }
        return new Column(name, dt, pk);
    }

    private Query.CreateIndex parseCreateIndex() {
        String idxName = expectWord();
        expectWord("ON");
        String table = expectWord();
        expect("(");
        String column = expectWord();
        expect(")");
        optionalSemicolon();
        return new Query.CreateIndex(idxName, table, column);
    }

    private Query parseDrop() {
        expectWord("TABLE");
        String table = expectWord();
        optionalSemicolon();
        return new Query.DropTable(table);
    }

    private Expression parseExpression() {
        Expression left = parseComparison();
        while (peekWordEquals("AND") || peekWordEquals("OR")) {
            String op = expectWord().toUpperCase();
            Expression right = parseComparison();
            left = op.equals("AND") ? new Expression.And(left, right)
                                    : new Expression.Or(left, right);
        }
        return left;
    }

    private Expression parseComparison() {
        String col = expectWord();
        String op = expect(Token.Kind.OP).text;
        Object val = parseValue();
        return new Expression.Compare(col, op, val);
    }

    private List<Object> parseValueList() {
        expect("(");
        List<Object> values = new ArrayList<>();
        values.add(parseValue());
        while (peek().kind == Token.Kind.COMMA) {
            advance();
            values.add(parseValue());
        }
        expect(")");
        return values;
    }

    private List<String> parseIdentifierList() {
        expect("(");
        List<String> ids = new ArrayList<>();
        ids.add(expectWord());
        while (peek().kind == Token.Kind.COMMA) {
            advance();
            ids.add(expectWord());
        }
        expect(")");
        return ids;
    }

    private Object parseValue() {
        Token t = peek();
        if (t.kind == Token.Kind.STRING) {
            advance();
            String s = t.text;
            return s.substring(1, s.length() - 1);
        }
        if (t.kind == Token.Kind.NUMBER) {
            advance();
            return t.text.contains(".") ? Double.parseDouble(t.text)
                                        : Integer.parseInt(t.text);
        }
        if (t.kind == Token.Kind.WORD) {
            String w = t.text.toUpperCase();
            if (w.equals("TRUE") || w.equals("FALSE")) {
                advance();
                return Boolean.parseBoolean(w);
            }
        }
        throw new IllegalArgumentException("Expected value, got: " + t);
    }

    private Token peek() { return tokens.get(pos); }
    private Token advance() { return tokens.get(pos++); }

    private Token expect(Token.Kind kind) {
        Token t = advance();
        if (t.kind != kind)
            throw new IllegalArgumentException("Expected " + kind + ", got " + t);
        return t;
    }

    private String expectWord() {
        Token t = advance();
        if (t.kind != Token.Kind.WORD)
            throw new IllegalArgumentException("Expected word, got " + t);
        return t.text;
    }

    private void expectWord(String expected) {
        String w = expectWord();
        if (!w.equalsIgnoreCase(expected))
            throw new IllegalArgumentException("Expected " + expected + ", got " + w);
    }

    private void expect(String text) {
        Token t = advance();
        if (!t.text.equals(text))
            throw new IllegalArgumentException("Expected " + text + ", got " + t);
    }

    private boolean peekWordEquals(String w) {
        Token t = peek();
        return t.kind == Token.Kind.WORD && t.text.equalsIgnoreCase(w);
    }

    private void optionalSemicolon() {
        if (peek().kind == Token.Kind.SEMICOLON) advance();
    }
}