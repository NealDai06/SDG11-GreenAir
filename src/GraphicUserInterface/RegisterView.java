package GraphicUserInterface;

import UserController.GroupType;
import UserController.UserProfile;
import UserController.UserRole;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Resident registration form (no password, no region choice). Residents subscribe to regions later from the
 * "All regions" tab of their dashboard.
 */
public class RegisterView extends JPanel {
    private final MainFrame frame;
    private final MainFrame.Controllers controllers;
    private final JTextField nameField = new JTextField(24);
    private final JTextField usernameField = new JTextField(24);
    private final JComboBox<String> groupCombo = new JComboBox<>();

    public RegisterView(MainFrame frame, MainFrame.Controllers controllers) {
        this.frame = frame;
        this.controllers = controllers;
        setLayout(new GridBagLayout());
        setBackground(Theme.BG);
        for (GroupType g : GroupType.values()) groupCombo.addItem(g.label());

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 12));
        card.setPreferredSize(new Dimension(520, 380));
        card.add(Theme.title("Create resident account"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        Theme.addRow(form, 0, "Full name", nameField);
        Theme.addRow(form, 1, "Username", usernameField);
        Theme.addRow(form, 2, "I am a", groupCombo);
        card.add(form, BorderLayout.CENTER);

        JButton back = Theme.secondaryButton("Back to login");
        JButton submit = Theme.primaryButton("Register");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(back);
        buttons.add(submit);
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(Theme.muted("After logging in, use the All regions tab to choose the regions you want alerts for."), BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);
        card.add(south, BorderLayout.SOUTH);
        add(card);

        back.addActionListener(e -> frame.show(MainFrame.LOGIN));
        submit.addActionListener(e -> Theme.guard(this, this::submit));
    }

    private void submit() {
        UserProfile profile = controllers.user().register(usernameField.getText().trim(), nameField.getText().trim(),
                GroupType.fromLabel((String) groupCombo.getSelectedItem()), List.of());
        Theme.info(this, "Account created. You can now log in as \"" + profile.username() + "\".");
        nameField.setText("");
        usernameField.setText("");
        groupCombo.setSelectedIndex(0);
        frame.show(MainFrame.LOGIN);
    }
}
