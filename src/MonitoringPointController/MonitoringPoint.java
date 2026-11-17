package MonitoringPointController;

import java.util.Locale;

/**
 * One monitoring station, the {@code MonitoringPoint} of the domain model.
 *
 * <p>A point is the smallest unit the department configures: it names a station, says which
 * district the station belongs to and gives the coordinates that the resident park search uses as
 * the centre of that district. Districts themselves are not stored anywhere - they exist as long
 * as at least one active point mentions them.</p>
 *
 * <p>Immutable: every change goes through the service, which builds a new instance and lets
 * {@link MonitoringPointRepository} store it.</p>
 *
 * @param id        primary key in monitoring_points.json, for example {@code MP004}; blank for a point that is not stored yet
 * @param name      station name shown in the tables, for example {@code Klang Station}
 * @param district  district the station belongs to, for example {@code Klang}
 * @param latitude  WGS84 latitude in degrees, -90 to 90
 * @param longitude WGS84 longitude in degrees, -180 to 180
 * @param active    {@code false} once the point is retired; inactive points drop out of the district list
 */
public record MonitoringPoint(String id,
                              String name,
                              String district,
                              double latitude,
                              double longitude,
                              boolean active) {

    /** Trims the text fields so a stored point is always written back without stray spaces. */
    public MonitoringPoint {
        id = id == null ? "" : id.trim();
        name = name == null ? "" : name.trim();
        district = district == null ? "" : district.trim();
    }

    /** Coordinates as shown in the administrator table, for example {@code 3.0449, 101.4455}. */
    public String coordinates() {
        return String.format(Locale.ROOT, "%.4f, %.4f", latitude, longitude);
    }

    /** True when the point carries real coordinates; a hand-edited row may leave both at 0. */
    public boolean hasCoordinates() {
        return latitude != 0 || longitude != 0;
    }
}
