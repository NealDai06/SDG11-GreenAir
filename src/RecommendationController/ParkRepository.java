package RecommendationController;

import AirQualityController.AirQualityJson;
import MonitoringPointController.MonitoringPoint;
import MonitoringPointController.MonitoringPointRepository;
import MonitoringPointController.MonitoringPointService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Reads parks.json and resolves monitoring points through the existing monitoring module. */
public class ParkRepository {
    private final Path path;
    private final MonitoringPointService monitoringPointService;

    /** Opens parks.json and monitoring_points.json in the configured data directory. */
    public ParkRepository() {
        this(Path.of(System.getProperty("greenair.data", "data")).resolve("parks.json"));
    }

    /** Opens a park file and the monitoring point file in the same directory. */
    public ParkRepository(Path path) {
        this(path, new MonitoringPointService(new MonitoringPointRepository(
                path.toAbsolutePath().resolveSibling("monitoring_points.json"))));
    }

    /** Uses the application's existing monitoring point service for point and district lookups. */
    public ParkRepository(Path path, MonitoringPointService monitoringPointService) {
        this.path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        this.monitoringPointService = Objects.requireNonNull(monitoringPointService, "monitoringPointService");
    }

    /** Returns the configured parks ordered by their distance from the requested monitoring point. */
    public List<Park> findNearByParks(String pointId) {
        MonitoringPoint origin = requirePoint(pointId);
        return findAll().stream().sorted(Comparator.comparingDouble(park -> park.distanceFrom(origin))).toList();
    }

    /** Finds a park by ID, or returns null when it is absent. */
    public Park findById(String parkId) {
        Objects.requireNonNull(parkId, "parkId");
        return findAll().stream().filter(park -> park.parkId().equals(parkId.strip())).findFirst().orElse(null);
    }

    /** Reads the current local park list, accepting numeric values and legacy numeric strings. */
    public List<Park> findAll() {
        try {
            List<Park> parks = AirQualityJson.array(AirQualityJson.parse(Files.readString(path))).stream()
                    .map(value -> decode(AirQualityJson.object(value))).toList();
            checkUniqueIds(parks);
            return parks;
        } catch (NoSuchFileException e) {
            return List.of();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the local park list.", e);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalStateException("The local park list is invalid.", e);
        }
    }

    MonitoringPoint requirePoint(String pointId) {
        MonitoringPoint point = monitoringPointService.requirePoint(pointId);
        if (!point.active()) throw new IllegalArgumentException("The selected monitoring point is inactive.");
        return point;
    }

    MonitoringPoint pointForDistrict(String districtId) {
        return monitoringPointService.findAll().stream()
                .filter(point -> point.active() && point.district().equalsIgnoreCase(districtId))
                .min(Comparator.comparing(MonitoringPoint::id)).orElse(null);
    }

    private static Park decode(Map<?, ?> row) {
        return new Park(AirQualityJson.string(row.get("parkId")), AirQualityJson.string(row.get("name")),
                AirQualityJson.number(row.get("latitude")), AirQualityJson.number(row.get("longitude")),
                AirQualityJson.string(row.get("districtId")));
    }

    private static void checkUniqueIds(List<Park> parks) {
        Set<String> ids = new HashSet<>();
        for (Park park : parks) {
            if (!ids.add(park.parkId())) throw new IllegalArgumentException("Duplicate park ID: " + park.parkId());
        }
    }
}
