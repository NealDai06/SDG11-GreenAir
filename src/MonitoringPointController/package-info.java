/**
 * Monitoring point feature of GreenAir Klang Valley: the monitoring stations and the districts
 * they define (Figure 3.0 of the report).
 *
 * <p>The package follows the layered design of the system and holds the whole vertical slice:</p>
 *
 * <ul>
 *   <li>{@link MonitoringPointController.MonitoringPointController} - Controller layer, called by
 *       {@code GraphicUserInterface}; the configuration functions are restricted to
 *       {@code UserController.UserRole#ADMINISTRATOR}.</li>
 *   <li>{@link MonitoringPointController.MonitoringPointService} - Service layer: validation and
 *       the rules of the use cases.</li>
 *   <li>{@link MonitoringPointController.MonitoringPointRepository} - Repository layer: reads and
 *       writes {@code monitoring_points.json}.</li>
 *   <li>{@link MonitoringPointController.MonitoringPoint} - Domain model.</li>
 * </ul>
 *
 * <p>Districts are not a separate entity: they exist as long as an active point mentions them.
 * {@code activeDistricts()} and {@code districtCentre(String)} are therefore open to every role,
 * while saving and deleting a point needs an administrator.</p>
 */
package MonitoringPointController;
