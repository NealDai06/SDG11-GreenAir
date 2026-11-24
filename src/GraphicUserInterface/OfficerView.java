package GraphicUserInterface;

import AirQualityController.AirQualityReading;
import AlertController.PollutionAlert;
import MonitoringPointController.MonitoringPoint;
import ReportController.Report;
import UserController.UserProfile;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Environment officer dashboard: citizen reports, alerts and AQI across all monitored points. */
public class OfficerView extends JPanel {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String[] STATUSES = {"Submitted", "Under review", "Action taken", "Resolved", "Dismissed"};

    private final MainFrame.Controllers c;
    private final JLabel userLabel = new JLabel();

    private final DefaultTableModel reportModel = Theme.readOnlyModel("ID", "Submitted", "Type", "District", "Reporter", "Status");
    private final JTable reportTable = new JTable(reportModel);
    private final JTextArea reportDetail = new JTextArea(6, 40);
    private final JComboBox<String> statusCombo = new JComboBox<>(STATUSES);
    private final List<String> reportDescriptions = new ArrayList<>();

    private final DefaultTableModel alertModel = Theme.readOnlyModel("Time", "District", "Severity", "Message");
    private final DefaultTableModel overviewModel = Theme.readOnlyModel("Station", "District", "PM2.5 (µg/m³)", "PM10 (µg/m³)", "US AQI", "Category");

    public OfficerView(MainFrame frame, MainFrame.Controllers controllers) {
        this.c = controllers;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        JButton logout = Theme.secondaryButton("Log out");
        logout.addActionListener(e -> frame.logout());
        add(Theme.banner("Environment Officer", userLabel, logout), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.HEADING);
        tabs.addTab("Citizen reports", buildReportsTab());
        tabs.addTab("Alerts (all districts)", buildAlertsTab());
        tabs.addTab("District overview", buildOverviewTab());
        add(tabs, BorderLayout.CENTER);
    }

    /** Called by MainFrame after login. */
    public void open(UserProfile profile) {
        userLabel.setText("Signed in as " + profile.fullName());
        Theme.guard(this, () -> { refreshReports(); refreshAlerts(); refreshOverview(); });
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
        refresh.addActionListener(e -> Theme.guard(this, this::refreshReports));
        update.addActionListener(e -> Theme.guard(this, () -> {
            int row = reportTable.getSelectedRow();
            if (row < 0) throw new IllegalArgumentException("Select a report first.");
            c.report().updateStatus(String.valueOf(reportModel.getValueAt(row, 0)), (String) statusCombo.getSelectedItem());
            refreshReports();
        }));
        return page;
    }

    private JPanel buildAlertsTab() {
        JPanel page = Theme.page();
        JTable table = new JTable(alertModel);
        Theme.styleTable(table);
        table.getColumnModel().getColumn(3).setPreferredWidth(420);
        JButton refresh = Theme.secondaryButton("Refresh");
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Theme.heading("Pollution alerts across all monitored districts"), BorderLayout.WEST);
        top.add(refresh, BorderLayout.EAST);
        page.add(top, BorderLayout.NORTH);
        page.add(Theme.scroll(table), BorderLayout.CENTER);
        refresh.addActionListener(e -> Theme.guard(this, this::refreshAlerts));
        return page;
    }

    private JPanel buildOverviewTab() {
        JPanel page = Theme.page();
        JTable table = new JTable(overviewModel);
        Theme.styleTable(table);
        table.getColumnModel().getColumn(4).setCellRenderer(new Theme.AqiCellRenderer());
        JButton refresh = Theme.secondaryButton("Refresh");
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Theme.heading("Latest reading at every monitoring point"), BorderLayout.WEST);
        top.add(refresh, BorderLayout.EAST);
        page.add(top, BorderLayout.NORTH);
        page.add(Theme.scroll(table), BorderLayout.CENTER);
        refresh.addActionListener(e -> Theme.guard(this, () -> refreshOverview(true)));
        return page;
    }

    private void refreshReports() {
        List<Object[]> rows = new ArrayList<>();
        reportDescriptions.clear();
        for (Report r : c.report().getAllReports()) {
            String district;
            try { district = c.monitoring().getPointById(r.getPointId()).district(); }
            catch (RuntimeException ex) { district = "(removed point)"; }
            rows.add(new Object[]{r.getReportId(), r.getSubmittedAt().format(FMT), r.getType(),
                    district, r.getUsername(), r.getStatus()});
            reportDescriptions.add(r.getDescription());
        }
        Theme.fill(reportModel, rows);
        reportDetail.setText("");
    }

    private void refreshAlerts() {
        List<Object[]> rows = new ArrayList<>();
        for (PollutionAlert a : c.alert().getOfficerAlerts()) {
            rows.add(new Object[]{a.getTimestamp().format(FMT), a.getDistrict(), a.getSeverity(), a.getMessage()});
        }
        Theme.fill(alertModel, rows);
    }

    private void refreshOverview() {
        refreshOverview(false);
    }

    private void refreshOverview(boolean forceRefresh) {
        List<MonitoringPoint> points = c.monitoring().getAllPoints();
        Theme.background(this, () -> overviewRows(points, forceRefresh), rows -> {
            Theme.fill(overviewModel, rows);
            refreshAlerts();
        });
    }

    private List<Object[]> overviewRows(List<MonitoringPoint> points, boolean forceRefresh) {
        List<Object[]> rows = new ArrayList<>();
        for (MonitoringPoint p : points) {
            if (!p.active()) continue;
            try {
                AirQualityReading r = forceRefresh ? c.airQuality().refresh(p.id()) : c.airQuality().getCurrentReading(p.id());
                rows.add(new Object[]{p.name(), p.district(), r.pm25(), r.pm10(), r.usAqi(), Theme.aqiCategory(r.usAqi())});
            } catch (RuntimeException ex) {
                rows.add(new Object[]{p.name(), p.district(), "-", "-", "-", "No data"});
            }
        }
        return rows;
    }

    /** Observer callback: a new alert from any district. Adds it to the table and shows a notification. */
    public void showAlert(PollutionAlert a) {
        alertModel.insertRow(0, new Object[]{a.getTimestamp().format(FMT), a.getDistrict(), a.getSeverity(), a.getMessage()});
        JOptionPane.showMessageDialog(this, a.getMessage(), "Pollution alert - " + a.getDistrict(), JOptionPane.WARNING_MESSAGE);
    }
}
