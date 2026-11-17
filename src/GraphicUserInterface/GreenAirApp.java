package graphicuserinterface.greenair.graphicuserinterface;


import graphicuserinterface.greenair.airqualitycontroller.AirQualityController;
import graphicuserinterface.greenair.airqualitycontroller.AirQualityRepository;
import graphicuserinterface.greenair.airqualitycontroller.AirQualityService;
import graphicuserinterface.greenair.alertcontroller.AlertController;
import graphicuserinterface.greenair.alertcontroller.AlertRepository;
import graphicuserinterface.greenair.alertcontroller.PollutionAlertService;
import graphicuserinterface.greenair.infrasructure.DataSeeder;
import graphicuserinterface.greenair.infrasructure.OpenMeteoClient;
import graphicuserinterface.greenair.monitoringcontroller.MonitoringPointController;
import graphicuserinterface.greenair.monitoringcontroller.MonitoringPointRepository;
import graphicuserinterface.greenair.monitoringcontroller.MonitoringService;
import graphicuserinterface.greenair.recommendationcontroller.AdviceService;
import graphicuserinterface.greenair.recommendationcontroller.ParkRecommendationService;
import graphicuserinterface.greenair.recommendationcontroller.ParkRepository;
import graphicuserinterface.greenair.recommendationcontroller.RecommendationController;
import graphicuserinterface.greenair.reportcontroller.ReportController;
import graphicuserinterface.greenair.reportcontroller.ReportRepository;
import graphicuserinterface.greenair.reportcontroller.ReportService;
import graphicuserinterface.greenair.usercontroller.ProfileRepository;
import graphicuserinterface.greenair.usercontroller.UserController;
import graphicuserinterface.greenair.usercontroller.UserService;


import javax.swing.*;
import java.nio.file.Path;

/**
 * Application entry point and composition root: creates the data folder, builds every repository,
 * service and controller, and opens the window.
 */
public class GreenAirApp {
    /** Set to true to fetch live data from the Open-Meteo API (stored data is still the fallback). */
    private static final boolean USE_LIVE_API = true;

    public static void main(String[] args) {
        Path dir = Path.of(System.getProperty("greenair.data", "data")).toAbsolutePath();
        DataSeeder.seedIfMissing(dir);
        System.out.println("GreenAir data folder: " + dir);

        // Users
        UserController userController = new UserController(new UserService(new ProfileRepository(dir.resolve("users.json"))));

        // Monitoring points
        MonitoringService monitoringService = new MonitoringService(new MonitoringPointRepository(dir.resolve("monitoring_points.json")));
        MonitoringPointController monitoringController = new MonitoringPointController(monitoringService);

        // Alerts (Observer pattern)
        PollutionAlertService alertService = new PollutionAlertService(new AlertRepository(dir.resolve("alerts.json")), monitoringService);
        AlertController alertController = new AlertController(alertService);

        // Air quality
        AirQualityService airQualityService = new AirQualityService(USE_LIVE_API ? new OpenMeteoClient() : null,
                new AirQualityRepository(dir.resolve("air_history.json")), alertService, monitoringService);
        AirQualityController airQualityController = new AirQualityController(airQualityService);

        // Recommendations (Strategy pattern)
        ParkRecommendationService parkService = new ParkRecommendationService(
                new ParkRepository(dir.resolve("parks.json")), airQualityService, monitoringService);
        RecommendationController recommendationController =
                new RecommendationController(new AdviceService(), parkService, airQualityService);

        // Reports
        ReportController reportController = new ReportController(new ReportService(new ReportRepository(dir.resolve("reports.json"))));

        MainFrame.Controllers controllers = new MainFrame.Controllers(userController, monitoringController,
                airQualityController, recommendationController, alertController, reportController);
        SwingUtilities.invokeLater(() -> new MainFrame(controllers).setVisible(true));
    }
}
