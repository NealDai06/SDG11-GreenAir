package greenair.view;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;

/** Login screen. Delegates authentication to a controller via {@link #setOnLogin}. */
public class LoginView extends JPanel {
    private final JTextField emailField = new JTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);
    private final JLabel errorLabel = new JLabel(" ");
    private final JButton loginButton = Theme.primaryButton("Log in");
    private final JButton registerButton = Theme.secondaryButton("Create a resident account");

    private BiConsumer<String, char[]> onLogin = (e, p) -> { };
    private Runnable onRegister = () -> { };

    public LoginView() {
        setLayout(new GridBagLayout());
        setBackground(Theme.BG);

        JPanel card = Theme.card();
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(440, 420));

        JLabel brand = Theme.title("GreenAir Klang Valley");
        JLabel tagline = Theme.muted("Smart city air quality monitoring");
        errorLabel.setForeground(new Color(0xC62828));
        errorLabel.setFont(Theme.BODY);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(4, 0, 4, 0);
        c.gridy = 0; card.add(brand, c);
        c.gridy = 1; card.add(tagline, c);
        c.gridy = 2; c.insets = new Insets(22, 0, 2, 0); card.add(Theme.heading("Email"), c);
        c.gridy = 3; c.insets = new Insets(2, 0, 4, 0); card.add(emailField, c);
        c.gridy = 4; c.insets = new Insets(10, 0, 2, 0); card.add(Theme.heading("Password"), c);
        c.gridy = 5; c.insets = new Insets(2, 0, 4, 0); card.add(passwordField, c);
        c.gridy = 6; c.insets = new Insets(8, 0, 4, 0); card.add(errorLabel, c);
        c.gridy = 7; card.add(loginButton, c);
        c.gridy = 8; c.insets = new Insets(14, 0, 0, 0); card.add(registerButton, c);

        add(card);

        loginButton.addActionListener(e -> submit());
        passwordField.addActionListener(e -> submit());
        registerButton.addActionListener(e -> onRegister.run());
    }

    private void submit() {
        String email = emailField.getText().trim();
        char[] pw = passwordField.getPassword();
        if (email.isEmpty() || pw.length == 0) {
            showError("Enter your email and password.");
            return;
        }
        showError(" ");
        onLogin.accept(email, pw);
    }

    public void setOnLogin(BiConsumer<String, char[]> handler) { this.onLogin = handler; }
    public void setOnRegister(Runnable handler) { this.onRegister = handler; }

    public void showError(String message) { errorLabel.setText(message); }

    /** Clears the form, e.g. after logout. */
    public void reset() {
        emailField.setText("");
        passwordField.setText("");
        showError(" ");
    }
}
