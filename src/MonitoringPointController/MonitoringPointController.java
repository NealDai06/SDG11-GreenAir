package MonitoringPointController;

import UserController.UserRole;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Controller layer of the monitoring point feature, the {@code MonitoringPointController} of
 * Figure 3.0.
 *
 * <p>It is the mediator between {@code GraphicUserInterface} (the "Monitoring points" tab of
 * AdminView, the district combo boxes of RegisterView and ResidentView) and
 * {@link MonitoringPointService}. Reading the district list and the district centre is open to
 * every role, because registration and the park search need it; creating, editing, deactivating
 * and deleting a point is reserved to administrators.</p>
 *
 * <p>The signed-in role is passed in by the caller, normally
 * {@code userController.currentUser().role()}, so this controller never has to own the session
 * and can be unit tested without the GUI.</p>
 */
public class MonitoringPointController {

    private final MonitoringPointService service;

    public MonitoringPointController(MonitoringPointService service) {
        this.service = service;
    }

    /** Controller over the given monitoring_points.json file. */
    public MonitoringPointController(MonitoringPointRepository repository) {
        this(new MonitoringPointService(repository));
    }

    /** Controller over the monitoring_points.json file of the running application. */
    public MonitoringPointController() {
        this(new MonitoringPointRepository());
    }

    public Path pointFile() {
        return service.repository().file();
    }

    // ---------------------------------------------------------------- everyone

    /**
     * Districts that currently have an active monitoring point. The registration form offers them
     * as alert subscriptions and the resident screens use them for advice and park search.
     */
    public List<String> activeDistricts() {
        return service.activeDistricts();
    }

    /** Centre of a district, so the distance to each park can be computed. */
    public Optional<double[]> districtCentre(String district) {
        return service.districtCentre(district);
    }

    // ---------------------------------------------------------------- administrator functions

    /** Every point, as listed on the "Monitoring points" tab of the administrator dashboard. */
    public List<MonitoringPoint> listPoints(UserRole actor) {
        requireAdministrator(actor, "see the monitoring points");
        return service.findAll();
    }

    /**
     * Saves the point typed into the administrator form: creates a new one when {@code id} is
     * blank, otherwise updates the row with that id.
     *
     * @return the stored point, with the id assigned to a new one
     * @throws MonitoringPointService.MonitoringPointRequestException when a rule is broken or the
     *                                                                caller is not an administrator
     */
    public MonitoringPoint save(UserRole actor, String id, String name, String district,
                                double latitude, double longitude, boolean active) {
        requireAdministrator(actor, "change monitoring points");
        return service.save(id, name, district, latitude, longitude, active);
    }

    /** Same as {@link #save} for a form that already built the record. */
    public MonitoringPoint save(UserRole actor, MonitoringPoint point) {
        requireAdministrator(actor, "change monitoring points");
        if (point == null) {
            throw new MonitoringPointService.MonitoringPointRequestException(
                    "No monitoring point details were supplied.");
        }
        return service.save(point.id(), point.name(), point.district(),
                point.latitude(), point.longitude(), point.active());
    }

    /** Activates or deactivates a point, for example when a station is taken out of service. */
    public MonitoringPoint setActive(UserRole actor, String id, boolean active) {
        requireAdministrator(actor, "change monitoring points");
        return service.setActive(id, active);
    }

    /** Deletes a monitoring point. */
    public void delete(UserRole actor, String id) {
        requireAdministrator(actor, "delete monitoring points");
        service.delete(id);
    }

    // ---------------------------------------------------------------- access control

    private static void requireAdministrator(UserRole actor, String action) {
        if (actor == null) {
            throw new MonitoringPointService.MonitoringPointRequestException(
                    "Sign in as an administrator first.");
        }
        if (!actor.isAdministrator()) {
            throw new MonitoringPointService.MonitoringPointRequestException(
                    "Only administrators may " + action + ".");
        }
    }
}
