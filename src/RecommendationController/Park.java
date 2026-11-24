package RecommendationController;

import MonitoringPointController.MonitoringPoint;
import java.util.Objects;

/** A public park and the district whose monitoring point provides its air quality estimate. */
public record Park(String parkId, String name, double latitude, double longitude, String districtId) {
    private static final double EARTH_RADIUS_KM = 6371.0088;
    private static final double MAX_LATITUDE = 90;
    private static final double MAX_LONGITUDE = 180;

    /** Validates the park identity, district and geographic coordinates. */
    public Park {
        parkId = requiredText(parkId, "parkId");
        name = requiredText(name, "name");
        districtId = requiredText(districtId, "districtId");
        validateCoordinates(latitude, longitude);
    }

    double distanceFrom(MonitoringPoint origin) {
        return distanceBetween(origin.latitude(), origin.longitude(), latitude, longitude);
    }

    static double distanceBetween(double latitude1, double longitude1, double latitude2, double longitude2) {
        validateCoordinates(latitude1, longitude1);
        validateCoordinates(latitude2, longitude2);
        double latitudeDelta = Math.toRadians(latitude2 - latitude1);
        double longitudeDelta = Math.toRadians(longitude2 - longitude1);
        double haversine = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.pow(Math.sin(longitudeDelta / 2), 2);
        return EARTH_RADIUS_KM * 2 * Math.asin(Math.sqrt(Math.min(1, Math.max(0, haversine))));
    }

    private static void validateCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || Math.abs(latitude) > MAX_LATITUDE
                || !Double.isFinite(longitude) || Math.abs(longitude) > MAX_LONGITUDE) {
            throw new IllegalArgumentException("The park coordinates are invalid.");
        }
    }

    private static String requiredText(String text, String field) {
        String value = Objects.requireNonNull(text, field).strip();
        if (value.isEmpty()) throw new IllegalArgumentException("A park must have a " + field + ".");
        return value;
    }
}
