package AirQualityController;

import AlertController.PollutionAlertService;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Retrieves current and forecast data, persists it and passes fresh readings to the alert module. */
public class AirQualityService {
    private static final Duration CACHE_MAX_AGE = Duration.ofMinutes(30);

    private final OpenMeteoClient apiClient;
    private final AirQualityRepository airQualityRepository;
    private final PollutionAlertService alertService;
    private final Function<String, Coordinates> findCoordinates;
    private final BiConsumer<PollutionAlertService, AirQualityReading> checkThreshold;
    private final Clock clock;

    /**
     * Wires the API client, history repository, pollution alert service and monitoring point lookup.
     *
     * @param apiClient retrieves air quality data
     * @param airQualityRepository stores and retrieves readings
     * @param alertService receives fresh readings for threshold checks
     * @param findCoordinates looks up coordinates by monitoring point ID, or returns null for an unknown point
     * @param checkThreshold invokes the alert service's threshold check once that module implements it
     */
    public AirQualityService(OpenMeteoClient apiClient, AirQualityRepository airQualityRepository,
                             PollutionAlertService alertService, Function<String, Coordinates> findCoordinates,
                             BiConsumer<PollutionAlertService, AirQualityReading> checkThreshold) {
        this(apiClient, airQualityRepository, alertService, findCoordinates, checkThreshold,
                Clock.system(OpenMeteoClient.TIME_ZONE));
    }

    AirQualityService(OpenMeteoClient apiClient, AirQualityRepository airQualityRepository,
                      PollutionAlertService alertService, Function<String, Coordinates> findCoordinates,
                      BiConsumer<PollutionAlertService, AirQualityReading> checkThreshold, Clock clock) {
        this.apiClient = Objects.requireNonNull(apiClient, "apiClient");
        this.airQualityRepository = Objects.requireNonNull(airQualityRepository, "airQualityRepository");
        this.alertService = Objects.requireNonNull(alertService, "alertService");
        this.findCoordinates = Objects.requireNonNull(findCoordinates, "findCoordinates");
        this.checkThreshold = Objects.requireNonNull(checkThreshold, "checkThreshold");
        this.clock = Objects.requireNonNull(clock, "clock").withZone(OpenMeteoClient.TIME_ZONE);
    }

    /** Geographic coordinates from a configured monitoring point. */
    public record Coordinates(double latitude, double longitude) { }

    /** Reuses recently fetched data for up to 30 minutes, then tries to refresh. */
    public AirQualityReading getCurrentReading(String pointId) {
        String id = requirePointId(pointId);
        AirQualityReading latest = airQualityRepository.findLatest(id);
        if (latest != null && latest.fetchedAt().isAfter(LocalDateTime.now(clock).minus(CACHE_MAX_AGE))) {
            return latest.withCached(true);
        }
        return refresh(id);
    }

    /** Gets forecasts after the current time, refreshing stale or missing data when possible. */
    public List<AirQualityReading> getForecast(String pointId) {
        String id = requirePointId(pointId);
        LocalDateTime now = LocalDateTime.now(clock);
        List<AirQualityReading> saved = airQualityRepository.findForecast(id, now);
        if (saved.stream().noneMatch(reading -> reading.fetchedAt().isAfter(now.minus(CACHE_MAX_AGE)))) {
            try {
                return fetchAndSaveForecast(id);
            } catch (IOException e) {
                if (saved.isEmpty()) {
                    throw new IllegalStateException(e.getMessage() + " No saved forecast is available.", e);
                }
            }
        }
        return saved.stream().map(reading -> reading.withCached(true)).toList();
    }

    /** Gets saved non-forecast readings without making a network request. */
    public List<AirQualityReading> getHistory(String pointId) {
        return airQualityRepository.findHistory(requirePointId(pointId));
    }

    /**
     * Requests current and recent readings and checks the latest fresh reading for pollution alerts.
     * If the API fails, returns the last known non-forecast reading with its cached flag set.
     *
     * @throws IllegalStateException if the API fails and there is no reading for this point
     */
    public AirQualityReading refresh(String pointId) {
        String id = requirePointId(pointId);
        AirQualityReading current;
        try {
            current = fetchAndSaveCurrent(id);
        } catch (IOException e) {
            return cachedReading(id, e);
        }
        checkThreshold.accept(alertService, current);
        return current;
    }

    private AirQualityReading fetchAndSaveCurrent(String pointId) throws IOException {
        Coordinates point = coordinatesFor(pointId);
        List<AirQualityReading> readings = forPoint(pointId, apiClient.fetchAirQuality(point.latitude(), point.longitude()));
        AirQualityReading current = readings.stream()
                .max(Comparator.comparing(AirQualityReading::validTime))
                .orElseThrow(() -> new IOException("The air quality service returned no current reading."));
        airQualityRepository.saveAll(readings);
        return current;
    }

    private List<AirQualityReading> fetchAndSaveForecast(String pointId) throws IOException {
        Coordinates point = coordinatesFor(pointId);
        List<AirQualityReading> forecast = forPoint(pointId, apiClient.fetchForecast(point.latitude(), point.longitude()));
        if (forecast.isEmpty()) throw new IOException("The air quality service returned no forecast readings.");
        airQualityRepository.saveAll(forecast);
        return forecast;
    }

    private Coordinates coordinatesFor(String pointId) {
        Coordinates point = findCoordinates.apply(pointId);
        if (point == null) throw new IllegalArgumentException("No monitoring point found for " + pointId + ".");
        return point;
    }

    private static List<AirQualityReading> forPoint(String pointId, List<AirQualityReading> readings) {
        return readings.stream().map(reading -> reading.withPointId(pointId)).toList();
    }

    private AirQualityReading cachedReading(String pointId, IOException error) {
        AirQualityReading latest = airQualityRepository.findLatest(pointId);
        if (latest == null) {
            throw new IllegalStateException(error.getMessage() + " No saved reading is available.", error);
        }
        return latest.withCached(true);
    }

    private static String requirePointId(String pointId) {
        if (pointId == null || pointId.isBlank()) throw new IllegalArgumentException("Select a monitoring point first.");
        return pointId.strip();
    }
}
