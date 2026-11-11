package com.minidb.model;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public enum DataType {
    INT, FLOAT, STRING, DATE, BOOLEAN;

    public static DataType detect(String raw) {
        if (raw == null) return STRING;
        String s = raw.trim();
        if (s.equalsIgnoreCase("TRUE") || s.equalsIgnoreCase("FALSE")) return BOOLEAN;
        if (s.matches("-?\\d+")) return INT;
        if (s.matches("-?\\d+\\.\\d+")) return FLOAT;
        try {
            LocalDate.parse(s);
            return DATE;
        } catch (DateTimeParseException ignored) { }
        return STRING;
    }

    public Object parse(String raw) {
        String s = raw.trim();
        while (s.startsWith("'") && s.endsWith("'") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        switch (this) {
            case INT:     return Integer.parseInt(s);
            case FLOAT:   return Double.parseDouble(s);
            case BOOLEAN: return Boolean.parseBoolean(s);
            case DATE:    return LocalDate.parse(s);
            case STRING:  return s;
            default:      return s;
    }
}
}