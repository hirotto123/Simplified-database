package com.minidb.storage;

import com.minidb.model.Column;
import com.minidb.model.DataType;
import com.minidb.model.Row;
import com.minidb.model.Table;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class TableRepository {
    private final Path dir;

    public TableRepository(Path dir) {
        this.dir = dir;
        try { Files.createDirectories(dir); }
        catch (IOException e) { throw new RuntimeException(e); }
    }

    public void save(Table table) throws IOException {
        Path file = dir.resolve(table.getName() + ".tbl");
        Path tmp = dir.resolve(table.getName() + ".tbl.tmp");

        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(tmp))) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < table.getSchema().size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(table.getSchema().get(i).toString());
            }
            w.println("# schema: " + sb.toString());

            for (Row row : table.allRows()) {
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < table.getSchema().size(); i++) {
                    if (i > 0) line.append("|");
                    line.append(stringify(row.get(table.getSchema().get(i).getName())));
                }
                w.println(line.toString());
            }
        }
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    public Table load(String name) throws IOException {
        Path file = dir.resolve(name + ".tbl");
        List<String> lines = Files.readAllLines(file);
        if (lines.isEmpty()) throw new IOException("Empty file: " + file);

        String schemaLine = lines.get(0);
        if (!schemaLine.startsWith("# schema:"))
            throw new IOException("Bad schema: " + schemaLine);

        List<Column> schema = parseSchema(schemaLine.substring("# schema:".length()).trim());
        Table table = new Table(name, schema);

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\|", -1);
            Row row = new Row();
            for (int j = 0; j < schema.size(); j++) {
                Column c = schema.get(j);
                row.put(c.getName(), c.getType().parse(parts[j]));
            }
            table.insert(row);
        }
        return table;
    }

    public List<String> listTables() throws IOException {
        List<String> names = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.tbl")) {
            for (Path p : stream) {
                String fn = p.getFileName().toString();
                names.add(fn.substring(0, fn.length() - 4));
            }
        }
        return names;
    }

    public void delete(String name) throws IOException {
        Files.deleteIfExists(dir.resolve(name + ".tbl"));
    }

    private List<Column> parseSchema(String s) {
        List<Column> cols = new ArrayList<>();
        for (String part : s.split(",")) {
            String[] ps = part.trim().split(":");
            String colName = ps[0].trim();
            DataType type = DataType.valueOf(ps[1].trim().toUpperCase());
            boolean pk = ps.length > 2 && ps[2].trim().equalsIgnoreCase("pk");
            cols.add(new Column(colName, type, pk));
        }
        return cols;
    }

    private String stringify(Object v) {
        if (v == null) return "";
        if (v instanceof String) return "'" + v + "'";
        return v.toString();
    }
}