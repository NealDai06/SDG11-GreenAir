package MonitoringPointController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service layer of the monitoring point feature, the {@code MonitoringPointService} of
 * Figure 3.0.
 *
 * <p>It owns the rules the administrator dashboard relies on: a point needs a name and a district,
 * the name stays unique, the coordinates are inside the valid range, an id that no longer exists
 * cannot be edited or deleted, and only active points take part in the district list. Persistence
 * is delegated to {@link MonitoringPointRepository}, so this class never touches a file.</p>
 */
public class MonitoringPointService {

    /**
     * Thrown when a request cannot be carried out. The message is written for the end user, so the
     * controller or the view can show it as it is instead of a stack trace.
     */
    public static class MonitoringPointRequestException extends RuntimeException {
        public MonitoringPointRequestException(String message) {
            super(message);
        }
    }

    static final int NAME_MAX_LENGTH = 60;
    static final int DISTRICT_MAX_LENGTH = 60;

    private final MonitoringPointRepository repository;

    public MonitoringPointService(MonitoringPointRepository repository) {
        this.repository = repository;
    }

    public MonitoringPointRepository repository() {
        return repository;
    }

    /** Every point, active or not, in file order. */
    public List<MonitoringPoint> findAll() {
        return repository.findAll();
    }

    /** Sorted district names that currently have at least one active point. */
    public List<String> activeDistricts() {
        return repository.findActiveDistricts();
    }

    /** Centre of a district, used by the park recommendation search. */
    public Optional<double[]> districtCentre(String district) {
        if (district == null || district.isBlank()) return Optional.empty();
        return repository.findDistrictCentre(district.trim());
    }

    /** @throws MonitoringPointRequestException when no point carries that id any more */
    public MonitoringPoint requirePoint(String id) {
        if (id == null || id.isBlank()) throw new MonitoringPointRequestException("Select a monitoring point first.");
        return repository.findById(id)
                .orElseThrow(() -> new MonitoringPointRequestException(
                        "Monitoring point " + id.trim() + " no longer exists."));
    }

    /**
     * Creates a point when {@code id} is blank, otherwise updates the point with that id.
     *
     * @return the stored point, with the id assigned to a new one
     * @throws MonitoringPointRequestException when a rule below is broken
     */
    public MonitoringPoint save(String id, String name, String district,
                                double latitude, double longitude, boolean active) {
        String cleanId = id == null ? "" : id.trim();
        String cleanName = validateText(name, "A monitoring point name is required.", NAME_MAX_LENGTH,
                "A monitoring point name must be at most " + NAME_MAX_LENGTH + " characters.");
        String cleanDistrict = validateText(district, "A district is required.", DISTRICT_MAX_LENGTH,
                "A district must be at most " + DISTRICT_MAX_LENGTH + " characters.");

        if (!cleanId.isEmpty()) {
            requirePoint(cleanId); // an edited point has to exist before it can be changed
        }
        assertNameAvailable(cleanName, cleanId);
        assertCoordinates(latitude, longitude);

        MonitoringPoint draft = new MonitoringPoint(cleanId, cleanName, cleanDistrict, latitude, longitude, active);
        return repository.save(draft);
    }

    /** Activates or deactivates a point without touching its other fields. */
    public MonitoringPoint setActive(String id, boolean active) {
        MonitoringPoint point = requirePoint(id);
        MonitoringPoint updated = new MonitoringPoint(point.id(), point.name(), point.district(),
                point.latitude(), point.longitude(), active);
        return repository.save(updated);
    }

    /** Removes a point from monitoring_points.json. */
    public void delete(String id) {
        MonitoringPoint point = requirePoint(id);
        if (!repository.deleteById(point.id())) {
            throw new MonitoringPointRequestException(
                    "Monitoring point " + point.id() + " no longer exists.");
        }
    }

    // ---------------------------------------------------------------- validation

    private static String validateText(String value, String requiredMessage, int maxLength, String tooLongMessage) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) throw new MonitoringPointRequestException(requiredMessage);
        if (clean.length() > maxLength) throw new MonitoringPointRequestException(tooLongMessage);
        return clean;
    }

    /** A name may only be used by one point, so the tables of the administrator stay unambiguous. */
    private void assertNameAvailable(String name, String editingId) {
        for (MonitoringPoint point : repository.findAll()) {
            if (point.id().equals(editingId)) continue;
            if (point.name().equalsIgnoreCase(name)) {
                throw new MonitoringPointRequestException(
                        "A monitoring point named \"" + name + "\" already exists.");
            }
        }
    }

    private static void assertCoordinates(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new MonitoringPointRequestException("Latitude must be between -90 and 90.");
        }
        if (longitude < -180 || longitude > 180) {
            throw new MonitoringPointRequestException("Longitude must be between -180 and 180.");
        }
    }

    /** Districts of the active points, ready for a combo box model. */
    public List<String> districtsForSelection() {
        return new ArrayList<>(activeDistricts());
    }
}
