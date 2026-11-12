package greenair.view;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/** Resident registration form. */
public class RegisterView extends JPanel {

    /** Data captured by the form, handed to the controller. */
    public record RegistrationForm(String fullName, String email, char[] password,
                                   String groupType, List<String> subscribedDistricts) { }

    private final JTextField nameField = new JTextField(24);
    private final JTextField emailField = new JTextField(24);
    private final JPasswordField passwordField = new JPasswordField(24);
    private final JPasswordField confirmField = new JPasswordField(24);
    private final JComboBox<String> groupCombo =
            new JComboBox<>(new String[]{"General public", "Children", "Elderly"});
    private final DefaultListModel<String> districtModel = new DefaultListModel<>();
    private final JList<String> districtList = new JList<>(districtModel);
    private final JButton submitButton = Theme.primaryButton("Register");
    private final JButton backButton = Theme.secondaryButton("Back to login");

    private Consumer<RegistrationForm> onSubmit = f -> { };
    private Runnable onBack = () -> { };

    public RegisterView() {
        setLayout(new GridBagLayout());
        setBackground(Theme.BG);

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 12));
        card.setPreferredSize(new Dimension(560, 600));
        card.add(Theme.title("Create resident account"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        districtList.setVisibleRowCount(5);
        districtList.setFont(Theme.BODY);
        Theme.addRow(form, 0, "Full name", nameField);
        Theme.addRow(form, 1, "Email", emailField);
        Theme.addRow(form, 2, "Password", passwordField);
        Theme.addRow(form, 3, "Confirm password", confirmField);
        Theme.addRow(form, 4, "I am a", groupCombo);
        Theme.addRow(form, 5, "Alert districts", Theme.scroll(districtList));
        card.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(backButton);
        buttons.add(submitButton);
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(Theme.muted("Hold Ctrl / Cmd to select several districts for pollution alerts."), BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);
        card.add(south, BorderLayout.SOUTH);
        add(card);

        submitButton.addActionListener(e -> submit());
        backButton.addActionListener(e -> onBack.run());
    }

    private void submit() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        char[] pw = passwordField.getPassword();
        if (name.isEmpty() || email.isEmpty() || pw.length == 0) {
            Theme.error(this, "Please fill in name, email and password.");
            return;
        }
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            Theme.error(this, "Enter a valid email address.");
            return;
        }
        if (!java.util.Arrays.equals(pw, confirmField.getPassword())) {
            Theme.error(this, "Passwords do not match.");
            return;
        }
        onSubmit.accept(new RegistrationForm(name, email, pw,
                (String) groupCombo.getSelectedItem(), districtList.getSelectedValuesList()));
    }

    public void setDistricts(List<String> districts) {
        districtModel.clear();
        districts.forEach(districtModel::addElement);
    }

    public void setOnSubmit(Consumer<RegistrationForm> handler) { this.onSubmit = handler; }
    public void setOnBack(Runnable handler) { this.onBack = handler; }

    public void showMessage(String message) { Theme.info(this, message); }

    public void reset() {
        nameField.setText("");
        emailField.setText("");
        passwordField.setText("");
        confirmField.setText("");
        groupCombo.setSelectedIndex(0);
        districtList.clearSelection();
    }
}
