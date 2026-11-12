package greenair.api;

import greenair.data.JsonFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Serves air quality data from air_history.json. Also used as the cache/fallback for a live API. */
public class FileAirQualityProvider implements AirQualityProvider {
    private final JsonFile file;

    public FileAirQualityProvider(Path path) { this.file = new JsonFile(path); }

    private static AirQualityReading toReading(Map<String, String> m) {
        return new AirQualityReading(m.getOrDefault("district", ""), m.getOrDefault("time", ""),
                JsonFile.num(m, "pm25"), JsonFile.num(m, "pm10"), (int) Math.round(JsonFile.num(m, "usAqi")));
    }

    private static Map<String, String> toRow(String type, AirQualityReading r) {
        return JsonFile.row("type", type, "district", r.district(), "time", r.time(), "pm25", String.valueOf(r.pm25()),
                "pm10", String.valueOf(r.pm10()), "usAqi", String.valueOf(r.usAqi()));
    }

    @Override
    public AirQualityReading getCurrent(String district) throws IOException {
        return file.load().stream()
                .filter(m -> "current".equals(m.get("type")) && district.equalsIgnoreCase(m.get("district")))
                .max(Comparator.comparing(m -> m.getOrDefault("time", "")))
                .map(FileAirQualityProvider::toReading)
                .orElseThrow(() -> new IOException("No stored air quality reading for " + district));
    }

    @Override
    public List<AirQualityReading> getForecast(String district) throws IOException {
        List<AirQualityReading> out = new ArrayList<>();
        for (Map<String, String> m : file.load()) {
            if ("forecast".equals(m.get("type")) && district.equalsIgnoreCase(m.get("district"))) out.add(toReading(m));
        }
        out.sort(Comparator.comparing(AirQualityReading::time));
        return out;
    }

    /** Appends a current reading to the history file. */
    public void saveCurrent(AirQualityReading r) {
        List<Map<String, String>> rows = file.load();
        rows.add(toRow("current", r));
        file.save(rows);
    }

    /** Replaces the stored forecast for a district. */
    public void saveForecast(String district, List<AirQualityReading> forecast) {
        List<Map<String, String>> rows = file.load();
        rows.removeIf(m -> "forecast".equals(m.get("type")) && district.equalsIgnoreCase(m.get("district")));
        for (AirQualityReading r : forecast) rows.add(toRow("forecast", r));
        file.save(rows);
    }
}
