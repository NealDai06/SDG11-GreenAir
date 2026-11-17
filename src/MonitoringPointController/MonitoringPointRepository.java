package MonitoringPointController;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Repository layer of the monitoring point feature: reads and writes
 * {@code monitoring_points.json}.
 *
 * <p>The file holds a flat JSON array, one object per point:</p>
 *
 * <pre>
 * [
 *   {
 *     "id": "MP001",
 *     "name": "Kuala Lumpur Station",
 *     "district": "Kuala Lumpur",
 *     "latitude": "3.1390",
 *     "longitude": "101.6869",
 *     "active": "true"
 *   }
 * ]
 * </pre>
 *
 * <p>The service layer is the only caller, so no rule of the application lives here: this class
 * only maps rows to {@link MonitoringPoint}, assigns the next {@code MP} id and writes the file
 * back through a temporary file, so a crash cannot truncate the real one. Reading tolerates a
 * missing field, a non-numeric coordinate or the {@code "active": "Active"} spelling.</p>
 */
public class MonitoringPointRepository {

    private static final String ID_PREFIX = "MP";

    /** Prefix that marks a token as a JSON string, so it cannot be confused with a punctuation token. */
    private static final char STRING_MARK = '\u0001';

    private final Path file;

    public MonitoringPointRepository(Path file) {
        this.file = file;
    }

    /** Repository over the {@code data/monitoring_points.json} file of the running application. */
    public MonitoringPointRepository() {
        this(Path.of("data", "monitoring_points.json"));
    }

    public Path file() {
        return file;
    }

    public List<MonitoringPoint> findAll() {
        List<MonitoringPoint> points = new ArrayList<>();
        for (Map<String, String> row : readRows()) {
            points.add(toPoint(row));
        }
        return points;
    }

    public Optional<MonitoringPoint> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return findAll().stream().filter(p -> id.trim().equals(p.id())).findFirst();
    }

    /** Case-insensitive lookup used by the duplicate-name check. */
    public Optional<MonitoringPoint> findByName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        String wanted = name.trim();
        return findAll().stream().filter(p -> p.name().equalsIgnoreCase(wanted)).findFirst();
    }

    /** Sorted district names of the active points, the list the registration screen offers. */
    public List<String> findActiveDistricts() {
        TreeSet<String> districts = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (MonitoringPoint point : findAll()) {
            if (point.active() && !point.district().isBlank()) districts.add(point.district());
        }
        return new ArrayList<>(districts);
    }

    /**
     * Average coordinates of a district, used as its centre by the park recommendation search.
     *
     * @return {@code {latitude, longitude}}, or empty when the district has no point at all
     */
    public Optional<double[]> findDistrictCentre(String district) {
        if (district == null || district.isBlank()) return Optional.empty();
        String wanted = district.trim();
        double sumLatitude = 0;
        double sumLongitude = 0;
        int count = 0;
        for (MonitoringPoint point : findAll()) {
            if (!point.district().equalsIgnoreCase(wanted)) continue;
            sumLatitude += point.latitude();
            sumLongitude += point.longitude();
            count++;
        }
        if (count == 0) return Optional.empty();
        return Optional.of(new double[]{sumLatitude / count, sumLongitude / count});
    }

    /**
     * Inserts a point whose id is blank, otherwise replaces the point with that id.
     *
     * @return the stored point, with the id that was assigned to a new one
     */
    public MonitoringPoint save(MonitoringPoint point) {
        List<Map<String, String>> rows = readRows();
        String id = point.id();
        MonitoringPoint stored = point;

        if (id.isEmpty()) {
            id = nextId(rows);
            stored = new MonitoringPoint(id, point.name(), point.district(),
                    point.latitude(), point.longitude(), point.active());
        }

        Map<String, String> row = toRow(stored);
        int existing = indexOf(rows, id);
        if (existing >= 0) {
            rows.set(existing, row);
        } else {
            rows.add(row);
        }
        writeRows(rows);
        return stored;
    }

    /** @return {@code true} when a point was removed, {@code false} when the id was unknown */
    public boolean deleteById(String id) {
        if (id == null || id.isBlank()) return false;
        List<Map<String, String>> rows = readRows();
        int existing = indexOf(rows, id.trim());
        if (existing < 0) return false;
        rows.remove(existing);
        writeRows(rows);
        return true;
    }

    // ---------------------------------------------------------------- monitoring_points.json <-> MonitoringPoint

    private static int indexOf(List<Map<String, String>> rows, String id) {
        for (int i = 0; i < rows.size(); i++) {
            if (id.equals(rows.get(i).getOrDefault("id", ""))) return i;
        }
        return -1;
    }

    private static String nextId(List<Map<String, String>> rows) {
        int highest = 0;
        for (Map<String, String> row : rows) {
            String id = row.getOrDefault("id", "");
            if (!id.startsWith(ID_PREFIX)) continue;
            try {
                highest = Math.max(highest, Integer.parseInt(id.substring(ID_PREFIX.length())));
            } catch (NumberFormatException ignored) {
                // a hand-edited id that is not a number simply does not take part
            }
        }
        return ID_PREFIX + String.format("%03d", highest + 1);
    }

    private static Map<String, String> toRow(MonitoringPoint point) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("id", point.id());
        row.put("name", point.name());
        row.put("district", point.district());
        row.put("latitude", coordinate(point.latitude()));
        row.put("longitude", coordinate(point.longitude()));
        row.put("active", String.valueOf(point.active()));
        return row;
    }

    private static MonitoringPoint toPoint(Map<String, String> row) {
        return new MonitoringPoint(
                row.getOrDefault("id", ""),
                row.getOrDefault("name", ""),
                row.getOrDefault("district", ""),
                number(row.get("latitude")),
                number(row.get("longitude")),
                isActive(row));
    }

    /** {@code 3.139} instead of {@code 3.13900000}: precise, and easy to read in the file. */
    private static String coordinate(double value) {
        String text = String.format(Locale.ROOT, "%.6f", value);
        text = text.replaceAll("0+$", "");
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }

    private static double number(String stored) {
        if (stored == null || stored.isBlank()) return 0;
        try {
            return Double.parseDouble(stored.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static boolean isActive(Map<String, String> row) {
        String stored = row.get("active");
        if (stored == null) return true;
        String value = stored.trim();
        return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("Active") || value.equals("1");
    }

    // ---------------------------------------------------------------- file access

    private List<Map<String, String>> readRows() {
        if (!Files.exists(file)) return new ArrayList<>();
        try {
            return parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot read " + file.getFileName() + ": " + ex.getMessage(), ex);
        }
    }

    private void writeRows(List<Map<String, String>> rows) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, toJson(rows), StandardCharsets.UTF_8);
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot write " + file.getFileName() + ": " + ex.getMessage(), ex);
        }
    }

    // ---------------------------------------------------------------- minimal JSON for flat records
    // The same small reader/writer is used by the other repositories of the project; a shared
    // Json utility class can replace both copies later without touching the service layer.

    private static String toJson(List<Map<String, String>> rows) {
        StringBuilder out = new StringBuilder("[\n");
        for (int r = 0; r < rows.size(); r++) {
            Map<String, String> row = rows.get(r);
            out.append("  {\n");
            int written = 0;
            for (Map.Entry<String, String> entry : row.entrySet()) {
                out.append("    \"").append(escape(entry.getKey()))
                        .append("\": \"").append(escape(entry.getValue())).append('"');
                out.append(++written < row.size() ? ",\n" : "\n");
            }
            out.append("  }").append(r + 1 < rows.size() ? ",\n" : "\n");
        }
        return out.append("]\n").toString();
    }

    private static String escape(String value) {
        String text = value == null ? "" : value;
        StringBuilder out = new StringBuilder(text.length() + 8);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    /** Parses the flat array of string records that this repository writes. */
    private static List<Map<String, String>> parse(String json) {
        List<String> tokens = tokenize(json);
        List<Map<String, String>> rows = new ArrayList<>();
        int i = 0;
        while (i < tokens.size()) {
            if (!"{".equals(tokens.get(i))) {
                i++;
                continue;
            }
            i++;
            Map<String, String> row = new LinkedHashMap<>();
            while (i < tokens.size() && tokens.get(i).charAt(0) == STRING_MARK) {
                String key = tokens.get(i++).substring(1);
                if (i >= tokens.size() || !":".equals(tokens.get(i))) break;
                i++;
                if (i >= tokens.size()) break;
                String token = tokens.get(i++);
                String value = token.charAt(0) == STRING_MARK ? token.substring(1) : token;
                row.put(key, value);
                if (i < tokens.size() && ",".equals(tokens.get(i))) i++;
            }
            rows.add(row);
        }
        return rows;
    }

    private static List<String> tokenize(String json) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        int length = json.length();
        while (i < length) {
            char c = json.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '"') {
                StringBuilder text = new StringBuilder();
                i++;
                while (i < length && json.charAt(i) != '"') {
                    char ch = json.charAt(i);
                    if (ch == '\\' && i + 1 < length) {
                        char escaped = json.charAt(++i);
                        switch (escaped) {
                            case 'n' -> text.append('\n');
                            case 't' -> text.append('\t');
                            case 'r' -> text.append('\r');
                            case 'b' -> text.append('\b');
                            case 'f' -> text.append('\f');
                            case 'u' -> {
                                if (i + 4 < length) {
                                    text.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                                    i += 4;
                                }
                            }
                            default -> text.append(escaped);
                        }
                    } else {
                        text.append(ch);
                    }
                    i++;
                }
                i++; // closing quote
                tokens.add(STRING_MARK + text.toString());
                continue;
            }
            if (c == '{' || c == '}' || c == '[' || c == ']' || c == ':' || c == ',') {
                tokens.add(String.valueOf(c));
                i++;
                continue;
            }
            int start = i;
            while (i < length && ",}] \t\r\n".indexOf(json.charAt(i)) < 0) i++;
            tokens.add(json.substring(start, i));
        }
        return tokens;
    }
}
