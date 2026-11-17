package graphicuserinterface.greenair.graphicuserinterface;

import graphicuserinterface.greenair.monitoringcontroller.MonitoringPoint;
import graphicuserinterface.greenair.usercontroller.UserProfile;
import graphicuserinterface.greenair.usercontroller.UserRole;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Administrator dashboard: user account management and monitoring point configuration. */
public class AdminView extends JPanel {
    private final MainFrame.Controllers c;
    private UserProfile admin;
    private final JLabel userLabel = new JLabel();

    private final DefaultTableModel userModel = Theme.readOnlyModel("Name", "Username", "Role", "Group", "Status");
    private final JTable userTable = new JTable(userModel);
    private final JComboBox<UserRole> roleCombo = new JComboBox<>(UserRole.values());

    private final DefaultTableModel pointModel = Theme.readOnlyModel("ID", "Location", "District", "Latitude", "Longitude", "Active");
    private final JTable pointTable = new JTable(pointModel);
    private final JTextField idField = new JTextField(12);
    private final JTextField locationField = new JTextField(20);
    private final JTextField districtField = new JTextField(20);
    private final JTextField latField = new JTextField(12);
    private final JTextField lonField = new JTextField(12);
    private final JCheckBox activeBox = new JCheckBox("Active", true);

    public AdminView(MainFrame frame, MainFrame.Controllers controllers) {
        this.c = controllers;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        roleCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                return super.getListCellRendererComponent(l, v instanceof UserRole r ? r.label() : v, i, s, f);
            }
        });
        JButton logout = Theme.secondaryButton("Log out");
        logout.addActionListener(e -> frame.logout());
        add(Theme.banner("Administrator", userLabel, logout), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.HEADING);
        tabs.addTab("User accounts", buildUsersTab());
        tabs.addTab("Monitoring points", buildPointsTab());
        add(tabs, BorderLayout.CENTER);
    }

    /** Called by MainFrame after login. */
    public void open(UserProfile profile) {
        this.admin = profile;
        userLabel.setText("Signed in as " + profile.name());
        Theme.guard(this, () -> { refreshUsers(); refreshPoints(); });
    }

    // ---------- users ----------

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

        refresh.addActionListener(e -> Theme.guard(this, this::refreshUsers));
        setRole.addActionListener(e -> Theme.guard(this, () -> {
            UserProfile u = selectedUser();
            UserRole role = (UserRole) roleCombo.getSelectedItem();
            if (u.username().equalsIgnoreCase(admin.username()) && role != UserRole.ADMINISTRATOR) {
                throw new IllegalArgumentException("You cannot remove your own administrator role.");
            }
            c.user().updateUser(u.withRole(role));
            refreshUsers();
        }));
        toggle.addActionListener(e -> Theme.guard(this, () -> {
            UserProfile u = selectedUser();
            if (u.username().equalsIgnoreCase(admin.username()) && u.active()) {
                throw new IllegalArgumentException("You cannot deactivate the account you are logged in with.");
            }
            c.user().updateUser(u.withActive(!u.active()));
            refreshUsers();
        }));
        delete.addActionListener(e -> Theme.guard(this, () -> {
            UserProfile u = selectedUser();
            if (u.username().equalsIgnoreCase(admin.username())) {
                throw new IllegalArgumentException("You cannot delete the account you are logged in with.");
            }
            int ok = JOptionPane.showConfirmDialog(this, "Delete user " + u.username() + "? This cannot be undone.",
                    "Confirm deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok == JOptionPane.YES_OPTION) { c.user().deleteUser(u.username()); refreshUsers(); }
        }));
        return page;
    }

    private UserProfile selectedUser() {
        int row = userTable.getSelectedRow();
        if (row < 0) throw new IllegalArgumentException("Select a user first.");
        String username = String.valueOf(userModel.getValueAt(row, 1));
        return c.user().getAllUsers().stream().filter(u -> u.username().equals(username)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("That user no longer exists."));
    }

    private void refreshUsers() {
        List<Object[]> rows = new ArrayList<>();
        for (UserProfile u : c.user().getAllUsers()) {
            rows.add(new Object[]{u.name(), u.username(), u.role().label(), u.groupType().label(), u.active() ? "Active" : "Inactive"});
        }
        Theme.fill(userModel, rows);
    }

    // ---------- monitoring points ----------

    private JPanel buildPointsTab() {
        JPanel page = Theme.page();
        Theme.styleTable(pointTable);
        pointTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pointTable.getSelectionModel().addListSelectionListener(e -> {
            int r = pointTable.getSelectedRow();
            if (!e.getValueIsAdjusting() && r >= 0) {
                idField.setText(String.valueOf(pointModel.getValueAt(r, 0)));
                locationField.setText(String.valueOf(pointModel.getValueAt(r, 1)));
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
        Theme.addRow(form, 1, "Location", locationField);
        Theme.addRow(form, 2, "District", districtField);
        Theme.addRow(form, 3, "Latitude", latField);
        Theme.addRow(form, 4, "Longitude", lonField);
        Theme.addRow(form, 5, "", activeBox);

        JButton save = Theme.primaryButton("Save");
        JButton fresh = Theme.secondaryButton("New");
        JButton delete = Theme.secondaryButton("Delete");
        delete.setForeground(new Color(0xC62828));
        JButton refresh = Theme.secondaryButton("Refresh");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(save);
        buttons.add(fresh);
        buttons.add(delete);
        buttons.add(refresh);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 6; gc.gridwidth = 2; gc.insets = new Insets(12, 4, 0, 4);
        gc.anchor = GridBagConstraints.WEST;
        form.add(buttons, gc);

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(form, BorderLayout.NORTH);
        page.add(left, BorderLayout.WEST);
        page.add(Theme.scroll(pointTable), BorderLayout.CENTER);

        save.addActionListener(e -> Theme.guard(this, this::savePoint));
        fresh.addActionListener(e -> clearPointForm());
        refresh.addActionListener(e -> Theme.guard(this, this::refreshPoints));
        delete.addActionListener(e -> Theme.guard(this, () -> {
            if (idField.getText().isBlank()) throw new IllegalArgumentException("Select a monitoring point first.");
            int ok = JOptionPane.showConfirmDialog(this, "Delete monitoring point " + idField.getText() + "?",
                    "Confirm deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok == JOptionPane.YES_OPTION) { c.monitoring().deletePoint(idField.getText()); refreshPoints(); clearPointForm(); }
        }));
        return page;
    }

    private void savePoint() {
        double lat, lon;
        try {
            lat = Double.parseDouble(latField.getText().trim());
            lon = Double.parseDouble(lonField.getText().trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Latitude and longitude must be numbers, e.g. 3.1390 and 101.6869.");
        }
        MonitoringPoint p = new MonitoringPoint(idField.getText().trim(), districtField.getText().trim(), lat, lon,
                locationField.getText().trim(), activeBox.isSelected());
        if (p.pointId().isBlank()) c.monitoring().createPoint(p); else c.monitoring().updatePoint(p);
        refreshPoints();
        clearPointForm();
        Theme.info(this, "Monitoring point saved.");
    }

    private void refreshPoints() {
        List<Object[]> rows = new ArrayList<>();
        for (MonitoringPoint p : c.monitoring().getAllPoints()) {
            rows.add(new Object[]{p.pointId(), p.location(), p.districtName(), p.latitude(), p.longitude(), p.active()});
        }
        Theme.fill(pointModel, rows);
    }

    private void clearPointForm() {
        pointTable.clearSelection();
        idField.setText("");
        locationField.setText("");
        districtField.setText("");
        latField.setText("");
        lonField.setText("");
        activeBox.setSelected(true);
    }
}
