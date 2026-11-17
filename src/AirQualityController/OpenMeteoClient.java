package AirQualityController;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_MULT_CHOICE;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;

/** Fetches and parses current, historical and forecast readings from Open-Meteo. */
public class OpenMeteoClient {
    private static final URI ENDPOINT = URI.create("https://air-quality-api.open-meteo.com/v1/air-quality");
    private static final String METRICS = "pm2_5,pm10,us_aqi";
    private static final int PAST_HOURS = 6;
    private static final int CURRENT_HOUR = 1;
    private static final int FORECAST_HOURS = 48;
    private static final double MAX_LATITUDE = 90;
    private static final double MAX_LONGITUDE = 180;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    static final ZoneId TIME_ZONE = ZoneId.of("Asia/Kuala_Lumpur");

    private final HttpClient httpClient;
    private final Clock clock;

    /** Creates a client with connection and request timeouts. */
    public OpenMeteoClient() {
        this(HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build(), Clock.system(TIME_ZONE));
    }

    OpenMeteoClient(HttpClient httpClient, Clock clock) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.clock = Objects.requireNonNull(clock, "clock").withZone(TIME_ZONE);
    }

    /** Fetches current and recent readings for the coordinates, oldest first. */
    public List<AirQualityReading> fetchAirQuality(double latitude, double longitude) throws IOException {
        String options = "&current=" + METRICS + "&past_hours=" + PAST_HOURS + "&forecast_hours=" + CURRENT_HOUR;
        return fetchReadings(latitude, longitude, options).stream()
                .filter(reading -> !reading.isForecast()).toList();
    }

    /** Fetches forecast readings for the coordinates, earliest first. */
    public List<AirQualityReading> fetchForecast(double latitude, double longitude) throws IOException {
        // Open-Meteo includes the current hour in the requested window.
        String options = "&forecast_hours=" + (FORECAST_HOURS + CURRENT_HOUR);
        return fetchReadings(latitude, longitude, options).stream()
                .filter(AirQualityReading::isForecast).toList();
    }

    private List<AirQualityReading> fetchReadings(double latitude, double longitude, String options) throws IOException {
        HttpResponse<String> response = send(buildRequest(latitude, longitude, options));
        int status = response.statusCode();
        if (status < HTTP_OK || status >= HTTP_MULT_CHOICE) {
            throw new IOException(errorMessage(status));
        }
        return parseReadings(response.body(), LocalDateTime.now(clock));
    }

    private HttpRequest buildRequest(double latitude, double longitude, String options) {
        if (!validCoordinate(latitude, MAX_LATITUDE) || !validCoordinate(longitude, MAX_LONGITUDE)) {
            throw new IllegalArgumentException("The monitoring point has invalid coordinates.");
        }
        String query = "?latitude=" + latitude + "&longitude=" + longitude
                + "&hourly=" + METRICS + options
                + "&timezone=" + URLEncoder.encode(TIME_ZONE.getId(), StandardCharsets.UTF_8);
        return HttpRequest.newBuilder(URI.create(ENDPOINT + query)).timeout(REQUEST_TIMEOUT).GET().build();
    }

    private static boolean validCoordinate(double value, double limit) {
        return Double.isFinite(value) && Math.abs(value) <= limit;
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new IOException("The air quality service did not respond in time.", e);
        } catch (IOException e) {
            throw new IOException("Could not connect to the air quality service.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("The air quality request was interrupted.", e);
        }
    }

    private static String errorMessage(int status) {
        if (status == HTTP_NOT_FOUND) return "The air quality service could not be found (HTTP 404).";
        if (status >= HTTP_INTERNAL_ERROR) return "The air quality service is temporarily unavailable (HTTP " + status + ").";
        return "The air quality service could not complete the request (HTTP " + status + ").";
    }

    static List<AirQualityReading> parseReadings(String body, LocalDateTime fetchedAt) throws IOException {
        try {
            Map<?, ?> response = AirQualityJson.object(AirQualityJson.parse(body));
            var readings = new TreeMap<LocalDateTime, AirQualityReading>();
            addHourly(readings, AirQualityJson.object(response.get("hourly")), fetchedAt);
            if (response.get("current") instanceof Map<?, ?> current) {
                addCurrent(readings, current, fetchedAt);
            }
            return List.copyOf(readings.values());
        } catch (IllegalArgumentException | DateTimeException e) {
            throw new IOException("The air quality service returned unreadable data.", e);
        }
    }

    private static void addHourly(Map<LocalDateTime, AirQualityReading> readings, Map<?, ?> hourly,
                                  LocalDateTime fetchedAt) {
        List<?> times = AirQualityJson.array(hourly.get("time"));
        List<?> pm25 = AirQualityJson.array(hourly.get("pm2_5"));
        List<?> pm10 = AirQualityJson.array(hourly.get("pm10"));
        List<?> aqi = AirQualityJson.array(hourly.get("us_aqi"));
        if (times.size() != pm25.size() || times.size() != pm10.size() || times.size() != aqi.size()) {
            throw new IllegalArgumentException("The hourly air quality arrays have different lengths.");
        }
        for (int i = 0; i < times.size(); i++) {
            if (pm25.get(i) == null || pm10.get(i) == null || aqi.get(i) == null) continue;
            AirQualityReading reading = reading(times.get(i), pm25.get(i), pm10.get(i), aqi.get(i), fetchedAt);
            readings.put(reading.validTime(), reading);
        }
    }

    private static void addCurrent(Map<LocalDateTime, AirQualityReading> readings, Map<?, ?> current,
                                   LocalDateTime fetchedAt) {
        Object pm25 = current.get("pm2_5");
        Object pm10 = current.get("pm10");
        Object aqi = current.get("us_aqi");
        if (pm25 == null || pm10 == null || aqi == null) return;
        AirQualityReading reading = reading(current.get("time"), pm25, pm10, aqi, fetchedAt);
        if (!reading.isForecast()) readings.put(reading.validTime(), reading);
    }

    private static AirQualityReading reading(Object time, Object pm25, Object pm10, Object aqi,
                                             LocalDateTime fetchedAt) {
        double index = AirQualityJson.number(aqi);
        if (!Double.isFinite(index) || index < 0 || index > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid US AQI.");
        }
        return new AirQualityReading("", AirQualityJson.number(pm25), AirQualityJson.number(pm10),
                (int) Math.round(index), LocalDateTime.parse(AirQualityJson.string(time)), fetchedAt);
    }
}
