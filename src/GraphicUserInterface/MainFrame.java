package GraphicUserInterface;


import AirQualityController.AirQualityController;
import AlertController.AlertController;
import AlertController.AlertObserver;
import AlertController.PollutionAlert;
import AlertController.OfficerAlertObserver;
import AlertController.ResidentAlertObserver;
import MonitoringPointController.MonitoringPointController;
import MonitoringPointController.MonitoringPoint;
import RecommendationController.RecommendationController;
import ReportController.ReportController;
import UserController.UserController;
import UserController.UserProfile;

import javax.swing.*;
import java.awt.*;

/** Application window. Holds the login session and switches between the role-specific screens. */
public class MainFrame extends JFrame {
    /** The six controllers the views talk to. */
    public record Controllers(UserController user, MonitoringPointController monitoring, AirQualityController airQuality,
                              RecommendationController recommendation, AlertController alert, ReportController report) { }

    public static final String LOGIN = "login", REGISTER = "register", RESIDENT = "resident",
            OFFICER = "officer", ADMIN = "admin";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final Controllers controllers;

    private final RegisterView registerView;
    private final LoginView loginView;
    private final ResidentView residentView;
    private final OfficerView officerView;
    private final AdminView adminView;

    private UserProfile currentUser;
    private AlertObserver activeObserver;

    public MainFrame(Controllers controllers) {
        super("GreenAir Klang Valley");
        this.controllers = controllers;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 680));
        setSize(1150, 760);
        setLocationRelativeTo(null);

        loginView = new LoginView(this, controllers);
        registerView = new RegisterView(this, controllers);
        residentView = new ResidentView(this, controllers);
        officerView = new OfficerView(this, controllers);
        adminView = new AdminView(this, controllers);
        root.add(loginView, LOGIN);
        root.add(registerView, REGISTER);
        root.add(residentView, RESIDENT);
        root.add(officerView, OFFICER);
        root.add(adminView, ADMIN);
        setContentPane(root);
        show(LOGIN);
    }

    public void show(String viewName) { cards.show(root, viewName); }

    public void showRegister() { show(REGISTER); }

    public UserProfile currentUser() { return currentUser; }

    /** Opens the dashboard that matches the user's role and registers the matching alert observer. */
    public void openDashboard(UserProfile user) {
        currentUser = user;
        attachObserver(user);
        switch (user.role()) {
            case RESIDENT -> { residentView.open(user); show(RESIDENT); }
            case ENVIRONMENT_OFFICER -> { officerView.open(user); show(OFFICER); }
            case ADMINISTRATOR -> { adminView.open(user); show(ADMIN); }
        }
    }

    /** Called when the logged-in user's profile changes (e.g. new region subscriptions) so alerts follow the change. */
    public void refreshSession(UserProfile updated) {
        currentUser = updated;
        attachObserver(updated);
    }

    private void attachObserver(UserProfile user) {
        if (activeObserver != null) controllers.alert().removeObserver(activeObserver);
        activeObserver = switch (user.role()) {
            case RESIDENT -> new ResidentAlertObserver(user.username(), user.districts()) {
                @Override public void update(PollutionAlert alert) {
                    if (subscribesTo(alert.getDistrict())) {
                        dispatchAlert(user, () -> residentView.showAlert(alert));
                    }
                }
            };
            case ENVIRONMENT_OFFICER -> new OfficerAlertObserver(user.username()) {
                @Override public void update(PollutionAlert alert) {
                    dispatchAlert(user, () -> officerView.showAlert(alert));
                }
            };
            case ADMINISTRATOR -> null;
        };
        if (activeObserver != null) controllers.alert().registerObserver(activeObserver);
    }

    private void dispatchAlert(UserProfile recipient, Runnable display) {
        SwingUtilities.invokeLater(() -> {
            if (currentUser != null && currentUser.username().equals(recipient.username())) display.run();
        });
    }

    public void logout() {
        if (activeObserver != null) controllers.alert().removeObserver(activeObserver);
        activeObserver = null;
        currentUser = null;
        controllers.user().logOut();
        loginView.reset();
        show(LOGIN);
    }

    MonitoringPoint pointForDistrict(String district) {
        return controllers.monitoring().getAllPoints().stream()
                .filter(point -> point.active() && point.district().equalsIgnoreCase(district))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No active monitoring point in " + district + "."));
    }
}
