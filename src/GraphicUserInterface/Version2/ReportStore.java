package greenair.data;

import java.nio.file.Path;
import java.util.*;

/** Reads and writes reports.json (citizen reports). */
public class ReportStore {
    public record Report(String id, String submitted, String type, String district, String reporter,
                         String status, String location, String description) { }

    private final JsonFile file;

    public ReportStore(Path path) { this.file = new JsonFile(path); }

    private static Report toReport(Map<String, String> m) {
        return new Report(m.getOrDefault("id", ""), m.getOrDefault("submitted", ""), m.getOrDefault("type", ""),
                m.getOrDefault("district", ""), m.getOrDefault("reporter", ""), m.getOrDefault("status", "Submitted"),
                m.getOrDefault("location", ""), m.getOrDefault("description", ""));
    }

    /** Newest first. */
    public List<Report> all() {
        List<Report> out = new ArrayList<>();
        for (Map<String, String> m : file.load()) out.add(toReport(m));
        out.sort(Comparator.comparing(Report::submitted).reversed());
        return out;
    }

    public Report add(String submitted, String type, String district, String reporter, String location, String description) {
        List<Map<String, String>> rows = file.load();
        Map<String, String> m = JsonFile.row("id", JsonFile.nextId(rows, "R"), "submitted", submitted, "type", type,
                "district", district, "reporter", reporter, "status", "Submitted",
                "location", location, "description", description);
        rows.add(m);
        file.save(rows);
        return toReport(m);
    }

    public void updateStatus(String id, String status) {
        List<Map<String, String>> rows = file.load();
        for (Map<String, String> r : rows) if (id.equals(r.get("id"))) r.put("status", status);
        file.save(rows);
    }
}
