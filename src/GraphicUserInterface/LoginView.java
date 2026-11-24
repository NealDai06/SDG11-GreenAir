package GraphicUserInterface;

import UserController.UserProfile;

import javax.swing.*;
import java.awt.*;

/** Login screen. The username is checked against the users file through UserController. */
public class LoginView extends JPanel {
    private final MainFrame frame;
    private final MainFrame.Controllers controllers;
    private final JTextField usernameField = new JTextField(22);
    private final JLabel errorLabel = new JLabel(" ");

    public LoginView(MainFrame frame, MainFrame.Controllers controllers) {
        this.frame = frame;
        this.controllers = controllers;
        setLayout(new GridBagLayout());
        setBackground(Theme.BG);

        JPanel card = Theme.card();
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(440, 340));
        errorLabel.setForeground(new Color(0xC62828));
        errorLabel.setFont(Theme.BODY);
        JButton loginButton = Theme.primaryButton("Log in");
        JButton registerButton = Theme.secondaryButton("Create a resident account");

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(4, 0, 4, 0);
        c.gridy = 0; card.add(Theme.title("GreenAir Klang Valley"), c);
        c.gridy = 1; card.add(Theme.muted("Smart city air quality monitoring"), c);
        c.gridy = 2; c.insets = new Insets(22, 0, 2, 0); card.add(Theme.heading("Username"), c);
        c.gridy = 3; c.insets = new Insets(2, 0, 4, 0); card.add(usernameField, c);
        c.gridy = 4; c.insets = new Insets(8, 0, 4, 0); card.add(errorLabel, c);
        c.gridy = 5; card.add(loginButton, c);
        c.gridy = 6; c.insets = new Insets(14, 0, 0, 0); card.add(registerButton, c);
        add(card);

        loginButton.addActionListener(e -> submit());
        usernameField.addActionListener(e -> submit());
        registerButton.addActionListener(e -> frame.showRegister());
    }

    private void submit() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            errorLabel.setText("Enter your username.");
            return;
        }
        try {
            UserProfile profile = controllers.user().logIn(username);
            errorLabel.setText(" ");
            frame.openDashboard(profile);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            errorLabel.setText(ex.getMessage());
        } catch (RuntimeException ex) {
            errorLabel.setText(ex.getMessage());
        }
    }

    public void reset() {
        usernameField.setText("");
        errorLabel.setText(" ");
    }
}
