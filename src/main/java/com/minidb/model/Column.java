package com.minidb.model;

public class Column {
    private final String name;
    private final DataType type;
    private final boolean primaryKey;

    public Column(String name, DataType type, boolean primaryKey) {
        this.name = name;
        this.type = type;
        this.primaryKey = primaryKey;
    }

    public String getName() { return name; }
    public DataType getType() { return type; }
    public boolean isPrimaryKey() { return primaryKey; }

    @Override
    public String toString() {
        return name + ":" + type + (primaryKey ? ":pk" : "");
    }
}