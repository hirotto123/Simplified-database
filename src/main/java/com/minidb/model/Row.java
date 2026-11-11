package com.minidb.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Row {
    private final Map<String, Object> values = new LinkedHashMap<>();

    public void put(String column, Object value) { values.put(column, value); }
    public Object get(String column) { return values.get(column); }
    public Set<String> columns() { return values.keySet(); }
    public Map<String, Object> asMap() { return values; }
}