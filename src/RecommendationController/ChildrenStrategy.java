package RecommendationController;

import AirQualityController.AirQualityReading;
import java.util.Objects;

/** Advice for children, a group more sensitive to air pollution. */
public class ChildrenStrategy implements RecommendationStrategy {
    /** Adjusts outdoor play and sport to the reported US AQI band. */
    @Override
    public String advise(AirQualityReading reading) {
        int aqi = Objects.requireNonNull(reading, "reading").usAqi();
        if (aqi <= GOOD_MAX_AQI) return "Good air quality. Children can enjoy usual outdoor play and sports.";
        if (aqi <= MODERATE_MAX_AQI) return "Moderate air quality. Children unusually sensitive to pollution should reduce prolonged or heavy outdoor play.";
        if (aqi <= SENSITIVE_MAX_AQI) return "Children should reduce prolonged or heavy outdoor play and take more breaks.";
        if (aqi <= UNHEALTHY_MAX_AQI) return "Children should avoid prolonged or heavy outdoor exertion. Move sports to cleaner indoor air.";
        if (aqi <= VERY_UNHEALTHY_MAX_AQI) return "Children should avoid all outdoor physical activity. Choose activities in cleaner indoor air.";
        return "Hazardous air quality. Keep children in cleaner indoor air and avoid outdoor physical activity.";
    }
}
