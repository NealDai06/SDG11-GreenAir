package RecommendationController;

import AirQualityController.AirQualityReading;
import java.util.Objects;

/** Advice for the general public using the AirNow US AQI bands. */
public class GeneralPublicStrategy implements RecommendationStrategy {
    /** Advises progressively less outdoor exertion as air quality deteriorates. */
    @Override
    public String advise(AirQualityReading reading) {
        int aqi = Objects.requireNonNull(reading, "reading").usAqi();
        if (aqi <= GOOD_MAX_AQI) return "Good air quality. Enjoy your usual outdoor activities.";
        if (aqi <= MODERATE_MAX_AQI) return "Moderate air quality. Unusually sensitive people should reduce prolonged or heavy outdoor exertion.";
        if (aqi <= SENSITIVE_MAX_AQI) return "Sensitive groups should reduce prolonged or heavy outdoor exertion. Others can continue usual activities.";
        if (aqi <= UNHEALTHY_MAX_AQI) return "Reduce prolonged or heavy outdoor exertion and take more breaks.";
        if (aqi <= VERY_UNHEALTHY_MAX_AQI) return "Avoid prolonged or heavy outdoor exertion. Prefer activities in cleaner indoor air.";
        return "Hazardous air quality. Avoid outdoor physical activity and remain in cleaner indoor air.";
    }
}
