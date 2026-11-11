package com.minidb.index;

import com.minidb.model.Row;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Index {
    private final String name;
    private final String table;
    private final String column;
    private final Map<Object, List<Row>> map = new HashMap<>();

    public Index(String name, String table, String column) {
        this.name = name; this.table = table; this.column = column;
    }

    public String getName() { return name; }
    public String getTable() { return table; }
    public String getColumn() { return column; }

    public void add(Row r) {
        Object key = r.get(column);
        List<Row> list = map.get(key);
        if (list == null) {
            list = new ArrayList<>();
            map.put(key, list);
        }
        list.add(r);
    }

    public void remove(Row r) {
        Object key = r.get(column);
        List<Row> list = map.get(key);
        if (list != null) list.remove(r);
    }

    public List<Row> lookup(Object key) {
        List<Row> r = map.get(key);
        return r == null ? Collections.<Row>emptyList() : r;
    }

    public void buildFrom(List<Row> rows) {
        map.clear();
        for (Row r : rows) add(r);
    }

    public void save(Path file) throws IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(tmp))) {
            w.println("# index: " + name + " | table: " + table + " | column: " + column);
            for (Map.Entry<Object, List<Row>> e : map.entrySet()) {
                for (Row r : e.getValue()) {
                    w.println(serializeKey(e.getKey()) + "|" + r.toString());
                }
            }
        }
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    private String serializeKey(Object k) {
        if (k instanceof String) return "'" + k + "'";
        return String.valueOf(k);
    }
}