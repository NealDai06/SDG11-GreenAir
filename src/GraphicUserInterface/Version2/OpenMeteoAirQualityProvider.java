package greenair.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * Skeleton for the Open-Meteo Air Quality API. The HTTP request is implemented; turning the JSON response
 * into {@link AirQualityReading} objects is left as the two TODO methods below.
 *
 * To enable it, set USE_LIVE_API to true in GreenAirApp. Until the parse methods are written, the
 * {@link CachingAirQualityProvider} falls back to the stored data automatically.
 */
public class OpenMeteoAirQualityProvider implements AirQualityProvider {
    private static final String BASE = "https://air-quality-api.open-meteo.com/v1/air-quality";

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final Function<String, double[]> districtCoordinates; // district name -> {latitude, longitude}

    public OpenMeteoAirQualityProvider(Function<String, double[]> districtCoordinates) {
        this.districtCoordinates = districtCoordinates;
    }

    @Override
    public AirQualityReading getCurrent(String district) throws IOException {
        String json = fetch(district);
        return parseCurrent(district, json);
    }

    @Override
    public List<AirQualityReading> getForecast(String district) throws IOException {
        String json = fetch(district);
        return parseForecast(district, json);
    }

    /** Requests current values plus hourly pm10, pm2_5 and us_aqi for the district's coordinates. */
    private String fetch(String district) throws IOException {
        double[] c = districtCoordinates.apply(district);
        if (c == null) throw new IOException("No coordinates configured for " + district);
        String url = BASE + "?latitude=" + c[0] + "&longitude=" + c[1]
                + "&current=pm10,pm2_5,us_aqi&hourly=pm10,pm2_5,us_aqi&forecast_days=2&timezone=Asia%2FKuala_Lumpur";
        try {
            HttpResponse<String> resp = http.send(HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) throw new IOException("Open-Meteo returned HTTP " + resp.statusCode());
            return resp.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    /** TODO: read current.time, current.pm2_5, current.pm10 and current.us_aqi from the JSON response. */
    private AirQualityReading parseCurrent(String district, String json) {
        throw new UnsupportedOperationException("Open-Meteo response parsing is not implemented yet");
    }

    /** TODO: build one reading per hourly.time entry from hourly.pm2_5, hourly.pm10 and hourly.us_aqi. */
    private List<AirQualityReading> parseForecast(String district, String json) {
        throw new UnsupportedOperationException("Open-Meteo response parsing is not implemented yet");
    }
}
