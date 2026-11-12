package greenair.view;

import javax.swing.*;
import java.awt.*;

/** Application window. Swaps between the login, register and role-specific views. */
public class MainFrame extends JFrame {
    public static final String LOGIN = "login";
    public static final String REGISTER = "register";
    public static final String RESIDENT = "resident";
    public static final String OFFICER = "officer";
    public static final String ADMIN = "admin";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    private final LoginView loginView = new LoginView();
    private final RegisterView registerView = new RegisterView();
    private final ResidentView residentView = new ResidentView();
    private final OfficerView officerView = new OfficerView();
    private final AdminView adminView = new AdminView();

    public MainFrame() {
        super("GreenAir Klang Valley");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 680));
        setSize(1150, 760);
        setLocationRelativeTo(null);

        root.add(loginView, LOGIN);
        root.add(registerView, REGISTER);
        root.add(residentView, RESIDENT);
        root.add(officerView, OFFICER);
        root.add(adminView, ADMIN);
        setContentPane(root);
        show(LOGIN);
    }

    public void show(String viewName) { cards.show(root, viewName); }

    public LoginView getLoginView() { return loginView; }
    public RegisterView getRegisterView() { return registerView; }
    public ResidentView getResidentView() { return residentView; }
    public OfficerView getOfficerView() { return officerView; }
    public AdminView getAdminView() { return adminView; }
}
