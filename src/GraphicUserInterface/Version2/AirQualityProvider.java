package greenair.api;

import java.io.IOException;
import java.util.List;

/**
 * Source of air quality data. The rest of the application only depends on this interface, so a live
 * API (for example Open-Meteo) can replace or wrap the file-based provider without touching the UI.
 */
public interface AirQualityProvider {
    AirQualityReading getCurrent(String district) throws IOException;

    List<AirQualityReading> getForecast(String district) throws IOException;
}
