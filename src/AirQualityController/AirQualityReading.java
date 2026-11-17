package AirQualityController;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * PM2.5 and PM10 in µg/m³ and the US AQI for a monitoring point.
 * Both timestamps use Kuala Lumpur local time. The cached flag describes how
 * the service returned this reading and is independent of the measurement time.
 */
public record AirQualityReading(String pointId, double pm25, double pm10, int usAqi,
                                LocalDateTime validTime, LocalDateTime fetchedAt, boolean cached) {

    /** Creates a reading retrieved from the API. */
    public AirQualityReading(String pointId, double pm25, double pm10, int usAqi,
                             LocalDateTime validTime, LocalDateTime fetchedAt) {
        this(pointId, pm25, pm10, usAqi, validTime, fetchedAt, false);
    }

    /** Validates the timestamps and measurement values. */
    public AirQualityReading {
        pointId = Objects.requireNonNull(pointId, "pointId").strip();
        Objects.requireNonNull(validTime, "validTime");
        Objects.requireNonNull(fetchedAt, "fetchedAt");
        if (!nonNegativeFinite(pm25) || !nonNegativeFinite(pm10) || usAqi < 0) {
            throw new IllegalArgumentException("Air quality values must be finite and non-negative.");
        }
    }

    boolean isForecast() {
        return validTime.isAfter(fetchedAt);
    }

    AirQualityReading withPointId(String id) {
        return new AirQualityReading(id, pm25, pm10, usAqi, validTime, fetchedAt, cached);
    }

    AirQualityReading withCached(boolean fromCache) {
        return new AirQualityReading(pointId, pm25, pm10, usAqi, validTime, fetchedAt, fromCache);
    }

    private static boolean nonNegativeFinite(double value) {
        return Double.isFinite(value) && value >= 0;
    }
}
