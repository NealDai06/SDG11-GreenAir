package greenair.data;

import java.nio.file.Path;
import java.util.*;

/** Reads parks.json. */
public class ParkStore {
    public record Park(String name, String district, double latitude, double longitude) { }

    private final JsonFile file;

    public ParkStore(Path path) { this.file = new JsonFile(path); }

    public List<Park> all() {
        List<Park> out = new ArrayList<>();
        for (Map<String, String> m : file.load()) {
            out.add(new Park(m.getOrDefault("name", ""), m.getOrDefault("district", ""),
                    JsonFile.num(m, "latitude"), JsonFile.num(m, "longitude")));
        }
        return out;
    }
}
