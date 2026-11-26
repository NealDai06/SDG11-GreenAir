package MonitoringPointController;

import UserController.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests of {@link MonitoringPointController}. Every test works on its own temporary
 * monitoring_points.json, so the real data file is never touched and no GUI is needed.
 */
class MonitoringPointControllerTest {

    private static final UserRole ADMIN = UserRole.ADMINISTRATOR;
    private static final double DELTA = 1e-9;

    @TempDir
    Path tempDir;

    private MonitoringPointRepository repository;
    private MonitoringPointController controller;

    @BeforeEach
    void setUp() {
        repository = new MonitoringPointRepository(tempDir.resolve("monitoring_points.json"));
        controller = new MonitoringPointController(repository);
        controller.save(ADMIN, "", "Klang Station", "Klang", 3.0449, 101.4455, true);               // MP001
        controller.save(ADMIN, "", "Shah Alam Station", "Shah Alam", 3.0733, 101.5185, true);       // MP002
        controller.save(ADMIN, "", "Petaling Jaya Station", "Petaling Jaya", 3.1073, 101.6067, false); // MP003
    }

    @Test
    void save_administratorNewPoint_assignsNextIdAndPersists() {
        MonitoringPoint stored = controller.save(ADMIN, "", "  Kajang Station ", "Kajang", 2.9935, 101.7874, true);

        assertAll(
                () -> assertEquals("MP004", stored.id()),
                () -> assertEquals("Kajang Station", stored.name()),
                () -> assertEquals("Kajang", stored.district()),
                () -> assertTrue(repository.findById("MP004").isPresent()),
                () -> assertEquals(4, controller.getAllPoints().size())
        );
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"RESIDENT", "ENVIRONMENT_OFFICER"})
    void save_callerIsNotAdministrator_throwsRequestExceptionAndNothingSaved(UserRole actor) {
        MonitoringPointService.MonitoringPointRequestException error =
                assertThrows(MonitoringPointService.MonitoringPointRequestException.class,
                        () -> controller.save(actor, "", "Rawang Station", "Rawang", 3.4, 101.5, true));

        assertAll(
                () -> assertEquals("Only administrators may change monitoring points.", error.getMessage()),
                () -> assertEquals(3, controller.getAllPoints().size())
        );
    }

    @ParameterizedTest
    @CsvSource({
            "91.0,   101.0,  Latitude must be between -90 and 90.",
            "-91.0,  101.0,  Latitude must be between -90 and 90.",
            "3.0,    181.0,  Longitude must be between -180 and 180.",
            "3.0,    -181.0, Longitude must be between -180 and 180."
    })
    void save_coordinatesOutOfRange_throwsRequestExceptionWithRangeMessage(
            double latitude, double longitude, String expectedMessage) {
        MonitoringPointService.MonitoringPointRequestException error =
                assertThrows(MonitoringPointService.MonitoringPointRequestException.class,
                        () -> controller.save(ADMIN, "", "Bad Station", "Klang", latitude, longitude, true));

        assertEquals(expectedMessage, error.getMessage());
    }

    @Test
    void activeDistricts_pointReactivated_includesDistrictAfterSetActive() {
        List<String> before = controller.activeDistricts();

        controller.setActive(ADMIN, "MP003", true);
        List<String> after = controller.activeDistricts();

        assertAll(
                () -> assertEquals(List.of("Klang", "Shah Alam"), before),
                () -> assertEquals(List.of("Klang", "Petaling Jaya", "Shah Alam"), after)
        );
    }

    @Test
    void districtCentre_districtWithTwoPoints_returnsAverageOfCoordinates() {
        controller.save(ADMIN, "", "Port Klang Station", "Klang", 3.0, 101.3, true);

        Optional<double[]> centre = controller.districtCentre("klang");
        Optional<double[]> unknown = controller.districtCentre("Atlantis");

        assertAll(
                () -> assertTrue(centre.isPresent()),
                () -> assertEquals(3.02245, centre.orElseThrow()[0], DELTA),
                () -> assertEquals(101.37275, centre.orElseThrow()[1], DELTA),
                () -> assertTrue(unknown.isEmpty())
        );
    }
}
