package com.minidb.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Table {
    private final String name;
    private final List<Column> schema;
    private final Map<Object, Row> rows = new LinkedHashMap<>();
    private final String primaryKey;

    public Table(String name, List<Column> schema) {
        this.name = name;
        this.schema = new ArrayList<>(schema);
        String pk = null;
        for (Column c : schema) {
            if (c.isPrimaryKey()) pk = c.getName();
        }
        this.primaryKey = pk;
    }

    public String getName() { return name; }
    public List<Column> getSchema() { return Collections.unmodifiableList(schema); }
    public String getPrimaryKey() { return primaryKey; }

    public void insert(Row row) {
        Object pk = row.get(primaryKey);
        if (rows.containsKey(pk)) {
            throw new IllegalArgumentException("Duplicate primary key: " + pk);
        }
        rows.put(pk, row);
    }

    public List<Row> allRows() { return new ArrayList<>(rows.values()); }
    public Row findByPk(Object pk) { return rows.get(pk); }
    public boolean deleteByPk(Object pk) { return rows.remove(pk) != null; }
    public int size() { return rows.size(); }
}