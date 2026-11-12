package greenair.app;

import greenair.api.AirQualityProvider;
import greenair.api.AirQualityReading;
import greenair.data.*;
import greenair.data.AlertStore.Alert;
import greenair.data.MonitoringPointStore.Point;
import greenair.data.ParkStore.Park;
import greenair.data.ReportStore.Report;
import greenair.data.UserStore.User;
import greenair.view.*;

import javax.swing.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Connects the views to the JSON files and the air quality provider. */
public class AppController {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MainFrame frame;
    private final UserStore users;
    private final MonitoringPointStore points;
    private final ReportStore reports;
    private final AlertStore alerts;
    private final ParkStore parks;
    private final AirQualityProvider air;

    private final LoginView login;
    private final RegisterView register;
    private final ResidentView resident;
    private final OfficerView officer;
    private final AdminView admin;

    private User current;

    public AppController(MainFrame frame, UserStore users, MonitoringPointStore points, ReportStore reports,
                         AlertStore alerts, ParkStore parks, AirQualityProvider air) {
        this.frame = frame;
        this.users = users;
        this.points = points;
        this.reports = reports;
        this.alerts = alerts;
        this.parks = parks;
        this.air = air;
        this.login = frame.getLoginView();
        this.register = frame.getRegisterView();
        this.resident = frame.getResidentView();
        this.officer = frame.getOfficerView();
        this.admin = frame.getAdminView();
        wireAuth();
        wireResident();
        wireOfficer();
        wireAdmin();
        safely(this::refreshDistricts);
    }

    private void safely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(), "Data file problem", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String now() { return LocalDateTime.now().format(TS); }

    private void refreshDistricts() {
        List<String> districts = points.districts();
        register.setDistricts(districts);
        resident.setDistricts(districts);
    }

    // ---------------- authentication ----------------

    private void wireAuth() {
        login.setOnLogin(username -> {
            try {
                Optional<User> found = users.findByUsername(username);
                if (found.isEmpty()) {
                    login.showError("No account found for \"" + username + "\".");
                    return;
                }
                User u = found.get();
                if (!"Active".equalsIgnoreCase(u.status())) {
                    login.showError("This account is deactivated. Contact an administrator.");
                    return;
                }
                current = u;
                login.reset();
                openDashboard(u);
            } catch (RuntimeException ex) {
                login.showError("Could not read users file: " + ex.getMessage());
            }
        });
        login.setOnRegister(() -> { safely(this::refreshDistricts); frame.show(MainFrame.REGISTER); });
        register.setOnBack(() -> frame.show(MainFrame.LOGIN));
        register.setOnSubmit(form -> safely(() -> {
            try {
                users.add(form.username(), form.fullName(), UserStore.RESIDENT, form.groupType(), form.subscribedDistricts());
            } catch (IllegalArgumentException ex) {
                register.showError(ex.getMessage());
                return;
            }
            register.showMessage("Account created. You can now log in as \"" + form.username() + "\".");
            register.reset();
            frame.show(MainFrame.LOGIN);
        }));
        Runnable logout = () -> { current = null; login.reset(); frame.show(MainFrame.LOGIN); };
        resident.setOnLogout(logout);
        officer.setOnLogout(logout);
        admin.setOnLogout(logout);
    }

    private void openDashboard(User u) {
        refreshDistricts();
        switch (u.role()) {
            case UserStore.RESIDENT -> {
                resident.setUserInfo(u.fullName(), u.groupType());
                resident.setSubscribedDistricts(u.districts());
                resident.setAlerts(residentAlertRows());
                frame.show(MainFrame.RESIDENT);
            }
            case UserStore.OFFICER -> {
                officer.setUserInfo(u.fullName());
                refreshReports();
                officer.setAlerts(alertRows(alerts.all()));
                officer.setOverview(overviewRows());
                frame.show(MainFrame.OFFICER);
            }
            case UserStore.ADMIN -> {
                admin.setUserInfo(u.fullName());
                refreshUsers();
                refreshPoints();
                frame.show(MainFrame.ADMIN);
            }
            default -> login.showError("Unknown role \"" + u.role() + "\" in users file.");
        }
    }

    // ---------------- resident ----------------

    private void wireResident() {
        resident.setOnFetchAirQuality(d -> safely(() -> {
            AirQualityReading r;
            try {
                r = air.getCurrent(d);
            } catch (IOException ex) {
                resident.showMessage("No air quality data available for " + d + ".\n" + ex.getMessage());
                return;
            }
            resident.showCurrentReading(d, r.pm25(), r.pm10(), r.usAqi(), r.time());
            try {
                List<Object[]> rows = new ArrayList<>();
                for (AirQualityReading f : air.getForecast(d)) rows.add(new Object[]{f.time(), f.pm25(), f.pm10(), f.usAqi()});
                resident.setForecast(rows);
            } catch (IOException ex) {
                resident.setForecast(List.of());
            }
            raiseAlertIfNeeded(r);
            resident.setAlerts(residentAlertRows());
        }));

        resident.setOnFindParks((district, km) -> safely(() -> {
            Optional<double[]> centre = points.centre(district);
            if (centre.isEmpty()) {
                resident.showMessage("No monitoring point is configured for " + district + ".");
                return;
            }
            List<Object[]> rows = new ArrayList<>();
            List<Object[]> sortable = new ArrayList<>();
            for (Park p : parks.all()) {
                double dist = distanceKm(centre.get()[0], centre.get()[1], p.latitude(), p.longitude());
                if (dist > km) continue;
                Object aqi = "n/a";
                String suitability = "No air quality data";
                try {
                    int a = air.getCurrent(p.district()).usAqi();
                    aqi = a;
                    suitability = a <= 50 ? "Good for outdoor activity" : a <= 100 ? "Moderate - take breaks"
                            : a <= 150 ? "Sensitive groups should limit time" : "Avoid prolonged outdoor activity";
                } catch (IOException ignored) { }
                sortable.add(new Object[]{p.name(), p.district(), Math.round(dist * 10) / 10.0, aqi, suitability});
            }
            sortable.sort(Comparator.<Object[], Integer>comparing(r -> r[3] instanceof Integer i ? i : Integer.MAX_VALUE)
                    .thenComparing(r -> (Double) r[2]));
            rows.addAll(sortable);
            resident.setParks(rows);
            if (rows.isEmpty()) resident.showMessage("No parks found within " + km + " km of " + district + ".");
        }));

        resident.setOnGetAdvice((group, district) -> safely(() -> resident.showAdvice(buildAdvice(group, district))));

        resident.setOnSubmitReport(form -> safely(() -> {
            reports.add(now(), form.type(), form.district(), current == null ? "Unknown" : current.fullName(),
                    form.location(), form.description());
            resident.showMessage("Thank you. Your report was saved and is visible to environment officers.");
            resident.clearReportForm();
        }));

        resident.setOnRefreshAlerts(() -> safely(() -> resident.setAlerts(residentAlertRows())));
    }

    private void raiseAlertIfNeeded(AirQualityReading r) {
        if (r.usAqi() <= 100) return;
        String level = Theme.aqiCategory(r.usAqi());
        alerts.addIfNew(new Alert(now(), r.district(), "US AQI", level,
                "US AQI " + r.usAqi() + " (" + level + ") in " + r.district() + "."));
    }

    private List<Object[]> residentAlertRows() {
        if (current == null) return List.of();
        List<Alert> mine = alerts.all().stream()
                .filter(a -> current.districts().stream().anyMatch(d -> d.equalsIgnoreCase(a.district()))).toList();
        return alertRows(mine);
    }

    private static List<Object[]> alertRows(List<Alert> list) {
        List<Object[]> rows = new ArrayList<>();
        for (Alert a : list) rows.add(new Object[]{a.time(), a.district(), a.pollutant(), a.level(), a.message()});
        return rows;
    }

    private String buildAdvice(String group, String district) {
        int aqi;
        try {
            aqi = air.getCurrent(district).usAqi();
        } catch (IOException ex) {
            return "No air quality reading is available for " + district + " right now, so no advice can be given.";
        }
        String level = Theme.aqiCategory(aqi);
        String head = "Current US AQI in " + district + ": " + aqi + " (" + level + ")\n\n";
        boolean good = aqi <= 50, moderate = aqi <= 100, sensitive = aqi <= 150;
        return head + switch (group) {
            case "Children" -> good ? "Air is clean. Outdoor play and sports are fine."
                    : moderate ? "Outdoor play is fine, but watch for coughing or breathing discomfort and take breaks."
                    : sensitive ? "Keep outdoor play short and light. Prefer indoor activities, especially for children with asthma."
                    : "Keep children indoors. Avoid outdoor sports and wear a well-fitted mask if going out is unavoidable.";
            case "Elderly" -> good ? "Air is clean. Enjoy walks and gentle exercise outdoors."
                    : moderate ? "Light outdoor activity is fine. Rest often and carry any prescribed medication."
                    : sensitive ? "Limit time outdoors and avoid strenuous activity. Choose cleaner times of day or stay indoors."
                    : "Stay indoors with windows closed. Seek medical advice if you feel short of breath or dizzy.";
            default -> good ? "Air is clean. All outdoor activities are fine."
                    : moderate ? "Outdoor activity is fine. Unusually sensitive people should reduce long, heavy exertion."
                    : sensitive ? "Reduce prolonged or heavy exertion outdoors. People with respiratory conditions should take extra care."
                    : "Avoid prolonged outdoor activity. Wear a mask outdoors and keep windows closed.";
        };
    }

    /** Great-circle distance in kilometres. */
    private static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ---------------- officer ----------------

    private void wireOfficer() {
        officer.setOnRefreshReports(() -> safely(this::refreshReports));
        officer.setOnRefreshAlerts(() -> safely(() -> officer.setAlerts(alertRows(alerts.all()))));
        officer.setOnRefreshOverview(() -> safely(() -> officer.setOverview(overviewRows())));
        officer.setOnUpdateStatus((id, status) -> safely(() -> {
            reports.updateStatus(id, status);
            refreshReports();
        }));
    }

    private void refreshReports() {
        List<Object[]> rows = new ArrayList<>();
        List<String> details = new ArrayList<>();
        for (Report r : reports.all()) {
            rows.add(new Object[]{r.id(), r.submitted(), r.type(), r.district(), r.reporter(), r.status()});
            details.add((r.location().isBlank() ? "" : "Location: " + r.location() + "\n\n") + r.description());
        }
        officer.setReports(rows, details);
    }

    private List<Object[]> overviewRows() {
        List<Object[]> rows = new ArrayList<>();
        for (String d : points.districts()) {
            try {
                AirQualityReading r = air.getCurrent(d);
                rows.add(new Object[]{d, r.pm25(), r.pm10(), r.usAqi(), Theme.aqiCategory(r.usAqi())});
            } catch (IOException ex) {
                rows.add(new Object[]{d, "-", "-", "-", "No data"});
            }
        }
        return rows;
    }

    // ---------------- administrator ----------------

    private void wireAdmin() {
        admin.setOnRefreshUsers(() -> safely(this::refreshUsers));
        admin.setOnChangeRole((id, role) -> safely(() -> {
            if (current != null && id.equals(current.id()) && !UserStore.ADMIN.equals(role)) {
                admin.showMessage("You cannot remove your own administrator role.");
                return;
            }
            users.updateRole(id, role);
            refreshUsers();
        }));
        admin.setOnSetUserActive((id, active) -> safely(() -> {
            if (current != null && id.equals(current.id()) && !active) {
                admin.showMessage("You cannot deactivate the account you are logged in with.");
                return;
            }
            users.setStatus(id, active ? "Active" : "Inactive");
            refreshUsers();
        }));
        admin.setOnDeleteUser(id -> safely(() -> {
            if (current != null && id.equals(current.id())) {
                admin.showMessage("You cannot delete the account you are logged in with.");
                return;
            }
            users.delete(id);
            refreshUsers();
        }));
        admin.setOnRefreshPoints(() -> safely(this::refreshPoints));
        admin.setOnSavePoint(f -> safely(() -> {
            points.save(new Point(f.id(), f.name(), f.district(), f.latitude(), f.longitude(), f.active()));
            refreshPoints();
            refreshDistricts();
            admin.clearPointForm();
            admin.showMessage("Monitoring point saved.");
        }));
        admin.setOnDeletePoint(id -> safely(() -> {
            points.delete(id);
            refreshPoints();
            refreshDistricts();
            admin.clearPointForm();
        }));
    }

    private void refreshUsers() {
        List<Object[]> rows = new ArrayList<>();
        for (User u : users.all()) rows.add(new Object[]{u.id(), u.fullName(), u.username(), u.role(), u.status()});
        admin.setUsers(rows);
    }

    private void refreshPoints() {
        List<Object[]> rows = new ArrayList<>();
        for (Point p : points.all()) rows.add(new Object[]{p.id(), p.name(), p.district(), p.latitude(), p.longitude(), p.active()});
        admin.setMonitoringPoints(rows);
    }
}
