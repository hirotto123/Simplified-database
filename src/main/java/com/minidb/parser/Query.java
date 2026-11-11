package com.minidb.parser;

import com.minidb.model.Column;

import java.util.List;
import java.util.Map;

public abstract class Query {

    public static class Insert extends Query {
        public final String table;
        public final List<String> columns;
        public final List<Object> values;
        public Insert(String table, List<String> columns, List<Object> values) {
            this.table = table; this.columns = columns; this.values = values;
        }
    }

    public static class Select extends Query {
        public final String table;
        public final List<String> columns;
        public final Expression where;
        public Select(String table, List<String> columns, Expression where) {
            this.table = table; this.columns = columns; this.where = where;
        }
    }

    public static class Update extends Query {
        public final String table;
        public final Map<String, Object> assignments;
        public final Expression where;
        public Update(String table, Map<String, Object> assignments, Expression where) {
            this.table = table; this.assignments = assignments; this.where = where;
        }
    }

    public static class Delete extends Query {
        public final String table;
        public final Expression where;
        public Delete(String table, Expression where) {
            this.table = table; this.where = where;
        }
    }

    public static class CreateTable extends Query {
        public final String table;
        public final List<Column> schema;
        public CreateTable(String table, List<Column> schema) {
            this.table = table; this.schema = schema;
        }
    }

    public static class DropTable extends Query {
        public final String table;
        public DropTable(String table) { this.table = table; }
    }

    public static class CreateIndex extends Query {
        public final String indexName;
        public final String table;
        public final String column;
        public CreateIndex(String indexName, String table, String column) {
            this.indexName = indexName; this.table = table; this.column = column;
        }
    }
}