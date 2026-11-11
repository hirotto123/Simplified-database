package com.minidb.engine;

import com.minidb.index.Index;
import com.minidb.index.IndexManager;
import com.minidb.model.Column;
import com.minidb.model.Row;
import com.minidb.model.Table;
import com.minidb.parser.Expression;
import com.minidb.parser.Query;
import com.minidb.storage.Database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QueryExecutor {
    private final Database db;
    private final IndexManager indexes;

    public QueryExecutor(Database db, IndexManager indexes) {
        this.db = db;
        this.indexes = indexes;
    }

    public QueryResult execute(Query q) throws IOException {
        if (q instanceof Query.Insert)      return execInsert((Query.Insert) q);
        if (q instanceof Query.Select)      return execSelect((Query.Select) q);
        if (q instanceof Query.Update)      return execUpdate((Query.Update) q);
        if (q instanceof Query.Delete)      return execDelete((Query.Delete) q);
        if (q instanceof Query.CreateTable) return execCreateTable((Query.CreateTable) q);
        if (q instanceof Query.DropTable)   return execDropTable((Query.DropTable) q);
        if (q instanceof Query.CreateIndex) return execCreateIndex((Query.CreateIndex) q);
        throw new UnsupportedOperationException("Unknown query");
    }

    private QueryResult execInsert(Query.Insert q) throws IOException {
        Table t = db.getTable(q.table);
        Row row = new Row();
        List<Column> schema = t.getSchema();

        if (q.columns == null) {
            if (q.values.size() != schema.size())
                throw new IllegalArgumentException("Values count != schema size");
            for (int i = 0; i < schema.size(); i++) {
                row.put(schema.get(i).getName(),
                        schema.get(i).getType().parse(stringify(q.values.get(i))));
            }
        } else {
            for (int i = 0; i < q.columns.size(); i++) {
                String col = q.columns.get(i);
                Column c = null;
                for (Column x : schema) if (x.getName().equals(col)) { c = x; break; }
                if (c == null) throw new IllegalArgumentException("Column not found: " + col);
                row.put(col, c.getType().parse(stringify(q.values.get(i))));
            }
        }
        t.insert(row);
        indexes.onInsert(t, row);
        db.persist();
        return new QueryResult(1);
    }

    private String stringify(Object v) {
        if (v == null) return "";
        if (v instanceof String) return "'" + v + "'";
        if (v instanceof Number) {
            Number n = (Number) v;
            double d = n.doubleValue();
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return String.valueOf((long) d);
            }
            return String.valueOf(d);
        }
        if (v instanceof Boolean) return v.toString();
        return String.valueOf(v);
    }

    private QueryResult execSelect(Query.Select q) {
        Table t = db.getTable(q.table);
        List<Row> rows;

        Index idx = tryUseIndex(q.table, q.where);
        if (idx != null) {
            rows = idx.lookup(((Expression.Compare) q.where).value);
        } else {
            rows = new ArrayList<>();
            for (Row r : t.allRows()) {
                if (q.where == null || q.where.eval(r)) rows.add(r);
            }
        }

        List<String> outCols = q.columns;
        if (outCols.size() == 1 && outCols.get(0).equals("*")) {
            outCols = new ArrayList<>();
            for (Column c : t.getSchema()) outCols.add(c.getName());
        }

        List<Row> projected = new ArrayList<>();
        for (Row r : rows) {
            Row p = new Row();
            for (String c : outCols) p.put(c, r.get(c));
            projected.add(p);
        }
        return new QueryResult(outCols, projected);
    }

    private Index tryUseIndex(String table, Expression where) {
        if (!(where instanceof Expression.Compare)) return null;
        Expression.Compare cmp = (Expression.Compare) where;
        if (!cmp.op.equals("=")) return null;
        return indexes.get(table, cmp.column);
    }

    private QueryResult execUpdate(Query.Update q) throws IOException {
        Table t = db.getTable(q.table);
        int affected = 0;
        List<Row> snapshot = new ArrayList<>(t.allRows());
        for (Row r : snapshot) {
            if (q.where != null && !q.where.eval(r)) continue;
            Row oldCopy = copyRow(r);
            for (Map.Entry<String, Object> e : q.assignments.entrySet()) {
                r.put(e.getKey(), parseForColumn(t, e.getKey(), e.getValue()));
            }
            indexes.onUpdate(t, oldCopy, r);
            affected++;
        }
        db.persist();
        return new QueryResult(affected);
    }

    private QueryResult execDelete(Query.Delete q) throws IOException {
        Table t = db.getTable(q.table);
        int affected = 0;
        List<Row> snapshot = new ArrayList<>(t.allRows());
        for (Row r : snapshot) {
            if (q.where == null || q.where.eval(r)) {
                indexes.onDelete(t, r);
                t.deleteByPk(r.get(t.getPrimaryKey()));
                affected++;
            }
        }
        db.persist();
        return new QueryResult(affected);
    }

    private QueryResult execCreateTable(Query.CreateTable q) throws IOException {
        if (db.hasTable(q.table))
            throw new IllegalArgumentException("Table exists: " + q.table);
        db.addTable(new Table(q.table, q.schema));
        db.persist();
        return new QueryResult(0);
    }

    private QueryResult execDropTable(Query.DropTable q) throws IOException {
        db.dropTable(q.table);
        return new QueryResult(0);
    }

    private QueryResult execCreateIndex(Query.CreateIndex q) throws IOException {
        Table t = db.getTable(q.table);
        indexes.create(q.indexName, t, q.column);
        indexes.saveAll();
        return new QueryResult(0);
    }

    private Object parseForColumn(Table t, String col, Object value) {
        Column c = null;
        for (Column x : t.getSchema()) if (x.getName().equals(col)) { c = x; break; }
        if (c == null) throw new IllegalArgumentException("Column not found: " + col);
        return c.getType().parse(stringify(value));
    }

    private Row copyRow(Row r) {
        Row copy = new Row();
        for (Map.Entry<String, Object> e : r.asMap().entrySet()) copy.put(e.getKey(), e.getValue());
        return copy;
    }
}