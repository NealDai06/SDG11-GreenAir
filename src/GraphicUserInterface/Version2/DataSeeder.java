package greenair.data;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Creates the JSON data files with starter content the first time the app runs. Existing files are never
 * overwritten, so edits made through the UI or by hand are kept.
 */
public final class DataSeeder {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private DataSeeder() { }

    public static void seedIfMissing(Path dir) {
        seed(dir.resolve("users.json"), () -> List.of(
                JsonFile.row("id", "U001", "username", "admin", "fullName", "System Administrator", "role", UserStore.ADMIN,
                        "groupType", "General public", "status", "Active", "districts", ""),
                JsonFile.row("id", "U002", "username", "officer", "fullName", "Environment Officer", "role", UserStore.OFFICER,
                        "groupType", "General public", "status", "Active", "districts", ""),
                JsonFile.row("id", "U003", "username", "resident", "fullName", "Sample Resident", "role", UserStore.RESIDENT,
                        "groupType", "General public", "status", "Active", "districts", "Petaling Jaya;Shah Alam")));

        String[][] districts = {
                {"Kuala Lumpur", "3.1390", "101.6869"}, {"Petaling Jaya", "3.1073", "101.6067"},
                {"Shah Alam", "3.0733", "101.5185"}, {"Klang", "3.0449", "101.4455"},
                {"Subang Jaya", "3.0565", "101.5851"}, {"Kajang", "2.9927", "101.7909"},
                {"Ampang", "3.1500", "101.7600"}, {"Cheras", "3.1000", "101.7400"},
                {"Puchong", "3.0333", "101.6167"}, {"Rawang", "3.3217", "101.5767"}};

        seed(dir.resolve("monitoring_points.json"), () -> {
            List<Map<String, String>> rows = new ArrayList<>();
            for (int i = 0; i < districts.length; i++) {
                rows.add(JsonFile.row("id", String.format("MP%03d", i + 1), "name", districts[i][0] + " Station",
                        "district", districts[i][0], "latitude", districts[i][1], "longitude", districts[i][2], "active", "true"));
            }
            return rows;
        });

        // Sample parks: coordinates are approximate starter data. Edit parks.json to match your real park list.
        seed(dir.resolve("parks.json"), () -> List.of(
                park("Taman Tugu", "Kuala Lumpur", "3.1523", "101.6872"),
                park("Taman Tasik Perdana", "Kuala Lumpur", "3.1430", "101.6860"),
                park("Taman Tasik Titiwangsa", "Kuala Lumpur", "3.1760", "101.7060"),
                park("Taman Jaya", "Petaling Jaya", "3.1180", "101.6440"),
                park("Taman Tasik Shah Alam", "Shah Alam", "3.0730", "101.5260"),
                park("Taman Awam Subang Jaya", "Subang Jaya", "3.0600", "101.5900"),
                park("Taman Rekreasi Ampang", "Ampang", "3.1450", "101.7600"),
                park("Taman Cheras Hartamas", "Cheras", "3.1050", "101.7450"),
                park("Taman Rekreasi Puchong", "Puchong", "3.0300", "101.6200"),
                park("Taman Rekreasi Klang", "Klang", "3.0500", "101.4500")));

        seed(dir.resolve("air_history.json"), () -> {
            List<Map<String, String>> rows = new ArrayList<>();
            Random rnd = new Random(42);
            LocalDateTime now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
            int[] hoursAhead = {3, 6, 12, 24};
            for (String[] d : districts) {
                double base = 8 + rnd.nextInt(50);
                rows.add(air("current", d[0], now, base, base * 1.5));
                for (int h : hoursAhead) {
                    double pm25 = Math.max(3, base + rnd.nextInt(21) - 10);
                    rows.add(air("forecast", d[0], now.plusHours(h), pm25, pm25 * 1.5));
                }
            }
            return rows;
        });

        seed(dir.resolve("reports.json"), List::of);
        seed(dir.resolve("alerts.json"), List::of);
    }

    private static Map<String, String> park(String name, String district, String lat, String lon) {
        return JsonFile.row("name", name, "district", district, "latitude", lat, "longitude", lon);
    }

    private static Map<String, String> air(String type, String district, LocalDateTime t, double pm25, double pm10) {
        return JsonFile.row("type", type, "district", district, "time", t.format(TS),
                "pm25", String.format(Locale.ROOT, "%.1f", pm25), "pm10", String.format(Locale.ROOT, "%.1f", pm10),
                "usAqi", String.valueOf(aqiFromPm25(pm25)));
    }

    /** US EPA AQI for PM2.5 (24-hour breakpoints, linear interpolation). */
    public static int aqiFromPm25(double c) {
        double[][] bp = {{0, 12, 0, 50}, {12.1, 35.4, 51, 100}, {35.5, 55.4, 101, 150}, {55.5, 150.4, 151, 200},
                {150.5, 250.4, 201, 300}, {250.5, 500.4, 301, 500}};
        for (double[] b : bp) {
            if (c <= b[1]) return (int) Math.round((b[3] - b[2]) / (b[1] - b[0]) * (Math.max(c, b[0]) - b[0]) + b[2]);
        }
        return 500;
    }

    private static void seed(Path file, java.util.function.Supplier<List<Map<String, String>>> rows) {
        if (!Files.exists(file)) new JsonFile(file).save(rows.get());
    }
}
