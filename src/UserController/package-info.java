/**
 * User account feature of GreenAir Klang Valley: registration, log-in, profile editing and the
 * account administration of the administrator dashboard (Figure 3.0 of the report).
 *
 * <p>The package follows the layered design of the system and holds the whole vertical slice:</p>
 *
 * <ul>
 *   <li>{@link UserController.UserController} - Controller layer, called by
 *       {@code GraphicUserInterface}.</li>
 *   <li>{@link UserController.UserService} - Service layer: validation and the business rules of
 *       the use cases.</li>
 *   <li>{@link UserController.ProfileRepository} - Repository layer: reads and writes
 *       {@code users.json}.</li>
 *   <li>{@link UserController.UserProfile}, {@link UserController.UserRole},
 *       {@link UserController.GroupType} - Domain model shared with the other features.</li>
 * </ul>
 *
 * <p>The controller and the service import nothing from {@code java.awt} or
 * {@code javax.swing}, so the feature can be tested head-lessly with JUnit.</p>
 */
package UserController;
