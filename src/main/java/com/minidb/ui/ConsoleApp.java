package com.minidb.ui;

import com.minidb.engine.QueryExecutor;
import com.minidb.engine.QueryResult;
import com.minidb.index.IndexManager;
import com.minidb.model.Row;
import com.minidb.parser.Lexer;
import com.minidb.parser.Query;
import com.minidb.parser.SqlParser;
import com.minidb.storage.Database;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;
import java.io.PrintStream;

public class ConsoleApp {

    public static void main(String[] args) throws Exception {
        Path dataDir = Paths.get("data");
        Database db = new Database(dataDir);
        db.init();
        IndexManager indexes = new IndexManager(dataDir);
        QueryExecutor exec = new QueryExecutor(db, indexes);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try { db.persist(); indexes.saveAll(); }
                catch (Exception e) { e.printStackTrace(); }
            }
        });

        System.out.println("MiniDB ready. Type SQL or 'exit'.");
        Scanner sc = new Scanner(System.in);
        Lexer lexer = new Lexer();

        while (true) {
            System.out.print("sql> ");
            if (!sc.hasNextLine()) break;
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;
            if (line.equalsIgnoreCase("exit")) break;

            try {
                Query q = new SqlParser(lexer.tokenize(line)).parse();
                QueryResult r = exec.execute(q);
                printResult(r);
            } catch (Exception e) {
                System.err.println("ERROR: " + e.getMessage());
            }
        }
        db.persist();
        indexes.saveAll();
        sc.close();
    }

    private static void printResult(QueryResult r) {
        if (r.rows.isEmpty() && r.columns.isEmpty()) {
            System.out.println("OK, affected: " + r.affected);
            return;
        }
        int[] widths = new int[r.columns.size()];
        for (int i = 0; i < r.columns.size(); i++) widths[i] = r.columns.get(i).length();
        for (Row row : r.rows) {
            for (int i = 0; i < r.columns.size(); i++) {
                Object v = row.get(r.columns.get(i));
                int len = v == null ? 4 : String.valueOf(v).length();
                if (len > widths[i]) widths[i] = len;
            }
        }

        StringBuilder header = new StringBuilder();
        StringBuilder sep = new StringBuilder();
        for (int i = 0; i < r.columns.size(); i++) {
            header.append(padRight(r.columns.get(i), widths[i])).append(" | ");
            sep.append(repeatChar('-', widths[i])).append("-+-");
        }
        System.out.println(header);
        System.out.println(sep);
        for (Row row : r.rows) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < r.columns.size(); i++) {
                Object v = row.get(r.columns.get(i));
                sb.append(padRight(v == null ? "NULL" : String.valueOf(v), widths[i])).append(" | ");
            }
            System.out.println(sb);
        }
        System.out.println("(" + r.rows.size() + " rows)");
    }

    private static String padRight(String s, int width) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }

    private static String repeatChar(char c, int count) {
        char[] arr = new char[count];
        Arrays.fill(arr, c);
        return new String(arr);
    }
}