package AirQualityController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Reads the Open-Meteo response and history file using only the JDK. */
public final class AirQualityJson {
    private static final Pattern NUMBER = Pattern.compile("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?");
    private static final int JSON_CONTROL_LIMIT = 0x20;
    private static final int UNICODE_ESCAPE_LENGTH = 4;

    private final String text;
    private int cursor;

    private AirQualityJson(String text) {
        this.text = text;
        cursor = text.startsWith("\uFEFF") ? 1 : 0;
    }

    /** Parses a JSON value, including nested objects and arrays. */
    public static Object parse(String text) {
        AirQualityJson parser = new AirQualityJson(text);
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (parser.cursor != text.length()) throw parser.invalidJson();
        return value;
    }

    /** Requires a JSON object value. */
    public static Map<?, ?> object(Object value) {
        if (value instanceof Map<?, ?> map) return map;
        throw new IllegalArgumentException("Expected a JSON object.");
    }

    /** Requires a JSON array value. */
    public static List<?> array(Object value) {
        if (value instanceof List<?> list) return list;
        throw new IllegalArgumentException("Expected a JSON array.");
    }

    /** Requires a JSON string value. */
    public static String string(Object value) {
        if (value instanceof String s) return s;
        throw new IllegalArgumentException("Expected a JSON string.");
    }

    /** Reads a number, also accepting strings from legacy local JSON files. */
    public static double number(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        // Accept numeric strings from the original local-data prototype.
        return Double.parseDouble(string(value));
    }

    private Object readValue() {
        skipWhitespace();
        if (cursor == text.length()) throw invalidJson();
        return switch (text.charAt(cursor)) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> readLiteral("true", true);
            case 'f' -> readLiteral("false", false);
            case 'n' -> readLiteral("null", null);
            default -> readNumber();
        };
    }

    private Map<String, Object> readObject() {
        expect('{');
        Map<String, Object> result = new LinkedHashMap<>();
        if (consume('}')) return result;
        do {
            String key = readString();
            expect(':');
            result.put(key, readValue());
        } while (consume(','));
        expect('}');
        return result;
    }

    private List<Object> readArray() {
        expect('[');
        List<Object> result = new ArrayList<>();
        if (consume(']')) return result;
        do {
            result.add(readValue());
        } while (consume(','));
        expect(']');
        return result;
    }

    private Object readLiteral(String token, Object value) {
        if (!text.startsWith(token, cursor)) throw invalidJson();
        cursor += token.length();
        return value;
    }

    private BigDecimal readNumber() {
        var match = NUMBER.matcher(text).region(cursor, text.length());
        if (!match.lookingAt()) throw invalidJson();
        cursor = match.end();
        return new BigDecimal(match.group());
    }

    private String readString() {
        expect('"');
        StringBuilder value = new StringBuilder();
        while (cursor < text.length()) {
            char next = text.charAt(cursor++);
            if (next == '"') return value.toString();
            if (next < JSON_CONTROL_LIMIT) throw invalidJson();
            value.append(next == '\\' ? readEscape() : next);
        }
        throw invalidJson();
    }

    private char readEscape() {
        if (cursor == text.length()) throw invalidJson();
        return switch (text.charAt(cursor++)) {
            case '"' -> '"';
            case '\\' -> '\\';
            case '/' -> '/';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'u' -> readUnicodeEscape();
            default -> throw invalidJson();
        };
    }

    private char readUnicodeEscape() {
        int end = cursor + UNICODE_ESCAPE_LENGTH;
        if (end > text.length()) throw invalidJson();
        int value = HexFormat.fromHexDigits(text, cursor, end);
        cursor = end;
        return (char) value;
    }

    private void skipWhitespace() {
        while (cursor < text.length() && " \t\r\n".indexOf(text.charAt(cursor)) >= 0) {
            cursor++;
        }
    }

    private void expect(char token) {
        if (!consume(token)) throw invalidJson();
    }

    private boolean consume(char token) {
        skipWhitespace();
        if (cursor == text.length() || text.charAt(cursor) != token) return false;
        cursor++;
        return true;
    }

    private IllegalArgumentException invalidJson() {
        return new IllegalArgumentException("Invalid JSON at offset " + cursor + ".");
    }

    /** Escapes a string for storage in local JSON files. */
    public static String quote(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (char ch : value.toCharArray()) {
            switch (ch) {
                case '\\' -> result.append("\\\\");
                case '"' -> result.append("\\\"");
                default -> {
                    if (ch < JSON_CONTROL_LIMIT) {
                        result.append("\\u%04x".formatted((int) ch));
                    } else {
                        result.append(ch);
                    }
                }
            }
        }
        return result.append('"').toString();
    }
}
