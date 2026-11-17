package graphicuserinterface.greenair.graphicuserinterface;


import graphicuserinterface.greenair.airqualitycontroller.AirQualityController;
import graphicuserinterface.greenair.alertcontroller.AlertController;
import graphicuserinterface.greenair.alertcontroller.AlertObserver;
import graphicuserinterface.greenair.alertcontroller.OfficerAlertObserver;
import graphicuserinterface.greenair.alertcontroller.ResidentAlertObserver;
import graphicuserinterface.greenair.monitoringcontroller.MonitoringPointController;
import graphicuserinterface.greenair.recommendationcontroller.RecommendationController;
import graphicuserinterface.greenair.reportcontroller.ReportController;
import graphicuserinterface.greenair.usercontroller.UserController;
import graphicuserinterface.greenair.usercontroller.UserProfile;

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
        switch (user.role()) {
            case RESIDENT -> { residentView.open(user); show(RESIDENT); }
            case ENVIRONMENT_OFFICER -> { officerView.open(user); show(OFFICER); }
            case ADMINISTRATOR -> { adminView.open(user); show(ADMIN); }
        }
        attachObserver(user);
    }

    /** Called when the logged-in user's profile changes (e.g. new region subscriptions) so alerts follow the change. */
    public void refreshSession(UserProfile updated) {
        currentUser = updated;
        attachObserver(updated);
    }

    private void attachObserver(UserProfile user) {
        if (activeObserver != null) controllers.alert().removeObserver(activeObserver);
        activeObserver = switch (user.role()) {
            case RESIDENT -> new ResidentAlertObserver(user, a -> SwingUtilities.invokeLater(() -> residentView.showAlert(a)));
            case ENVIRONMENT_OFFICER -> new OfficerAlertObserver(user, a -> SwingUtilities.invokeLater(() -> officerView.showAlert(a)));
            case ADMINISTRATOR -> null;
        };
        if (activeObserver != null) controllers.alert().registerObserver(activeObserver);
    }

    public void logout() {
        if (activeObserver != null) controllers.alert().removeObserver(activeObserver);
        activeObserver = null;
        currentUser = null;
        loginView.reset();
        show(LOGIN);
    }
}
