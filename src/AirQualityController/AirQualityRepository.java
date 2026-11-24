package AirQualityController;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
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
import java.util.concurrent.ConcurrentHashMap;

/** Stores readings and forecasts in the local air quality history file. */
public class AirQualityRepository {
    private static final String HISTORY_FILE = "air_history.json";
    private static final Comparator<AirQualityReading> BY_VALID_TIME = Comparator.comparing(AirQualityReading::validTime);
    private static final Map<Path, Object> FILE_LOCKS = new ConcurrentHashMap<>();

    private final Path path;
    private Map<ReadingKey, AirQualityReading> readings;
    private final Map<String, AirQualityReading> cachedData = new HashMap<>();
    // Persistence metadata only; AirQualityReading keeps the fields from the class diagram.
    private final Map<String, String> pointCoordinates = new HashMap<>();
    private final Map<ReadingKey, String> readingCoordinates = new HashMap<>();

    synchronized boolean useCoordinates(String pointId, double latitude, double longitude) {
        String coordinates = Double.toString(latitude) + "," + Double.toString(longitude);
        return !coordinates.equals(pointCoordinates.put(pointId, coordinates));
    }

    private boolean matchesCoordinates(AirQualityReading reading) {
        String expected = pointCoordinates.get(reading.pointId());
        return expected == null || expected.equals(readingCoordinates.get(
                new ReadingKey(reading.pointId(), reading.validTime())));
    }

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
        refreshSnapshot();
        return readings.values().stream()
                .filter(reading -> pointId.equals(reading.pointId()) && !reading.isForecast())
                .sorted(BY_VALID_TIME).toList();
    }

    /** Returns the latest non-forecast reading, or null if this point has no history. */
    public synchronized AirQualityReading findLatest(String pointId) {
        refreshSnapshot();
        return cachedData.get(pointId);
    }

    synchronized List<AirQualityReading> findForecast(String pointId, LocalDateTime now) {
        refreshSnapshot();
        List<AirQualityReading> forecasts = readings.values().stream()
                .filter(reading -> pointId.equals(reading.pointId()) && reading.isForecast())
                .filter(this::matchesCoordinates)
                .toList();
        LocalDateTime latestFetch = forecasts.stream().map(AirQualityReading::fetchedAt)
                .max(Comparator.naturalOrder()).orElse(LocalDateTime.MIN);
        return forecasts.stream().filter(reading -> reading.fetchedAt().equals(latestFetch))
                .filter(reading -> reading.validTime().isAfter(now))
                .sorted(BY_VALID_TIME).toList();
    }

    synchronized void saveAll(List<AirQualityReading> fetched) {
        synchronized (FILE_LOCKS.computeIfAbsent(path, ignored -> new Object())) {
            try {
                Files.createDirectories(path.getParent());
                Path lockPath = path.resolveSibling(path.getFileName() + ".lock");
                try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                     FileLock lock = channel.lock()) {
                    if (!lock.isValid()) throw new IOException("Could not lock the history file.");
                    mergeAndSave(fetched);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Could not save the local air quality history.", e);
            }
        }
    }

    private void mergeAndSave(List<AirQualityReading> fetched) {
        Map<ReadingKey, AirQualityReading> stored = loadHistory();
        Map<ReadingKey, AirQualityReading> updated = new LinkedHashMap<>(stored);
        boolean metadataChanged = false;
        for (AirQualityReading reading : fetched) {
            ReadingKey key = new ReadingKey(reading.pointId(), reading.validTime());
            AirQualityReading previous = updated.get(key);
            if (previous != null && reading.fetchedAt().isBefore(previous.fetchedAt())) continue;
            addReading(updated, reading.withCached(false));
            String coordinates = pointCoordinates.get(reading.pointId());
            metadataChanged |= !Objects.equals(readingCoordinates.put(key, coordinates), coordinates);
        }
        if (!updated.equals(stored) || metadataChanged) writeHistory(updated);
        readings = updated;
        indexLatest();
    }

    private void refreshSnapshot() {
        readings = loadHistory();
        indexLatest();
    }

    private Map<ReadingKey, AirQualityReading> loadHistory() {
        Map<ReadingKey, AirQualityReading> result = new LinkedHashMap<>();
        readingCoordinates.clear();
        try {
            for (Object value : AirQualityJson.array(AirQualityJson.parse(Files.readString(path)))) {
                Map<?, ?> row = AirQualityJson.object(value);
                AirQualityReading reading = decode(row);
                ReadingKey key = new ReadingKey(reading.pointId(), reading.validTime());
                AirQualityReading previous = result.get(key);
                addReading(result, reading);
                if (previous == null || !reading.fetchedAt().isBefore(previous.fetchedAt())) {
                    readingCoordinates.put(key, row.get("coordinates") instanceof String s ? s : null);
                }
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
            if (reading.isForecast() || !matchesCoordinates(reading)) continue;
            cachedData.merge(reading.pointId(), reading,
                    (first, second) -> first.validTime().isAfter(second.validTime()) ? first : second);
        }
    }

    private void writeHistory(Map<ReadingKey, AirQualityReading> updated) {
        String contents = updated.values().stream().map(this::encode)
                .collect(Collectors.joining(",\n", "[\n", "\n]\n"));
        Path temporary = null;
        try {
            Files.createDirectories(path.getParent());
            temporary = Files.createTempFile(path.getParent(), HISTORY_FILE, ".tmp");
            Files.writeString(temporary, contents);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException cleanupError) { e.addSuppressed(cleanupError); }
            }
            throw new UncheckedIOException("Could not save the local air quality history.", e);
        }
    }

    private String encode(AirQualityReading reading) {
        String coordinates = readingCoordinates.get(new ReadingKey(reading.pointId(), reading.validTime()));
        return String.format(Locale.ROOT,
                "  {\"pointId\": %s, \"pm25\": %s, \"pm10\": %s, \"usAqi\": %d, \"validTime\": %s, \"fetchedAt\": %s, \"coordinates\": %s}",
                AirQualityJson.quote(reading.pointId()), reading.pm25(), reading.pm10(), reading.usAqi(),
                AirQualityJson.quote(reading.validTime().toString()), AirQualityJson.quote(reading.fetchedAt().toString()),
                coordinates == null ? "null" : AirQualityJson.quote(coordinates));
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
