package com.minidb.index;

import com.minidb.model.Row;
import com.minidb.model.Table;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IndexManager {
    private final Map<String, Index> indexes = new LinkedHashMap<>();
    private final Path dir;

    public IndexManager(Path dir) {
        this.dir = dir;
        try { Files.createDirectories(dir); }
        catch (IOException e) { throw new RuntimeException(e); }
    }

    public Index create(String name, Table table, String column) {
        Index idx = new Index(name, table.getName(), column);
        idx.buildFrom(table.allRows());
        indexes.put(key(table.getName(), column), idx);
        return idx;
    }

    public Index get(String table, String column) {
        return indexes.get(key(table, column));
    }

    public void onInsert(Table t, Row r) {
        for (Index idx : indexesFor(t.getName())) idx.add(r);
    }

    public void onDelete(Table t, Row r) {
        for (Index idx : indexesFor(t.getName())) idx.remove(r);
    }

    public void onUpdate(Table t, Row oldRow, Row newRow) {
        for (Index idx : indexesFor(t.getName())) {
            idx.remove(oldRow);
            idx.add(newRow);
        }
    }

    private List<Index> indexesFor(String table) {
        List<Index> res = new ArrayList<>();
        for (Index idx : indexes.values())
            if (idx.getTable().equals(table)) res.add(idx);
        return res;
    }

    private String key(String table, String column) { return table + "." + column; }

    public void saveAll() throws IOException {
        for (Index idx : indexes.values()) {
            idx.save(dir.resolve(idx.getName() + ".idx"));
        }
    }
}