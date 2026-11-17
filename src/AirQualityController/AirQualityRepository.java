package AirQualityController;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Stores readings and forecasts in the local air quality history file. */
public class AirQualityRepository {
    private static final String HISTORY_FILE = "air_history.json";
    private static final Comparator<AirQualityReading> BY_VALID_TIME = Comparator.comparing(AirQualityReading::validTime);

    private final Path path;
    private Map<ReadingKey, AirQualityReading> readings;
    private final Map<String, AirQualityReading> cachedData = new HashMap<>();

    /** Opens the history file in the application's configured data directory. */
    public AirQualityRepository() {
        this(Path.of(System.getProperty("greenair.data", "data")).resolve(HISTORY_FILE));
    }

    /** Loads an existing history file, or starts with empty history if it is missing. */
    public AirQualityRepository(Path path) {
        this.path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        readings = loadHistory();
        indexLatest();
    }

    /** Saves a reading, retaining the newer result for a repeated point and valid time. */
    public void save(AirQualityReading reading) {
        saveAll(List.of(reading));
    }

    /** Returns stored non-forecast readings for the monitoring point, oldest first. */
    public synchronized List<AirQualityReading> findHistory(String pointId) {
        return readings.values().stream()
                .filter(reading -> pointId.equals(reading.pointId()) && !reading.isForecast())
                .sorted(BY_VALID_TIME).toList();
    }

    /** Returns the latest non-forecast reading, or null if this point has no history. */
    public synchronized AirQualityReading findLatest(String pointId) {
        return cachedData.get(pointId);
    }

    synchronized List<AirQualityReading> findForecast(String pointId, LocalDateTime now) {
        return readings.values().stream()
                .filter(reading -> pointId.equals(reading.pointId()) && reading.isForecast())
                .filter(reading -> reading.validTime().isAfter(now))
                .sorted(BY_VALID_TIME).toList();
    }

    synchronized void saveAll(List<AirQualityReading> fetched) {
        Map<ReadingKey, AirQualityReading> updated = new LinkedHashMap<>(readings);
        fetched.forEach(reading -> addReading(updated, reading.withCached(false)));
        if (updated.equals(readings)) return;
        writeHistory(updated);
        readings = updated;
        indexLatest();
    }

    private Map<ReadingKey, AirQualityReading> loadHistory() {
        Map<ReadingKey, AirQualityReading> result = new LinkedHashMap<>();
        try {
            for (Object value : AirQualityJson.array(AirQualityJson.parse(Files.readString(path)))) {
                addReading(result, decode(AirQualityJson.object(value)));
            }
        } catch (NoSuchFileException e) {
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the local air quality history.", e);
        } catch (IllegalArgumentException | DateTimeException e) {
            throw new IllegalStateException("The local air quality history is invalid.", e);
        }
        return result;
    }

    private static void addReading(Map<ReadingKey, AirQualityReading> target, AirQualityReading reading) {
        if (reading.pointId().isEmpty()) throw new IllegalArgumentException("A monitoring point ID is required.");
        target.merge(new ReadingKey(reading.pointId(), reading.validTime()), reading,
                (stored, fetched) -> fetched.fetchedAt().isBefore(stored.fetchedAt()) ? stored : fetched);
    }

    private void indexLatest() {
        cachedData.clear();
        for (AirQualityReading reading : readings.values()) {
            if (reading.isForecast()) continue;
            cachedData.merge(reading.pointId(), reading,
                    (first, second) -> first.validTime().isAfter(second.validTime()) ? first : second);
        }
    }

    private void writeHistory(Map<ReadingKey, AirQualityReading> updated) {
        String contents = updated.values().stream().map(AirQualityRepository::encode)
                .collect(Collectors.joining(",\n", "[\n", "\n]\n"));
        Path temporary = null;
        try {
            Files.createDirectories(path.getParent());
            temporary = Files.createTempFile(path.getParent(), HISTORY_FILE, ".tmp");
            Files.writeString(temporary, contents);
            replaceHistory(temporary);
        } catch (IOException e) {
            deleteTemporaryFile(temporary, e);
            throw new UncheckedIOException("Could not save the local air quality history.", e);
        }
    }

    private void replaceHistory(Path temporary) throws IOException {
        try {
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteTemporaryFile(Path temporary, IOException error) {
        if (temporary == null) return;
        try {
            Files.deleteIfExists(temporary);
        } catch (IOException e) {
            error.addSuppressed(e);
        }
    }

    private static String encode(AirQualityReading reading) {
        return String.format(Locale.ROOT,
                "  {\"pointId\": %s, \"pm25\": %s, \"pm10\": %s, \"usAqi\": %d, \"validTime\": %s, \"fetchedAt\": %s}",
                AirQualityJson.quote(reading.pointId()), reading.pm25(), reading.pm10(), reading.usAqi(),
                AirQualityJson.quote(reading.validTime().toString()), AirQualityJson.quote(reading.fetchedAt().toString()));
    }

    private static AirQualityReading decode(Map<?, ?> row) {
        double index = AirQualityJson.number(row.get("usAqi"));
        if (index < 0 || index > Integer.MAX_VALUE || index != Math.rint(index)) {
            throw new IllegalArgumentException("Invalid US AQI in history.");
        }
        return new AirQualityReading(AirQualityJson.string(row.get("pointId")),
                AirQualityJson.number(row.get("pm25")), AirQualityJson.number(row.get("pm10")), (int) index,
                LocalDateTime.parse(AirQualityJson.string(row.get("validTime"))),
                LocalDateTime.parse(AirQualityJson.string(row.get("fetchedAt"))));
    }

    private record ReadingKey(String pointId, LocalDateTime validTime) { }
}
