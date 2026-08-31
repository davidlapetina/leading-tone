package fr.lapetina.music.knowledge.ingestion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A tab-separated table read by column name.
 *
 * <p>By name, never by position: the DCML corpora have twenty-nine columns and publish new
 * versions, and a parser that counts columns breaks the first time somebody inserts one.
 * A column that is absent reads as null rather than throwing, because corpora differ in
 * which optional columns they carry.
 */
public final class TsvTable {

    private final Map<String, Integer> columns = new HashMap<>();
    private final List<String[]> rows = new ArrayList<>();

    public TsvTable(String content) {
        String[] lines = content.split("\r?\n");
        if (lines.length == 0 || lines[0].isBlank()) {
            return;
        }
        String[] header = lines[0].split("\t", -1);
        for (int i = 0; i < header.length; i++) {
            columns.put(header[i].trim(), i);
        }
        for (int i = 1; i < lines.length; i++) {
            if (!lines[i].isBlank()) {
                rows.add(lines[i].split("\t", -1));
            }
        }
    }

    public boolean hasColumn(String name) {
        return columns.containsKey(name);
    }

    public int size() {
        return rows.size();
    }

    public List<Row> rows() {
        return rows.stream().map(Row::new).toList();
    }

    /** One row, addressed by column name. */
    public final class Row {

        private final String[] values;

        private Row(String[] values) {
            this.values = values;
        }

        public String text(String column) {
            Integer at = columns.get(column);
            if (at == null || at >= values.length) {
                return null;
            }
            String value = values[at].trim();
            return value.isEmpty() ? null : value;
        }

        public Integer integer(String column) {
            String value = text(column);
            try {
                return value == null ? null : Integer.valueOf(value);
            } catch (NumberFormatException notANumber) {
                return null;
            }
        }

        public Boolean flag(String column) {
            String value = text(column);
            return value == null ? null : value.equalsIgnoreCase("true") || value.equals("1");
        }

        /**
         * A position, which these corpora write as an exact fraction such as {@code 13/2}.
         * Reading that with {@code Double.parseDouble} silently fails, so it is handled.
         */
        public Double fraction(String column) {
            String value = text(column);
            if (value == null) {
                return null;
            }
            try {
                int slash = value.indexOf('/');
                if (slash < 0) {
                    return Double.valueOf(value);
                }
                double numerator = Double.parseDouble(value.substring(0, slash));
                double denominator = Double.parseDouble(value.substring(slash + 1));
                return denominator == 0 ? null : numerator / denominator;
            } catch (NumberFormatException notANumber) {
                return null;
            }
        }
    }
}
