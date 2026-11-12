package greenair.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Administrator dashboard: user account management and monitoring point configuration. */
public class AdminView extends JPanel {

    /** Monitoring point details captured by the form. {@code id} is empty for a new point. */
    public record MonitoringPointForm(String id, String name, String district,
                                      double latitude, double longitude, boolean active) { }

    private static final String[] ROLES = {"RESIDENT", "ENVIRONMENT_OFFICER", "ADMINISTRATOR"};

    private final JLabel userLabel = new JLabel();
    private final JButton logoutButton = Theme.secondaryButton("Log out");

    // Users tab
    private final DefaultTableModel userModel = Theme.readOnlyModel("ID", "Name", "Username", "Role", "Status");
    private final JTable userTable = new JTable(userModel);
    private final JComboBox<String> roleCombo = new JComboBox<>(ROLES);

    // Monitoring points tab
    private final DefaultTableModel pointModel =
            Theme.readOnlyModel("ID", "Name", "District", "Latitude", "Longitude", "Active");
    private final JTable pointTable = new JTable(pointModel);
    private final JTextField idField = new JTextField(12);
    private final JTextField nameField = new JTextField(20);
    private final JTextField districtField = new JTextField(20);
    private final JTextField latField = new JTextField(12);
    private final JTextField lonField = new JTextField(12);
    private final JCheckBox activeBox = new JCheckBox("Active", true);

    private Runnable onRefreshUsers = () -> { };
    private BiConsumer<String, String> onChangeRole = (id, role) -> { };
    private BiConsumer<String, Boolean> onSetUserActive = (id, active) -> { };
    private Consumer<String> onDeleteUser = id -> { };
    private Runnable onRefreshPoints = () -> { };
    private Consumer<MonitoringPointForm> onSavePoint = f -> { };
    private Consumer<String> onDeletePoint = id -> { };
    private Runnable onLogout = () -> { };

    public AdminView() {
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        add(Theme.banner("Administrator", userLabel, logoutButton), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.HEADING);
        tabs.addTab("User accounts", buildUsersTab());
        tabs.addTab("Monitoring points", buildPointsTab());
        add(tabs, BorderLayout.CENTER);
        logoutButton.addActionListener(e -> onLogout.run());
    }

    private JPanel buildUsersTab() {
        JPanel page = Theme.page();
        Theme.styleTable(userTable);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JButton refresh = Theme.secondaryButton("Refresh");
        JButton setRole = Theme.primaryButton("Set role");
        JButton toggle = Theme.secondaryButton("Activate / deactivate");
        JButton delete = Theme.secondaryButton("Delete user");
        delete.setForeground(new Color(0xC62828));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        actions.add(Theme.heading("Role"));
        actions.add(roleCombo);
        actions.add(setRole);
        actions.add(toggle);
        actions.add(delete);
        actions.add(refresh);

        page.add(actions, BorderLayout.NORTH);
        page.add(Theme.scroll(userTable), BorderLayout.CENTER);

        refresh.addActionListener(e -> onRefreshUsers.run());
        setRole.addActionListener(e -> withSelectedUser(id -> onChangeRole.accept(id, (String) roleCombo.getSelectedItem())));
        toggle.addActionListener(e -> {
            int row = userTable.getSelectedRow();
            if (row < 0) { Theme.error(this, "Select a user first."); return; }
            boolean currentlyActive = "Active".equalsIgnoreCase(String.valueOf(userModel.getValueAt(row, 4)));
            onSetUserActive.accept(String.valueOf(userModel.getValueAt(row, 0)), !currentlyActive);
        });
        delete.addActionListener(e -> withSelectedUser(id -> {
            int ok = JOptionPane.showConfirmDialog(this, "Delete user " + id + "? This cannot be undone.",
                    "Confirm deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok == JOptionPane.YES_OPTION) onDeleteUser.accept(id);
        }));
        return page;
    }

    private void withSelectedUser(Consumer<String> action) {
        int row = userTable.getSelectedRow();
        if (row < 0) { Theme.error(this, "Select a user first."); return; }
        action.accept(String.valueOf(userModel.getValueAt(row, 0)));
    }

    private JPanel buildPointsTab() {
        JPanel page = Theme.page();
        Theme.styleTable(pointTable);
        pointTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pointTable.getSelectionModel().addListSelectionListener(e -> {
            int r = pointTable.getSelectedRow();
            if (!e.getValueIsAdjusting() && r >= 0) {
                idField.setText(String.valueOf(pointModel.getValueAt(r, 0)));
                nameField.setText(String.valueOf(pointModel.getValueAt(r, 1)));
                districtField.setText(String.valueOf(pointModel.getValueAt(r, 2)));
                latField.setText(String.valueOf(pointModel.getValueAt(r, 3)));
                lonField.setText(String.valueOf(pointModel.getValueAt(r, 4)));
                activeBox.setSelected(Boolean.parseBoolean(String.valueOf(pointModel.getValueAt(r, 5))));
            }
        });

        idField.setEditable(false);
        idField.setToolTipText("Assigned automatically for new monitoring points");
        JPanel form = Theme.card();
        form.setLayout(new GridBagLayout());
        form.setPreferredSize(new Dimension(380, 100));
        Theme.addRow(form, 0, "ID", idField);
        Theme.addRow(form, 1, "Name", nameField);
        Theme.addRow(form, 2, "District", districtField);
        Theme.addRow(form, 3, "Latitude", latField);
        Theme.addRow(form, 4, "Longitude", lonField);
        Theme.addRow(form, 5, "", activeBox);

        JButton save = Theme.primaryButton("Save");
        JButton clear = Theme.secondaryButton("New");
        JButton delete = Theme.secondaryButton("Delete");
        delete.setForeground(new Color(0xC62828));
        JButton refresh = Theme.secondaryButton("Refresh");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(save);
        buttons.add(clear);
        buttons.add(delete);
        buttons.add(refresh);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 6; c.gridwidth = 2; c.insets = new Insets(12, 4, 0, 4);
        c.anchor = GridBagConstraints.WEST;
        form.add(buttons, c);

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(form, BorderLayout.NORTH);
        page.add(left, BorderLayout.WEST);
        page.add(Theme.scroll(pointTable), BorderLayout.CENTER);

        save.addActionListener(e -> savePoint());
        clear.addActionListener(e -> clearPointForm());
        refresh.addActionListener(e -> onRefreshPoints.run());
        delete.addActionListener(e -> {
            if (idField.getText().isBlank()) { Theme.error(this, "Select a monitoring point first."); return; }
            int ok = JOptionPane.showConfirmDialog(this, "Delete monitoring point " + idField.getText() + "?",
                    "Confirm deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok == JOptionPane.YES_OPTION) onDeletePoint.accept(idField.getText());
        });
        return page;
    }

    private void savePoint() {
        String name = nameField.getText().trim();
        String district = districtField.getText().trim();
        if (name.isEmpty() || district.isEmpty()) {
            Theme.error(this, "Name and district are required.");
            return;
        }
        double lat, lon;
        try {
            lat = Double.parseDouble(latField.getText().trim());
            lon = Double.parseDouble(lonField.getText().trim());
        } catch (NumberFormatException ex) {
            Theme.error(this, "Latitude and longitude must be numbers, e.g. 3.1390 and 101.6869.");
            return;
        }
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            Theme.error(this, "Latitude must be -90 to 90 and longitude -180 to 180.");
            return;
        }
        onSavePoint.accept(new MonitoringPointForm(idField.getText().trim(), name, district, lat, lon, activeBox.isSelected()));
    }

    public void clearPointForm() {
        pointTable.clearSelection();
        idField.setText("");
        nameField.setText("");
        districtField.setText("");
        latField.setText("");
        lonField.setText("");
        activeBox.setSelected(true);
    }

    // ---------- API for controllers ----------

    public void setUserInfo(String name) { userLabel.setText("Signed in as " + name); }

    /** Rows: {id, name, email, role, status}. */
    public void setUsers(List<Object[]> rows) { Theme.fill(userModel, rows); }

    /** Rows: {id, name, district, latitude, longitude, active}. */
    public void setMonitoringPoints(List<Object[]> rows) { Theme.fill(pointModel, rows); }

    public void showMessage(String message) { Theme.info(this, message); }

    public void setOnRefreshUsers(Runnable h) { onRefreshUsers = h; }
    public void setOnChangeRole(BiConsumer<String, String> h) { onChangeRole = h; }
    public void setOnSetUserActive(BiConsumer<String, Boolean> h) { onSetUserActive = h; }
    public void setOnDeleteUser(Consumer<String> h) { onDeleteUser = h; }
    public void setOnRefreshPoints(Runnable h) { onRefreshPoints = h; }
    public void setOnSavePoint(Consumer<MonitoringPointForm> h) { onSavePoint = h; }
    public void setOnDeletePoint(Consumer<String> h) { onDeletePoint = h; }
    public void setOnLogout(Runnable h) { onLogout = h; }
}
