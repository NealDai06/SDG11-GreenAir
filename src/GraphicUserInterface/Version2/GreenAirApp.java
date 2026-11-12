package greenair.app;

import greenair.api.*;
import greenair.data.*;
import greenair.view.MainFrame;

import javax.swing.*;
import java.nio.file.Path;

/** Application entry point: prepares the data folder, chooses the AQI provider and opens the window. */
public class GreenAirApp {
    /** Set to true once OpenMeteoAirQualityProvider's parse methods are implemented. */
    private static final boolean USE_LIVE_API = false;

    public static void main(String[] args) {
        Path dir = Path.of(System.getProperty("greenair.data", "data")).toAbsolutePath();
        DataSeeder.seedIfMissing(dir);
        System.out.println("GreenAir data folder: " + dir);

        UserStore users = new UserStore(dir.resolve("users.json"));
        MonitoringPointStore points = new MonitoringPointStore(dir.resolve("monitoring_points.json"));
        ReportStore reports = new ReportStore(dir.resolve("reports.json"));
        AlertStore alerts = new AlertStore(dir.resolve("alerts.json"));
        ParkStore parks = new ParkStore(dir.resolve("parks.json"));

        FileAirQualityProvider fileProvider = new FileAirQualityProvider(dir.resolve("air_history.json"));
        AirQualityProvider air = USE_LIVE_API
                ? new CachingAirQualityProvider(new OpenMeteoAirQualityProvider(
                        district -> points.centre(district).orElse(null)), fileProvider)
                : fileProvider;

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            new AppController(frame, users, points, reports, alerts, parks, air);
            frame.setVisible(true);
        });
    }
}
