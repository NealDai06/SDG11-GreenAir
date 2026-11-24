package RecommendationController;

import AirQualityController.AirQualityReading;

/** Supplies outdoor activity advice for a population group using the reported US AQI. */
@FunctionalInterface
public interface RecommendationStrategy {
    int GOOD_MAX_AQI = 50;
    int MODERATE_MAX_AQI = 100;
    int SENSITIVE_MAX_AQI = 150;
    int UNHEALTHY_MAX_AQI = 200;
    int VERY_UNHEALTHY_MAX_AQI = 300;

    /** Returns group-specific activity advice for a non-null air quality reading. */
    String advise(AirQualityReading reading);
}
