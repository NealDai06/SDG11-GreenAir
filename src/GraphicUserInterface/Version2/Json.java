package greenair.data;

import java.util.*;

/** Minimal JSON reader/writer for a flat array of objects whose values are stored as strings. */
final class Json {
    private final String s;
    private int i;

    private Json(String s) { this.s = s; }

    static List<Map<String, String>> parse(String text) {
        Json p = new Json(text.startsWith("\uFEFF") ? text.substring(1) : text);
        p.ws();
        if (p.i >= p.s.length()) return new ArrayList<>();
        p.expect('[');
        List<Map<String, String>> rows = new ArrayList<>();
        p.ws();
        if (p.peek() == ']') return rows;
        while (true) {
            rows.add(p.object());
            p.ws();
            if (p.peek() == ',') { p.i++; continue; }
            p.expect(']');
            return rows;
        }
    }

    private Map<String, String> object() {
        Map<String, String> m = new LinkedHashMap<>();
        ws();
        expect('{');
        ws();
        if (peek() == '}') { i++; return m; }
        while (true) {
            ws();
            String key = string();
            ws();
            expect(':');
            ws();
            m.put(key, value());
            ws();
            if (peek() == ',') { i++; continue; }
            expect('}');
            return m;
        }
    }

    private String value() {
        if (peek() == '"') return string();
        int start = i;
        while (i < s.length() && ",}] \n\r\t".indexOf(s.charAt(i)) < 0) i++;
        String raw = s.substring(start, i);
        return raw.equals("null") ? "" : raw;
    }

    private String string() {
        expect('"');
        StringBuilder b = new StringBuilder();
        while (true) {
            if (i >= s.length()) throw error("Unterminated string");
            char c = s.charAt(i++);
            if (c == '"') return b.toString();
            if (c != '\\') { b.append(c); continue; }
            char e = s.charAt(i++);
            switch (e) {
                case 'n' -> b.append('\n');
                case 't' -> b.append('\t');
                case 'r' -> b.append('\r');
                case 'b' -> b.append('\b');
                case 'f' -> b.append('\f');
                case 'u' -> { b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                default -> b.append(e);
            }
        }
    }

    private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
    private char peek() { return i < s.length() ? s.charAt(i) : '\0'; }
    private void expect(char c) {
        if (peek() != c) throw error("Expected '" + c + "'");
        i++;
    }
    private IllegalArgumentException error(String msg) {
        return new IllegalArgumentException(msg + " at position " + i);
    }

    static String write(List<Map<String, String>> rows) {
        StringBuilder b = new StringBuilder("[");
        for (int r = 0; r < rows.size(); r++) {
            b.append(r == 0 ? "\n  {" : ",\n  {");
            int n = 0;
            for (Map.Entry<String, String> e : rows.get(r).entrySet()) {
                b.append(n++ == 0 ? "\n    " : ",\n    ");
                b.append(quote(e.getKey())).append(": ").append(quote(e.getValue() == null ? "" : e.getValue()));
            }
            b.append("\n  }");
        }
        return b.append(rows.isEmpty() ? "]" : "\n]").append('\n').toString();
    }

    private static String quote(String v) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : v.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> { if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c); }
            }
        }
        return b.append('"').toString();
    }
}
