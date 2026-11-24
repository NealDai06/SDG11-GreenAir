package RecommendationController;

import AirQualityController.AirQualityReading;
import AirQualityController.AirQualityService;
import MonitoringPointController.MonitoringPoint;
import MonitoringPointController.MonitoringPointService.MonitoringPointRequestException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Combines the class diagram's park repository and air quality service to rank parks. */
public class ParkRecommendationService {
    private final ParkRepository parkRepository;
    private final AirQualityService airQualityService;

    /** Connects the two dependencies shown in the class diagram. */
    public ParkRecommendationService(ParkRepository parkRepository, AirQualityService airQualityService) {
        this.parkRepository = Objects.requireNonNull(parkRepository, "parkRepository");
        this.airQualityService = Objects.requireNonNull(airQualityService, "airQualityService");
    }

    /** Ranks parks with available district data by US AQI, then distance from the requested point. */
    public List<Park> getRecommendedPark(String pointId) {
        MonitoringPoint origin = parkRepository.requirePoint(pointId);
        Map<String, Optional<AirQualityReading>> readings = new HashMap<>();
        List<Park> available = new ArrayList<>();
        for (Park park : parkRepository.findNearByParks(pointId)) {
            Optional<AirQualityReading> reading = readings.computeIfAbsent(
                    park.districtId().toLowerCase(Locale.ROOT), ignored -> districtReading(park.districtId()));
            if (reading.isPresent()) available.add(park);
        }
        available.sort(Comparator.comparingInt((Park park) ->
                        readings.get(park.districtId().toLowerCase(Locale.ROOT)).orElseThrow().usAqi())
                .thenComparingDouble(park -> park.distanceFrom(origin)).thenComparing(Park::parkId));
        return List.copyOf(available);
    }

    /**
     * Returns kilometres between two latitudes on the same meridian.
     * The two-argument signature follows the diagram; park ranking uses both coordinates internally.
     */
    public double calculateDistance(double latitude1, double latitude2) {
        return Park.distanceBetween(latitude1, 0, latitude2, 0);
    }

    AirQualityReading currentReading(String pointId) {
        return airQualityService.getCurrentReading(pointId);
    }

    private Optional<AirQualityReading> districtReading(String districtId) {
        MonitoringPoint point = parkRepository.pointForDistrict(districtId);
        if (point == null) return Optional.empty();
        try {
            return Optional.of(currentReading(point.id()));
        } catch (IllegalStateException | MonitoringPointRequestException e) {
            return Optional.empty();
        }
    }
}
