package com.minidb.storage;

import com.minidb.model.Table;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class Database {
    private final Map<String, Table> tables = new LinkedHashMap<>();
    private final TableRepository repo;

    public Database(Path dataDir) {
        this.repo = new TableRepository(dataDir);
    }

    public void init() throws IOException {
        for (String name : repo.listTables()) {
            tables.put(name, repo.load(name));
        }
    }

    public Table getTable(String name) {
        Table t = tables.get(name);
        if (t == null) throw new IllegalArgumentException("Table not found: " + name);
        return t;
    }

    public boolean hasTable(String name) { return tables.containsKey(name); }
    public void addTable(Table t) { tables.put(t.getName(), t); }

    public void dropTable(String name) throws IOException {
        tables.remove(name);
        repo.delete(name);
    }

    public Collection<Table> allTables() { return tables.values(); }

    public void persist() throws IOException {
        for (Table t : tables.values()) repo.save(t);
    }
}