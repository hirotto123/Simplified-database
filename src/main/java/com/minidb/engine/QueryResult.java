package com.minidb.engine;

import com.minidb.model.Row;

import java.util.Collections;
import java.util.List;

public class QueryResult {
    public final List<String> columns;
    public final List<Row> rows;
    public final int affected;

    public QueryResult(List<String> columns, List<Row> rows) {
        this.columns = columns;
        this.rows = rows;
        this.affected = rows.size();
    }

    public QueryResult(int affected) {
        this.columns = Collections.<String>emptyList();
        this.rows = Collections.<Row>emptyList();
        this.affected = affected;
    }
}