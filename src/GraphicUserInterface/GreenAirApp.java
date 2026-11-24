package GraphicUserInterface;


import AirQualityController.AirQualityController;
import AirQualityController.AirQualityRepository;
import AirQualityController.AirQualityService;
import AlertController.AlertController;
import AlertController.AlertRepository;
import AlertController.PollutionAlertService;
import AirQualityController.OpenMeteoClient;
import UserController.UserProfile;
import UserController.UserRole;
import UserController.GroupType;
import java.nio.file.Files;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import MonitoringPointController.MonitoringPointController;
import MonitoringPointController.MonitoringPointRepository;
import MonitoringPointController.MonitoringPointService;
import MonitoringPointController.MonitoringPoint;
import RecommendationController.AdviceService;
import RecommendationController.ParkRecommendationService;
import RecommendationController.ParkRepository;
import RecommendationController.RecommendationController;
import ReportController.ReportController;
import ReportController.ReportRepository;
import ReportController.ReportService;
import UserController.ProfileRepository;
import UserController.UserController;
import UserController.UserService;


import javax.swing.*;
import java.nio.file.Path;

/**
 * Application entry point and composition root: creates the data folder, builds every repository,
 * service and controller, and opens the window.
 */
public class GreenAirApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> Theme.guard(null, () -> createFrame().setVisible(true)));
    }

    /** Builds the existing controllers and views around one configured data directory. */
    static MainFrame createFrame() throws IOException {
        Path dir = Path.of(System.getProperty("greenair.data", "data")).toAbsolutePath();
        initializeData(dir);
        System.out.println("GreenAir data folder: " + dir);

        // Users
        UserController userController = new UserController(new UserService(new ProfileRepository(dir.resolve("users.json"))));

        // Monitoring points
        MonitoringPointService monitoringService = new MonitoringPointService(new MonitoringPointRepository(dir.resolve("monitoring_points.json")));
        MonitoringPointController monitoringController = new MonitoringPointController(monitoringService);

        // Alerts (Observer pattern)
        PollutionAlertService alertService = new PollutionAlertService(new AlertRepository());
        AlertController alertController = new AlertController(alertService);

        // Air quality
        AirQualityService airQualityService = new AirQualityService(new OpenMeteoClient(),
                new AirQualityRepository(dir.resolve("air_history.json")), alertService, monitoringService);
        AirQualityController airQualityController = new AirQualityController(airQualityService);

        // Recommendations (Strategy pattern)
        ParkRecommendationService parkService = new ParkRecommendationService(
                new ParkRepository(dir.resolve("parks.json"), monitoringService), airQualityService);
        RecommendationController recommendationController =
                new RecommendationController(new AdviceService(), parkService);

        // Reports
        ReportController reportController = new ReportController(new ReportService(new ReportRepository(dir.resolve("reports.json"))));

        MainFrame.Controllers controllers = new MainFrame.Controllers(userController, monitoringController,
                airQualityController, recommendationController, alertController, reportController);
        return new MainFrame(controllers);
    }

    private static void initializeData(Path dir) throws IOException {
        Files.createDirectories(dir);
        if (!Files.exists(dir.resolve("users.json"))) {
            ProfileRepository users = new ProfileRepository(dir.resolve("users.json"));
            users.save(new UserProfile("U001", "admin", "System Administrator", UserRole.ADMINISTRATOR,
                    GroupType.GENERAL_PUBLIC, true, List.of()));
            users.save(new UserProfile("U002", "officer", "Environment Officer", UserRole.ENVIRONMENT_OFFICER,
                    GroupType.GENERAL_PUBLIC, true, List.of()));
            users.save(new UserProfile("U003", "resident", "Sample Resident", UserRole.RESIDENT,
                    GroupType.GENERAL_PUBLIC, true, List.of("Petaling Jaya", "Shah Alam")));
        }
        if (!Files.exists(dir.resolve("monitoring_points.json"))) seedMonitoringPoints(dir);
        if (!Files.exists(dir.resolve("parks.json"))) seedParks(dir);
    }

    private static void seedMonitoringPoints(Path dir) {
        String[][] districts = {
                {"Kuala Lumpur", "3.1390", "101.6869"}, {"Petaling Jaya", "3.1073", "101.6067"},
                {"Shah Alam", "3.0733", "101.5185"}, {"Klang", "3.0449", "101.4455"},
                {"Subang Jaya", "3.0565", "101.5851"}, {"Kajang", "2.9927", "101.7909"},
                {"Ampang", "3.1500", "101.7600"}, {"Cheras", "3.1000", "101.7400"},
                {"Puchong", "3.0333", "101.6167"}, {"Rawang", "3.3217", "101.5767"}};
        MonitoringPointRepository points = new MonitoringPointRepository(dir.resolve("monitoring_points.json"));
        for (int i = 0; i < districts.length; i++) {
            String[] district = districts[i];
            points.save(new MonitoringPoint(String.format(Locale.ROOT, "MP%03d", i + 1),
                    district[0] + " Station", district[0], Double.parseDouble(district[1]),
                    Double.parseDouble(district[2]), true));
        }
    }

    private static void seedParks(Path dir) throws IOException {
        Files.writeString(dir.resolve("parks.json"), """
                [
                  {"parkId":"P001","name":"Taman Tugu","latitude":3.1523,"longitude":101.6872,"districtId":"Kuala Lumpur"},
                  {"parkId":"P002","name":"Taman Tasik Perdana","latitude":3.1430,"longitude":101.6860,"districtId":"Kuala Lumpur"},
                  {"parkId":"P003","name":"Taman Tasik Titiwangsa","latitude":3.1760,"longitude":101.7060,"districtId":"Kuala Lumpur"},
                  {"parkId":"P004","name":"Taman Jaya","latitude":3.1180,"longitude":101.6440,"districtId":"Petaling Jaya"},
                  {"parkId":"P005","name":"Taman Tasik Shah Alam","latitude":3.0730,"longitude":101.5260,"districtId":"Shah Alam"},
                  {"parkId":"P006","name":"Taman Awam Subang Jaya","latitude":3.0600,"longitude":101.5900,"districtId":"Subang Jaya"},
                  {"parkId":"P007","name":"Taman Rekreasi Ampang","latitude":3.1450,"longitude":101.7600,"districtId":"Ampang"},
                  {"parkId":"P008","name":"Taman Cheras Hartamas","latitude":3.1050,"longitude":101.7450,"districtId":"Cheras"},
                  {"parkId":"P009","name":"Taman Rekreasi Puchong","latitude":3.0300,"longitude":101.6200,"districtId":"Puchong"},
                  {"parkId":"P010","name":"Taman Rekreasi Klang","latitude":3.0500,"longitude":101.4500,"districtId":"Klang"}
                ]
                """);
    }
}
