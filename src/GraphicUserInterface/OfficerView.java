package graphicuserinterface.greenair.graphicuserinterface;

import graphicuserinterface.greenair.airqualitycontroller.AirQualityReading;
import graphicuserinterface.greenair.alertcontroller.PollutionAlert;
import graphicuserinterface.greenair.monitoringcontroller.MonitoringPoint;
import graphicuserinterface.greenair.reportcontroller.Report;
import graphicuserinterface.greenair.usercontroller.UserProfile;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
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
        userLabel.setText("Signed in as " + profile.name());
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
        refresh.addActionListener(e -> Theme.guard(this, this::refreshOverview));
        return page;
    }

    private void refreshReports() {
        List<Object[]> rows = new ArrayList<>();
        reportDescriptions.clear();
        for (Report r : c.report().getAllReports()) {
            String district;
            try { district = c.monitoring().getPointById(r.pointId()).districtName(); }
            catch (IllegalArgumentException ex) { district = "(removed point)"; }
            rows.add(new Object[]{r.reportId(), r.submittedAt().format(FMT), r.type(), district, r.username(), r.status()});
            reportDescriptions.add(r.description());
        }
        Theme.fill(reportModel, rows);
        reportDetail.setText("");
    }

    private void refreshAlerts() {
        List<Object[]> rows = new ArrayList<>();
        for (PollutionAlert a : c.alert().getOfficerAlerts()) {
            rows.add(new Object[]{a.timestamp().format(FMT), a.districtId(), a.severity(), a.message()});
        }
        Theme.fill(alertModel, rows);
    }

    private void refreshOverview() {
        List<Object[]> rows = new ArrayList<>();
        for (MonitoringPoint p : c.monitoring().getAllPoints()) {
            if (!p.active()) continue;
            try {
                AirQualityReading r = c.airQuality().getCurrentReading(p.pointId());
                rows.add(new Object[]{p.location(), p.districtName(), r.pm25(), r.pm10(), r.usAqi(), r.category()});
            } catch (IOException ex) {
                rows.add(new Object[]{p.location(), p.districtName(), "-", "-", "-", "No data"});
            }
        }
        Theme.fill(overviewModel, rows);
        refreshAlerts();
    }

    /** Observer callback: a new alert from any district. Adds it to the table and shows a notification. */
    public void showAlert(PollutionAlert a) {
        alertModel.insertRow(0, new Object[]{a.timestamp().format(FMT), a.districtId(), a.severity(), a.message()});
        JOptionPane.showMessageDialog(this, a.message(), "Pollution alert - " + a.districtId(), JOptionPane.WARNING_MESSAGE);
    }
}
