package greenair.data;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/** Reads and writes monitoring_points.json. */
public class MonitoringPointStore {
    public record Point(String id, String name, String district, double latitude, double longitude, boolean active) { }

    private final JsonFile file;

    public MonitoringPointStore(Path path) { this.file = new JsonFile(path); }

    private static Point toPoint(Map<String, String> m) {
        return new Point(m.getOrDefault("id", ""), m.getOrDefault("name", ""), m.getOrDefault("district", ""),
                JsonFile.num(m, "latitude"), JsonFile.num(m, "longitude"),
                Boolean.parseBoolean(m.getOrDefault("active", "true")));
    }

    public List<Point> all() {
        List<Point> out = new ArrayList<>();
        for (Map<String, String> m : file.load()) out.add(toPoint(m));
        return out;
    }

    /** Sorted district names of active monitoring points. */
    public List<String> districts() {
        return all().stream().filter(Point::active).map(Point::district).distinct().sorted().collect(Collectors.toList());
    }

    /** Average coordinates of a district's monitoring points, used as the district centre. */
    public Optional<double[]> centre(String district) {
        List<Point> ps = all().stream().filter(p -> p.district().equalsIgnoreCase(district)).toList();
        if (ps.isEmpty()) return Optional.empty();
        return Optional.of(new double[]{ps.stream().mapToDouble(Point::latitude).average().orElse(0),
                ps.stream().mapToDouble(Point::longitude).average().orElse(0)});
    }

    /** Adds the point when its id is blank, otherwise updates the point with that id. */
    public void save(Point p) {
        List<Map<String, String>> rows = file.load();
        Map<String, String> target = null;
        if (!p.id().isBlank()) for (Map<String, String> r : rows) if (p.id().equals(r.get("id"))) target = r;
        if (target == null) {
            target = JsonFile.row("id", p.id().isBlank() ? JsonFile.nextId(rows, "MP") : p.id());
            rows.add(target);
        }
        target.put("name", p.name());
        target.put("district", p.district());
        target.put("latitude", String.valueOf(p.latitude()));
        target.put("longitude", String.valueOf(p.longitude()));
        target.put("active", String.valueOf(p.active()));
        file.save(rows);
    }

    public void delete(String id) {
        List<Map<String, String>> rows = file.load();
        rows.removeIf(r -> id.equals(r.get("id")));
        file.save(rows);
    }
}
