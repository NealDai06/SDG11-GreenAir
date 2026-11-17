package AirQualityController;

import java.util.List;
import java.util.Objects;

/** Entry point for air quality requests from the UI and other modules. */
public class AirQualityController {
    private final AirQualityService airQualityService;

    /** Creates the controller with the module's business service. */
    public AirQualityController(AirQualityService airQualityService) {
        this.airQualityService = Objects.requireNonNull(airQualityService, "airQualityService");
    }

    /** Gets the latest non-forecast reading. Its cached flag identifies stored data. */
    public AirQualityReading getCurrentReading(String pointId) {
        return airQualityService.getCurrentReading(pointId);
    }

    /** Gets the available future readings, in chronological order. */
    public List<AirQualityReading> getForecast(String pointId) {
        return airQualityService.getForecast(pointId);
    }

    /** Requests fresh data and falls back to the last stored reading if the API fails. */
    public AirQualityReading refresh(String pointId) {
        return airQualityService.refresh(pointId);
    }

    /** Gets stored non-forecast readings for the monitoring point, oldest first. */
    public List<AirQualityReading> getHistory(String pointId) {
        return airQualityService.getHistory(pointId);
    }
}
