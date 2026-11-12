package greenair.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.function.BiConsumer;

/** Environment officer dashboard: citizen reports, alerts and AQI across all monitored districts. */
public class OfficerView extends JPanel {
    private static final String[] STATUSES = {"Submitted", "Under review", "Action taken", "Resolved", "Dismissed"};

    private final JLabel userLabel = new JLabel();
    private final JButton logoutButton = Theme.secondaryButton("Log out");

    private final DefaultTableModel reportModel =
            Theme.readOnlyModel("ID", "Submitted", "Type", "District", "Reporter", "Status");
    private final JTable reportTable = new JTable(reportModel);
    private final JTextArea reportDetail = new JTextArea(6, 40);
    private final JComboBox<String> statusCombo = new JComboBox<>(STATUSES);
    private final List<String> reportDescriptions = new java.util.ArrayList<>();

    private final DefaultTableModel alertModel =
            Theme.readOnlyModel("Time", "District", "Pollutant", "Level", "Message");
    private final DefaultTableModel overviewModel =
            Theme.readOnlyModel("District", "PM2.5 (µg/m³)", "PM10 (µg/m³)", "US AQI", "Category");

    private Runnable onRefreshReports = () -> { };
    private Runnable onRefreshOverview = () -> { };
    private Runnable onRefreshAlerts = () -> { };
    private BiConsumer<String, String> onUpdateStatus = (id, s) -> { };
    private Runnable onLogout = () -> { };

    public OfficerView() {
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        add(Theme.banner("Environment Officer", userLabel, logoutButton), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.HEADING);
        tabs.addTab("Citizen reports", buildReportsTab());
        tabs.addTab("Alerts (all districts)", buildAlertsTab());
        tabs.addTab("District overview", buildOverviewTab());
        add(tabs, BorderLayout.CENTER);
        logoutButton.addActionListener(e -> onLogout.run());
    }

    private JPanel buildReportsTab() {
        JPanel page = Theme.page();
        Theme.styleTable(reportTable);
        reportTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        reportTable.getSelectionModel().addListSelectionListener(e -> {
            int row = reportTable.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0 && row < reportDescriptions.size()) {
                reportDetail.setText(reportDescriptions.get(row));
                statusCombo.setSelectedItem(reportModel.getValueAt(row, 5));
            }
        });

        JButton refresh = Theme.secondaryButton("Refresh");
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Theme.heading("Reports submitted by residents"), BorderLayout.WEST);
        top.add(refresh, BorderLayout.EAST);

        reportDetail.setEditable(false);
        reportDetail.setLineWrap(true);
        reportDetail.setWrapStyleWord(true);
        reportDetail.setFont(Theme.BODY);
        JButton update = Theme.primaryButton("Update status");
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        controls.setOpaque(false);
        controls.add(Theme.heading("Status"));
        controls.add(statusCombo);
        controls.add(update);

        JPanel detail = Theme.card();
        detail.setLayout(new BorderLayout(0, 8));
        detail.add(Theme.heading("Report details"), BorderLayout.NORTH);
        detail.add(Theme.scroll(reportDetail), BorderLayout.CENTER);
        detail.add(controls, BorderLayout.SOUTH);
        detail.setPreferredSize(new Dimension(100, 220));

        page.add(top, BorderLayout.NORTH);
        page.add(Theme.scroll(reportTable), BorderLayout.CENTER);
        page.add(detail, BorderLayout.SOUTH);

        refresh.addActionListener(e -> onRefreshReports.run());
        update.addActionListener(e -> {
            int row = reportTable.getSelectedRow();
            if (row < 0) { Theme.error(this, "Select a report first."); return; }
            onUpdateStatus.accept(String.valueOf(reportModel.getValueAt(row, 0)), (String) statusCombo.getSelectedItem());
        });
        return page;
    }

    private JPanel buildAlertsTab() {
        JPanel page = Theme.page();
        JTable table = new JTable(alertModel);
        Theme.styleTable(table);
        table.getColumnModel().getColumn(4).setPreferredWidth(380);
        JButton refresh = Theme.secondaryButton("Refresh");
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Theme.heading("Pollution alerts across all monitored districts"), BorderLayout.WEST);
        top.add(refresh, BorderLayout.EAST);
        page.add(top, BorderLayout.NORTH);
        page.add(Theme.scroll(table), BorderLayout.CENTER);
        refresh.addActionListener(e -> onRefreshAlerts.run());
        return page;
    }

    private JPanel buildOverviewTab() {
        JPanel page = Theme.page();
        JTable table = new JTable(overviewModel);
        Theme.styleTable(table);
        table.getColumnModel().getColumn(3).setCellRenderer(new ResidentView.AqiCellRenderer());
        JButton refresh = Theme.secondaryButton("Refresh");
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Theme.heading("Latest readings for every monitored district"), BorderLayout.WEST);
        top.add(refresh, BorderLayout.EAST);
        page.add(top, BorderLayout.NORTH);
        page.add(Theme.scroll(table), BorderLayout.CENTER);
        refresh.addActionListener(e -> onRefreshOverview.run());
        return page;
    }

    // ---------- API for controllers ----------

    public void setUserInfo(String name) { userLabel.setText("Signed in as " + name); }

    /** Rows: {id, submitted, type, district, reporter, status}; descriptions are shown in the detail pane. */
    public void setReports(List<Object[]> rows, List<String> descriptions) {
        Theme.fill(reportModel, rows);
        reportDescriptions.clear();
        reportDescriptions.addAll(descriptions);
        reportDetail.setText("");
    }

    /** Rows: {time, district, pollutant, level, message}. */
    public void setAlerts(List<Object[]> rows) { Theme.fill(alertModel, rows); }

    /** Adds an alert and notifies the officer (Observer callback target). */
    public void showAlert(Object[] row) {
        SwingUtilities.invokeLater(() -> {
            alertModel.insertRow(0, row);
            JOptionPane.showMessageDialog(this, row[4], "Pollution alert - " + row[1], JOptionPane.WARNING_MESSAGE);
        });
    }

    /** Rows: {district, pm2.5, pm10, aqi, category}. */
    public void setOverview(List<Object[]> rows) { Theme.fill(overviewModel, rows); }

    public void showMessage(String message) { Theme.info(this, message); }

    public void setOnRefreshReports(Runnable h) { onRefreshReports = h; }
    public void setOnRefreshAlerts(Runnable h) { onRefreshAlerts = h; }
    public void setOnRefreshOverview(Runnable h) { onRefreshOverview = h; }
    public void setOnUpdateStatus(BiConsumer<String, String> h) { onUpdateStatus = h; }
    public void setOnLogout(Runnable h) { onLogout = h; }
}
