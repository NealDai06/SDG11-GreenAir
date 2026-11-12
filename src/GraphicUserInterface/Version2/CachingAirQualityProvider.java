package greenair.api;

import java.io.IOException;
import java.util.List;

/**
 * Uses a live provider first, saves what it returns to the history file, and falls back to the stored
 * data when the live provider fails (no network, API error, or not implemented yet).
 */
public class CachingAirQualityProvider implements AirQualityProvider {
    private final AirQualityProvider live;
    private final FileAirQualityProvider cache;

    public CachingAirQualityProvider(AirQualityProvider live, FileAirQualityProvider cache) {
        this.live = live;
        this.cache = cache;
    }

    @Override
    public AirQualityReading getCurrent(String district) throws IOException {
        try {
            AirQualityReading r = live.getCurrent(district);
            cache.saveCurrent(r);
            return r;
        } catch (IOException | RuntimeException e) {
            return cache.getCurrent(district);
        }
    }

    @Override
    public List<AirQualityReading> getForecast(String district) throws IOException {
        try {
            List<AirQualityReading> f = live.getForecast(district);
            cache.saveForecast(district, f);
            return f;
        } catch (IOException | RuntimeException e) {
            return cache.getForecast(district);
        }
    }
}
