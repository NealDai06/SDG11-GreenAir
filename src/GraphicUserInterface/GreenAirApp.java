package greenair.view;

import javax.swing.*;
import java.util.List;

/**
 * Demo launcher for previewing the view layer on its own.
 * It uses hard-coded sample data in place of the real controllers, which are not part of this layer.
 * Demo logins: resident@demo.my, officer@demo.my, admin@demo.my (any password).
 */
public class GreenAirApp {
    private static final List<String> DISTRICTS = List.of("Kuala Lumpur", "Petaling Jaya", "Shah Alam",
            "Klang", "Subang Jaya", "Kajang", "Ampang", "Cheras", "Puchong", "Rawang");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            wireDemo(frame);
            frame.setVisible(true);
        });
    }

    private static void wireDemo(MainFrame f) {
        LoginView login = f.getLoginView();
        RegisterView register = f.getRegisterView();
        ResidentView resident = f.getResidentView();
        OfficerView officer = f.getOfficerView();
        AdminView admin = f.getAdminView();

        register.setDistricts(DISTRICTS);
        resident.setDistricts(DISTRICTS);

        login.setOnLogin((email, pw) -> {
            if (email.startsWith("resident")) {
                resident.setUserInfo("Aina", "General public");
                resident.setSubscribedDistricts(List.of("Petaling Jaya", "Shah Alam"));
                resident.setAlerts(sampleAlerts());
                f.show(MainFrame.RESIDENT);
            } else if (email.startsWith("officer")) {
                officer.setUserInfo("Officer Lim");
                officer.setReports(sampleReports(), List.of("Smoke seen from a burning rubbish pile behind the market.",
                        "Thick haze near the highway.", "Broken swings and overgrown grass."));
                officer.setAlerts(sampleAlerts());
                officer.setOverview(sampleOverview());
                f.show(MainFrame.OFFICER);
            } else if (email.startsWith("admin")) {
                admin.setUserInfo("Administrator");
                admin.setUsers(List.of(
                        new Object[]{"U001", "Aina", "resident@demo.my", "RESIDENT", "Active"},
                        new Object[]{"U002", "Officer Lim", "officer@demo.my", "ENVIRONMENT_OFFICER", "Active"},
                        new Object[]{"U003", "Admin", "admin@demo.my", "ADMINISTRATOR", "Active"}));
                admin.setMonitoringPoints(List.of(
                        new Object[]{"MP01", "KL Sentral", "Kuala Lumpur", 3.1343, 101.6866, true},
                        new Object[]{"MP02", "PJ Old Town", "Petaling Jaya", 3.0833, 101.6500, true}));
                f.show(MainFrame.ADMIN);
            } else {
                login.showError("Invalid email or password.");
            }
        });
        login.setOnRegister(() -> f.show(MainFrame.REGISTER));
        register.setOnBack(() -> f.show(MainFrame.LOGIN));
        register.setOnSubmit(form -> {
            register.showMessage("Demo only: account for " + form.fullName() + " would be created by UserController.");
            register.reset();
            f.show(MainFrame.LOGIN);
        });

        Runnable logout = () -> { login.reset(); f.show(MainFrame.LOGIN); };
        resident.setOnLogout(logout);
        officer.setOnLogout(logout);
        admin.setOnLogout(logout);

        resident.setOnFetchAirQuality(d -> {
            resident.showCurrentReading(d, 38.4, 55.1, 112, "just now");
            resident.setForecast(List.of(
                    new Object[]{"Today 15:00", 36.2, 52.0, 108},
                    new Object[]{"Today 18:00", 29.8, 44.3, 92},
                    new Object[]{"Tomorrow 09:00", 18.5, 30.2, 62},
                    new Object[]{"Tomorrow 15:00", 11.0, 21.4, 46}));
        });
        resident.setOnFindParks((d, km) -> resident.setParks(List.of(
                new Object[]{"Taman Tugu", "Kuala Lumpur", 2.4, 48, "Good for outdoor activity"},
                new Object[]{"Taman Jaya", "Petaling Jaya", 4.9, 85, "Moderate - take breaks"},
                new Object[]{"Taman Tasik Shah Alam", "Shah Alam", 9.1, 132, "Sensitive groups should limit time"})));
        resident.setOnGetAdvice((g, d) -> resident.showAdvice("Group: " + g + "\nDistrict: " + d
                + "\n\nAir quality is moderate. Light outdoor activity is fine, but take breaks and carry water."));
        resident.setOnSubmitReport(r -> {
            resident.showMessage("Thank you. Your report about " + r.type() + " in " + r.district() + " was received (demo).");
            resident.clearReportForm();
        });
        resident.setOnRefreshAlerts(() -> resident.setAlerts(sampleAlerts()));

        officer.setOnRefreshReports(() -> officer.showMessage("Demo: ReportController would reload reports."));
        officer.setOnRefreshAlerts(() -> officer.setAlerts(sampleAlerts()));
        officer.setOnRefreshOverview(() -> officer.setOverview(sampleOverview()));
        officer.setOnUpdateStatus((id, s) -> officer.showMessage("Demo: report " + id + " marked as " + s + "."));

        admin.setOnSavePoint(p -> admin.showMessage("Demo: would save monitoring point " + p.name() + "."));
        admin.setOnChangeRole((id, r) -> admin.showMessage("Demo: " + id + " would become " + r + "."));
    }

    private static List<Object[]> sampleAlerts() {
        return List.of(
                new Object[]{"2026-11-10 14:05", "Shah Alam", "PM2.5", "Unhealthy", "PM2.5 above threshold in Shah Alam."},
                new Object[]{"2026-11-10 09:40", "Klang", "US AQI", "Unhealthy for sensitive groups", "AQI above 100 in Klang."});
    }

    private static List<Object[]> sampleReports() {
        return List.of(
                new Object[]{"R101", "2026-11-10 08:12", "Open burning", "Klang", "Aina", "Submitted"},
                new Object[]{"R102", "2026-11-09 17:30", "Smog / haze", "Shah Alam", "Farid", "Under review"},
                new Object[]{"R103", "2026-11-08 10:02", "Park maintenance", "Petaling Jaya", "Mei Ling", "Resolved"});
    }

    private static List<Object[]> sampleOverview() {
        return List.of(
                new Object[]{"Kuala Lumpur", 22.1, 35.0, 72, Theme.aqiCategory(72)},
                new Object[]{"Shah Alam", 55.3, 80.2, 148, Theme.aqiCategory(148)},
                new Object[]{"Klang", 61.0, 90.4, 155, Theme.aqiCategory(155)},
                new Object[]{"Petaling Jaya", 14.0, 25.0, 55, Theme.aqiCategory(55)});
    }
}
