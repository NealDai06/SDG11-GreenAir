package UserController;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository layer of the user account feature: reads and writes {@code users.json}.
 *
 * <p>The file holds a flat JSON array, one object per account:</p>
 *
 * <pre>
 * [
 *   {
 *     "id": "U001",
 *     "username": "admin",
 *     "fullName": "System Administrator",
 *     "role": "ADMINISTRATOR",
 *     "groupType": "General public",
 *     "status": "Active",
 *     "districts": "Petaling Jaya;Shah Alam"
 *   }
 * ]
 * </pre>
 *
 * <p>The service layer is the only caller: it asks for profiles, changes them and stores them
 * again, so no rule of the application lives here. Reading is tolerant - a missing field, an old
 * role spelling or the older {@code "active": "true"} key still produce a usable profile - and a
 * write always goes through a temporary file so a crash cannot truncate the real one.</p>
 */
public class ProfileRepository {

    private static final String ID_PREFIX = "U";
    private static final String ACTIVE = "Active";
    private static final String INACTIVE = "Inactive";

    /** Prefix that marks a token as a JSON string, so it cannot be confused with a punctuation token. */
    private static final char STRING_MARK = '\u0001';

    private final Path file;

    public ProfileRepository(Path file) {
        this.file = file;
    }

    /** Repository over the {@code data/users.json} file of the running application. */
    public ProfileRepository() {
        this(Path.of("data", "users.json"));
    }

    public Path file() {
        return file;
    }

    public List<UserProfile> findAll() {
        List<UserProfile> profiles = new ArrayList<>();
        for (Map<String, String> row : readRows()) {
            profiles.add(toProfile(row));
        }
        return profiles;
    }

    public Optional<UserProfile> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return findAll().stream().filter(p -> id.trim().equals(p.id())).findFirst();
    }

    /** Case-insensitive lookup used by the log-in screen. */
    public Optional<UserProfile> findByUsername(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        String wanted = username.trim();
        return findAll().stream().filter(p -> p.username().equalsIgnoreCase(wanted)).findFirst();
    }

    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    /**
     * Inserts a profile whose id is blank, otherwise replaces the profile with that id.
     *
     * @return the stored profile, with the id that was assigned to a new account
     */
    public UserProfile save(UserProfile profile) {
        List<Map<String, String>> rows = readRows();
        String id = profile.id() == null ? "" : profile.id().trim();
        UserProfile stored = profile;

        if (id.isEmpty()) {
            id = nextId(rows);
            stored = new UserProfile(id, profile.username(), profile.fullName(), profile.role(),
                    profile.groupType(), profile.active(), profile.districts());
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

    /** @return {@code true} when an account was removed, {@code false} when the id was unknown */
    public boolean deleteById(String id) {
        if (id == null || id.isBlank()) return false;
        List<Map<String, String>> rows = readRows();
        int existing = indexOf(rows, id.trim());
        if (existing < 0) return false;
        rows.remove(existing);
        writeRows(rows);
        return true;
    }

    // ---------------------------------------------------------------- users.json <-> UserProfile

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

    private static Map<String, String> toRow(UserProfile profile) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("id", profile.id());
        row.put("username", profile.username());
        row.put("fullName", profile.fullName());
        row.put("role", profile.role().storageValue());
        row.put("groupType", profile.groupType().label());
        row.put("status", profile.active() ? ACTIVE : INACTIVE);
        row.put("districts", String.join(";", profile.districts()));
        return row;
    }

    private static UserProfile toProfile(Map<String, String> row) {
        return new UserProfile(
                row.getOrDefault("id", ""),
                row.getOrDefault("username", ""),
                row.getOrDefault("fullName", ""),
                UserRole.fromStorage(row.get("role")),
                groupTypeOf(row),
                isActive(row),
                districtsOf(row.getOrDefault("districts", "")));
    }

    /** Reads the group from the label, falling back to the general public on an unknown value. */
    private static GroupType groupTypeOf(Map<String, String> row) {
        String stored = row.get("groupType");
        try {
            return GroupType.fromLabel(stored);
        } catch (IllegalArgumentException ex) {
            return GroupType.GENERAL_PUBLIC;
        }
    }

    private static boolean isActive(Map<String, String> row) {
        String status = row.get("status");
        if (status != null) return !INACTIVE.equalsIgnoreCase(status.trim());
        String active = row.get("active");
        if (active != null) return Boolean.parseBoolean(active.trim());
        return true;
    }

    private static List<String> districtsOf(String stored) {
        if (stored == null || stored.isBlank()) return List.of();
        List<String> districts = new ArrayList<>();
        for (String part : stored.split(";")) {
            if (!part.isBlank()) districts.add(part.trim());
        }
        return districts;
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
