package greenair.data;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** A JSON file on disk holding a list of flat records. Every call reads/writes the file directly. */
public class JsonFile {
    private final Path path;

    public JsonFile(Path path) { this.path = path; }

    public Path path() { return path; }

    public synchronized List<Map<String, String>> load() {
        if (!Files.exists(path)) return new ArrayList<>();
        try {
            return Json.parse(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + path.getFileName() + ": " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(path.getFileName() + " is not valid JSON: " + e.getMessage(), e);
        }
    }

    public synchronized void save(List<Map<String, String>> rows) {
        try {
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(tmp, Json.write(rows), StandardCharsets.UTF_8);
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + path.getFileName() + ": " + e.getMessage(), e);
        }
    }

    /** Next id such as U004, built from the highest numeric suffix already used with this prefix. */
    public static String nextId(List<Map<String, String>> rows, String prefix) {
        int max = 0;
        for (Map<String, String> r : rows) {
            String id = r.getOrDefault("id", "");
            if (id.startsWith(prefix)) {
                try { max = Math.max(max, Integer.parseInt(id.substring(prefix.length()))); }
                catch (NumberFormatException ignored) { }
            }
        }
        return prefix + String.format("%03d", max + 1);
    }

    public static Map<String, String> row(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    public static double num(Map<String, String> m, String key) {
        try { return Double.parseDouble(m.getOrDefault(key, "0")); } catch (NumberFormatException e) { return 0; }
    }
}
