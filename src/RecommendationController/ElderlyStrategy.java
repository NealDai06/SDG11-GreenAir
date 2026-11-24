package RecommendationController;

import AirQualityController.AirQualityReading;
import java.util.Objects;

/** Advice for older adults, a group more sensitive to air pollution. */
public class ElderlyStrategy implements RecommendationStrategy {
    /** Adjusts outdoor walking and exercise to the reported US AQI band. */
    @Override
    public String advise(AirQualityReading reading) {
        int aqi = Objects.requireNonNull(reading, "reading").usAqi();
        if (aqi <= GOOD_MAX_AQI) return "Good air quality. Older adults can enjoy usual outdoor walks and exercise.";
        if (aqi <= MODERATE_MAX_AQI) return "Moderate air quality. Older adults unusually sensitive to pollution should reduce prolonged or heavy outdoor exercise.";
        if (aqi <= SENSITIVE_MAX_AQI) return "Older adults should reduce prolonged or heavy outdoor exercise and take more breaks.";
        if (aqi <= UNHEALTHY_MAX_AQI) return "Older adults should avoid prolonged or heavy outdoor exertion. Exercise in cleaner indoor air.";
        if (aqi <= VERY_UNHEALTHY_MAX_AQI) return "Older adults should avoid all outdoor physical activity. Remain in cleaner indoor air.";
        return "Hazardous air quality. Older adults should stay in cleaner indoor air and avoid outdoor physical activity.";
    }
}
