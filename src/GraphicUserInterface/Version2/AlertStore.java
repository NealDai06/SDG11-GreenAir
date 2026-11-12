package greenair.data;

import java.nio.file.Path;
import java.util.*;

/** Reads and writes alerts.json. */
public class AlertStore {
    public record Alert(String time, String district, String pollutant, String level, String message) { }

    private final JsonFile file;

    public AlertStore(Path path) { this.file = new JsonFile(path); }

    /** Newest first. */
    public List<Alert> all() {
        List<Alert> out = new ArrayList<>();
        for (Map<String, String> m : file.load()) {
            out.add(new Alert(m.getOrDefault("time", ""), m.getOrDefault("district", ""),
                    m.getOrDefault("pollutant", ""), m.getOrDefault("level", ""), m.getOrDefault("message", "")));
        }
        out.sort(Comparator.comparing(Alert::time).reversed());
        return out;
    }

    /** Stores the alert unless one for the same district, pollutant and hour already exists. Returns true if added. */
    public boolean addIfNew(Alert a) {
        List<Map<String, String>> rows = file.load();
        String hour = a.time().length() >= 13 ? a.time().substring(0, 13) : a.time();
        for (Map<String, String> r : rows) {
            if (a.district().equalsIgnoreCase(r.getOrDefault("district", ""))
                    && a.pollutant().equals(r.getOrDefault("pollutant", ""))
                    && r.getOrDefault("time", "").startsWith(hour)) return false;
        }
        rows.add(JsonFile.row("time", a.time(), "district", a.district(), "pollutant", a.pollutant(),
                "level", a.level(), "message", a.message()));
        file.save(rows);
        return true;
    }
}
