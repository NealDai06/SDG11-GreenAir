package greenair.view;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/** Login screen. The username is checked against the users file by the controller. */
public class LoginView extends JPanel {
    private final JTextField usernameField = new JTextField(22);
    private final JLabel errorLabel = new JLabel(" ");
    private final JButton loginButton = Theme.primaryButton("Log in");
    private final JButton registerButton = Theme.secondaryButton("Create a resident account");

    private Consumer<String> onLogin = u -> { };
    private Runnable onRegister = () -> { };

    public LoginView() {
        setLayout(new GridBagLayout());
        setBackground(Theme.BG);

        JPanel card = Theme.card();
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(440, 340));

        errorLabel.setForeground(new Color(0xC62828));
        errorLabel.setFont(Theme.BODY);

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
        registerButton.addActionListener(e -> onRegister.run());
    }

    private void submit() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            showError("Enter your username.");
            return;
        }
        showError(" ");
        onLogin.accept(username);
    }

    public void setOnLogin(Consumer<String> handler) { this.onLogin = handler; }
    public void setOnRegister(Runnable handler) { this.onRegister = handler; }
    public void showError(String message) { errorLabel.setText(message); }

    public void reset() {
        usernameField.setText("");
        showError(" ");
    }
}
