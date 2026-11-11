package com.minidb.parser;

import com.minidb.model.Row;

public interface Expression {
    boolean eval(Row row);

    class Compare implements Expression {
        public final String column;
        public final String op;
        public final Object value;

        public Compare(String column, String op, Object value) {
            this.column = column;
            this.op = op;
            this.value = value;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public boolean eval(Row row) {
            Object v = row.get(column);
            if (v == null) return false;
            if (v instanceof Number && value instanceof Number) {
                double a = ((Number) v).doubleValue();
                double b = ((Number) value).doubleValue();
                return compareInt(Double.compare(a, b));
            }
            if (v instanceof Comparable && value instanceof Comparable) {
                int c = ((Comparable) v).compareTo(value);
                return compareInt(c);
            }
            return false;
        }

        private boolean compareInt(int c) {
            switch (op) {
                case "=":  return c == 0;
                case "!=": case "<>": return c != 0;
                case ">":  return c > 0;
                case "<":  return c < 0;
                case ">=": return c >= 0;
                case "<=": return c <= 0;
            }
            return false;
        }
    }

    class And implements Expression {
        public final Expression left, right;
        public And(Expression left, Expression right) { this.left = left; this.right = right; }
        @Override public boolean eval(Row row) { return left.eval(row) && right.eval(row); }
    }

    class Or implements Expression {
        public final Expression left, right;
        public Or(Expression left, Expression right) { this.left = left; this.right = right; }
        @Override public boolean eval(Row row) { return left.eval(row) || right.eval(row); }
    }
}