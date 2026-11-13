package greenair.api;

/** One air quality observation or forecast step. {@code time} uses the format yyyy-MM-dd HH:mm. */
public record AirQualityReading(String district, String time, double pm25, double pm10, int usAqi) { }
