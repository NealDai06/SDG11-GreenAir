package greenair.data;

import java.nio.file.Path;
import java.util.*;

/** Reads and writes users.json (ProfileRepository equivalent). */
public class UserStore {
    public record User(String id, String username, String fullName, String role,
                       String groupType, String status, List<String> districts) { }

    public static final String RESIDENT = "RESIDENT";
    public static final String OFFICER = "ENVIRONMENT_OFFICER";
    public static final String ADMIN = "ADMINISTRATOR";

    private final JsonFile file;

    public UserStore(Path path) { this.file = new JsonFile(path); }

    private static User toUser(Map<String, String> m) {
        String d = m.getOrDefault("districts", "");
        List<String> districts = d.isBlank() ? List.of() : List.of(d.split(";"));
        return new User(m.getOrDefault("id", ""), m.getOrDefault("username", ""), m.getOrDefault("fullName", ""),
                m.getOrDefault("role", RESIDENT), m.getOrDefault("groupType", "General public"),
                m.getOrDefault("status", "Active"), districts);
    }

    public List<User> all() {
        List<User> out = new ArrayList<>();
        for (Map<String, String> m : file.load()) out.add(toUser(m));
        return out;
    }

    /** Case-insensitive lookup used by the login screen. */
    public Optional<User> findByUsername(String username) {
        String wanted = username.trim();
        return all().stream().filter(u -> u.username().equalsIgnoreCase(wanted)).findFirst();
    }

    public User add(String username, String fullName, String role, String groupType, List<String> districts) {
        List<Map<String, String>> rows = file.load();
        for (Map<String, String> r : rows) {
            if (r.getOrDefault("username", "").equalsIgnoreCase(username.trim())) {
                throw new IllegalArgumentException("The username \"" + username + "\" is already taken.");
            }
        }
        Map<String, String> m = JsonFile.row("id", JsonFile.nextId(rows, "U"), "username", username.trim(),
                "fullName", fullName.trim(), "role", role, "groupType", groupType, "status", "Active",
                "districts", String.join(";", districts));
        rows.add(m);
        file.save(rows);
        return toUser(m);
    }

    public void updateRole(String id, String role) { update(id, "role", role); }
    public void setStatus(String id, String status) { update(id, "status", status); }

    public void delete(String id) {
        List<Map<String, String>> rows = file.load();
        rows.removeIf(r -> id.equals(r.get("id")));
        file.save(rows);
    }

    private void update(String id, String key, String value) {
        List<Map<String, String>> rows = file.load();
        for (Map<String, String> r : rows) if (id.equals(r.get("id"))) r.put(key, value);
        file.save(rows);
    }
}
