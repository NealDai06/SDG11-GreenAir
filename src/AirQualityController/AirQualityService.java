package AirQualityController;

import AlertController.PollutionAlertService;
import MonitoringPointController.MonitoringPoint;
import MonitoringPointController.MonitoringPointRepository;
import MonitoringPointController.MonitoringPointService;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Retrieves current and forecast data, persists it and passes fresh readings to the alert module. */
public class AirQualityService {
    private static final Duration CACHE_MAX_AGE = Duration.ofMinutes(30);
    private static final Duration FAILURE_RETRY_DELAY = Duration.ofMinutes(1);

    private final OpenMeteoClient apiClient;
    private final AirQualityRepository airQualityRepository;
    private final PollutionAlertService alertService;
    private final MonitoringPointService monitoringPointService;
    private final Clock clock;
    // Suffixes keep current and forecast failures separate for each monitoring point.
    private final Map<String, FailedRequest> failures = new HashMap<>();

    /**
     * Wires the API client, history repository, pollution alert service and monitoring point lookup.
     *
     * @param apiClient retrieves air quality data
     * @param airQualityRepository stores and retrieves readings
     * @param alertService receives fresh readings for threshold checks
     * @param monitoringPointService resolves point IDs from the local monitoring point list
     */
    public AirQualityService(OpenMeteoClient apiClient, AirQualityRepository airQualityRepository,
                             PollutionAlertService alertService, MonitoringPointService monitoringPointService) {
        this(apiClient, airQualityRepository, alertService, monitoringPointService,
                Clock.system(OpenMeteoClient.TIME_ZONE));
    }

    /** Uses the configured local monitoring_points.json file to resolve point IDs. */
    public AirQualityService(OpenMeteoClient apiClient, AirQualityRepository airQualityRepository,
                             PollutionAlertService alertService) {
        this(apiClient, airQualityRepository, alertService, new MonitoringPointService(
                new MonitoringPointRepository(Path.of(System.getProperty("greenair.data", "data"))
                        .resolve("monitoring_points.json"))));
    }

    AirQualityService(OpenMeteoClient apiClient, AirQualityRepository airQualityRepository,
                      PollutionAlertService alertService, MonitoringPointService monitoringPointService, Clock clock) {
        this.apiClient = Objects.requireNonNull(apiClient, "apiClient");
        this.airQualityRepository = Objects.requireNonNull(airQualityRepository, "airQualityRepository");
        this.alertService = Objects.requireNonNull(alertService, "alertService");
        this.monitoringPointService = Objects.requireNonNull(monitoringPointService, "monitoringPointService");
        this.clock = Objects.requireNonNull(clock, "clock").withZone(OpenMeteoClient.TIME_ZONE);
    }

    /** Reuses data for up to 30 minutes; failed requests wait one minute before another API attempt. */
    public synchronized AirQualityReading getCurrentReading(String pointId) {
        String id = requirePointId(pointId);
        requireActivePoint(id);
        AirQualityReading latest = airQualityRepository.findLatest(id);
        if (latest != null && latest.fetchedAt().isAfter(LocalDateTime.now(clock).minus(CACHE_MAX_AGE))) {
            return latest.withCached(true);
        }
        return refresh(id);
    }

    /** Gets forecasts after the current time, refreshing stale or missing data when possible. */
    public synchronized List<AirQualityReading> getForecast(String pointId) {
        String id = requirePointId(pointId);
        MonitoringPoint point = requireActivePoint(id);
        LocalDateTime now = LocalDateTime.now(clock);
        List<AirQualityReading> saved = airQualityRepository.findForecast(id, now);
        if (saved.stream().noneMatch(reading -> reading.fetchedAt().isAfter(now.minus(CACHE_MAX_AGE)))) {
            String key = id + ":forecast";
            try {
                checkRetryDelay(key);
                List<AirQualityReading> forecast = forPoint(id,
                        apiClient.fetchForecast(point.latitude(), point.longitude()));
                if (forecast.isEmpty()) throw new IOException("The air quality service returned no forecast readings.");
                airQualityRepository.saveAll(forecast);
                failures.remove(key);
                return forecast;
            } catch (IOException e) {
                rememberFailure(key, e);
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
    public synchronized AirQualityReading refresh(String pointId) {
        String id = requirePointId(pointId);
        MonitoringPoint point = requireActivePoint(id);
        String key = id + ":current";
        AirQualityReading current;
        try {
            checkRetryDelay(key);
            List<AirQualityReading> readings = forPoint(id,
                    apiClient.fetchAirQuality(point.latitude(), point.longitude()));
            current = readings.stream().max(Comparator.comparing(AirQualityReading::validTime))
                    .orElseThrow(() -> new IOException("The air quality service returned no current reading."));
            airQualityRepository.saveAll(readings);
            failures.remove(key);
        } catch (IOException e) {
            rememberFailure(key, e);
            AirQualityReading saved = airQualityRepository.findLatest(id);
            if (saved == null) {
                throw new IllegalStateException(e.getMessage() + " No saved reading is available.", e);
            }
            return saved.withCached(true);
        }
        alertService.checkThreshold(point.district(), current.usAqi());
        return current;
    }

    private void checkRetryDelay(String key) throws IOException {
        FailedRequest previous = failures.get(key);
        if (previous != null && LocalDateTime.now(clock).isBefore(previous.retryAt())) {
            throw previous.error();
        }
    }

    private void rememberFailure(String key, IOException error) {
        LocalDateTime now = LocalDateTime.now(clock);
        failures.compute(key, (ignored, previous) ->
                previous != null && now.isBefore(previous.retryAt())
                        ? previous : new FailedRequest(error, now.plus(FAILURE_RETRY_DELAY)));
    }

    private MonitoringPoint requireActivePoint(String pointId) {
        MonitoringPoint point = monitoringPointService.requirePoint(pointId);
        if (!point.active()) throw new IllegalArgumentException("The selected monitoring point is inactive.");
        if (airQualityRepository.useCoordinates(pointId, point.latitude(), point.longitude())) {
            failures.remove(pointId + ":current");
            failures.remove(pointId + ":forecast");
        }
        return point;
    }

    private static List<AirQualityReading> forPoint(String pointId, List<AirQualityReading> readings) {
        return readings.stream().map(reading -> reading.withPointId(pointId)).toList();
    }

    private static String requirePointId(String pointId) {
        if (pointId == null || pointId.isBlank()) throw new IllegalArgumentException("Select a monitoring point first.");
        return pointId.strip();
    }

    private record FailedRequest(IOException error, LocalDateTime retryAt) { }
}
