package GraphicUserInterface;

import AirQualityController.AirQualityReading;
import AlertController.PollutionAlert;
import MonitoringPointController.MonitoringPoint;
import RecommendationController.Park;
import ReportController.Report;
import UserController.GroupType;
import UserController.UserProfile;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Resident dashboard: air quality, park recommendations, advice, reports and alerts. */
public class ResidentView extends JPanel {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String[] REPORT_TYPES = {"Open burning", "Smog / haze", "Park maintenance", "Other"};

    private final MainFrame frame;
    private final MainFrame.Controllers c;
    private UserProfile user;

    private final JLabel userLabel = new JLabel();
    private final List<JComboBox<String>> districtCombos = new ArrayList<>();

    private final JComboBox<String> aqDistrict = newDistrictCombo();
    private final JLabel pm25Value = valueLabel(), pm10Value = valueLabel(), aqiValue = valueLabel();
    private final JLabel aqiCategory = Theme.heading(" ");
    private final JLabel aqUpdated = Theme.muted("No data loaded yet.");
    private final DefaultTableModel forecastModel = Theme.readOnlyModel("Time", "PM2.5 (µg/m³)", "PM10 (µg/m³)", "US AQI");

    private final JComboBox<String> parkDistrict = newDistrictCombo();
    private final DefaultTableModel parkModel = Theme.readOnlyModel("Park", "District", "Latitude", "Longitude");

    private final JComboBox<String> adviceGroup = new JComboBox<>();
    private final JComboBox<String> adviceDistrict = newDistrictCombo();
    private final JTextArea adviceArea = new JTextArea(8, 40);

    private final JComboBox<String> reportType = new JComboBox<>(REPORT_TYPES);
    private final JComboBox<String> reportDistrict = newDistrictCombo();
    private final JTextField reportLocation = new JTextField(30);
    private final JTextArea reportDescription = new JTextArea(6, 30);

    private final DefaultTableModel regionModel = Theme.readOnlyModel("Region", "US AQI", "Category", "Issued at", "Subscribed");
    private final JTable regionTable = new JTable(regionModel);

    private final DefaultTableModel alertModel = Theme.readOnlyModel("Time", "District", "Severity", "Message");
    private final JLabel subscribedLabel = Theme.muted("Subscribed districts: -");

    public ResidentView(MainFrame frame, MainFrame.Controllers controllers) {
        this.frame = frame;
        this.c = controllers;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        for (GroupType g : GroupType.values()) adviceGroup.addItem(g.label());

        JButton logout = Theme.secondaryButton("Log out");
        logout.addActionListener(e -> frame.logout());
        add(Theme.banner("Resident", userLabel, logout), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.HEADING);
        tabs.addTab("Air quality", buildAirQualityTab());
        tabs.addTab("All regions", buildRegionsTab());
        tabs.addTab("Park recommendations", buildParksTab());
        tabs.addTab("Activity advice", buildAdviceTab());
        tabs.addTab("Report an issue", buildReportTab());
        tabs.addTab("Alerts", buildAlertsTab());
        add(tabs, BorderLayout.CENTER);
    }

    /** Called by MainFrame after login. */
    public void open(UserProfile profile) {
        this.user = profile;
        userLabel.setText("Signed in as " + profile.fullName() + " (" + profile.groupType().label() + ")");
        adviceGroup.setSelectedItem(profile.groupType().label());
        Theme.guard(this, () -> {
            List<String> districts = c.monitoring().activeDistricts();
            for (JComboBox<String> cb : districtCombos) {
                cb.removeAllItems();
                districts.forEach(cb::addItem);
            }
            showSubscriptions();
            loadRegions();
            refreshAlerts();
        });
    }

    // ---------- tabs ----------

    private JPanel buildAirQualityTab() {
        JPanel page = Theme.page();
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        top.setOpaque(false);
        JButton fetch = Theme.primaryButton("Check air quality");
        top.add(Theme.heading("District"));
        top.add(aqDistrict);
        top.add(fetch);
        fetch.addActionListener(e -> Theme.guard(this, this::loadAirQuality));

        JPanel aqiCard = Theme.card();
        JPanel cards = new JPanel(new GridLayout(1, 3, 14, 0));
        cards.setOpaque(false);
        cards.add(metricCard("PM2.5 (µg/m³)", pm25Value, null, Theme.card()));
        cards.add(metricCard("PM10 (µg/m³)", pm10Value, null, Theme.card()));
        cards.add(metricCard("US AQI", aqiValue, aqiCategory, aqiCard));

        JPanel north = new JPanel(new BorderLayout(0, 12));
        north.setOpaque(false);
        north.add(top, BorderLayout.NORTH);
        north.add(cards, BorderLayout.CENTER);
        north.add(aqUpdated, BorderLayout.SOUTH);

        JTable table = new JTable(forecastModel);
        Theme.styleTable(table);
        table.getColumnModel().getColumn(3).setCellRenderer(new Theme.AqiCellRenderer());
        JPanel forecast = new JPanel(new BorderLayout(0, 6));
        forecast.setOpaque(false);
        forecast.add(Theme.heading("Forecast"), BorderLayout.NORTH);
        forecast.add(Theme.scroll(table), BorderLayout.CENTER);
        page.add(north, BorderLayout.NORTH);
        page.add(forecast, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildRegionsTab() {
        JPanel page = Theme.page();
        JButton toggle = Theme.primaryButton("Subscribe / unsubscribe");
        JButton refresh = Theme.secondaryButton("Refresh readings");
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        top.setOpaque(false);
        top.add(toggle);
        top.add(refresh);
        top.add(Theme.muted("Select a region, then subscribe to receive pollution alerts for it."));

        Theme.styleTable(regionTable);
        regionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        regionTable.getColumnModel().getColumn(1).setCellRenderer(new Theme.AqiCellRenderer());
        page.add(top, BorderLayout.NORTH);
        page.add(Theme.scroll(regionTable), BorderLayout.CENTER);
        page.add(Theme.muted("Subscribed regions are used for the Alerts tab and for pop-up alerts."), BorderLayout.SOUTH);

        toggle.addActionListener(e -> Theme.guard(this, this::toggleSubscription));
        refresh.addActionListener(e -> Theme.guard(this, () -> { loadRegions(true); refreshAlerts(); }));
        return page;
    }

    private JPanel buildParksTab() {
        JPanel page = Theme.page();
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        top.setOpaque(false);
        JButton find = Theme.primaryButton("Find parks");
        top.add(Theme.heading("Near district"));
        top.add(parkDistrict);
        top.add(find);
        find.addActionListener(e -> Theme.guard(this, this::findParks));
        JTable table = new JTable(parkModel);
        Theme.styleTable(table);
        page.add(top, BorderLayout.NORTH);
        page.add(Theme.scroll(table), BorderLayout.CENTER);
        page.add(Theme.muted("Parks are ranked by air quality, then distance from the selected district's monitoring point."), BorderLayout.SOUTH);
        return page;
    }

    private JPanel buildAdviceTab() {
        JPanel page = Theme.page();
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        JButton get = Theme.primaryButton("Get advice");
        Theme.addRow(form, 0, "User group", adviceGroup);
        Theme.addRow(form, 1, "District", adviceDistrict);
        JPanel btn = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btn.setOpaque(false);
        btn.add(get);
        Theme.addRow(form, 2, "", btn);

        adviceArea.setEditable(false);
        adviceArea.setLineWrap(true);
        adviceArea.setWrapStyleWord(true);
        adviceArea.setFont(new Font("SansSerif", Font.PLAIN, 15));
        adviceArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        adviceArea.setText("Choose a group and district, then press \"Get advice\".");
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 8));
        card.add(Theme.heading("Personalised outdoor activity advice"), BorderLayout.NORTH);
        card.add(Theme.scroll(adviceArea), BorderLayout.CENTER);

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(380, 100));
        left.add(form, BorderLayout.NORTH);
        page.add(left, BorderLayout.WEST);
        page.add(card, BorderLayout.CENTER);
        get.addActionListener(e -> Theme.guard(this, () -> {
            GroupType group = GroupType.fromLabel((String) adviceGroup.getSelectedItem());
            String pid = pointId(adviceDistrict);
            Theme.background(this, () -> c.recommendation().getActivityAdvice(group, pid), adviceArea::setText);
        }));
        return page;
    }

    private JPanel buildReportTab() {
        JPanel page = Theme.page();
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 12));
        card.add(Theme.title("Report an environmental issue"), BorderLayout.NORTH);
        reportDescription.setLineWrap(true);
        reportDescription.setWrapStyleWord(true);
        reportDescription.setFont(Theme.BODY);
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        Theme.addRow(form, 0, "Issue type", reportType);
        Theme.addRow(form, 1, "District", reportDistrict);
        Theme.addRow(form, 2, "Location details", reportLocation);
        Theme.addRow(form, 3, "Description", Theme.scroll(reportDescription));
        card.add(form, BorderLayout.CENTER);
        JButton submit = Theme.primaryButton("Submit report");
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setOpaque(false);
        south.add(submit);
        card.add(south, BorderLayout.SOUTH);
        page.add(card, BorderLayout.CENTER);
        submit.addActionListener(e -> Theme.guard(this, this::submitReport));
        return page;
    }

    private JPanel buildAlertsTab() {
        JPanel page = Theme.page();
        JButton refresh = Theme.secondaryButton("Refresh");
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(subscribedLabel, BorderLayout.CENTER);
        top.add(refresh, BorderLayout.EAST);
        JTable table = new JTable(alertModel);
        Theme.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(130);
        table.getColumnModel().getColumn(3).setPreferredWidth(420);
        page.add(top, BorderLayout.NORTH);
        page.add(Theme.scroll(table), BorderLayout.CENTER);
        refresh.addActionListener(e -> Theme.guard(this, this::refreshAlerts));
        return page;
    }

    // ---------- actions ----------

    private String pointId(JComboBox<String> districtBox) {
        String district = (String) districtBox.getSelectedItem();
        if (district == null) throw new IllegalArgumentException("Select a district first.");
        return frame.pointForDistrict(district).id();
    }

    private void loadAirQuality() throws Exception {
        String pid = pointId(aqDistrict);
        Theme.background(this, () -> {
            AirQualityReading current = c.airQuality().getCurrentReading(pid);
            try { return new AirQualityResult(current, c.airQuality().getForecast(pid), ""); }
            catch (IllegalStateException e) { return new AirQualityResult(current, List.of(), " Forecast unavailable."); }
        }, this::showAirQuality);
    }

    private void showAirQuality(AirQualityResult result) {
        AirQualityReading r = result.current();
        pm25Value.setText(String.format("%.1f", r.pm25()));
        pm10Value.setText(String.format("%.1f", r.pm10()));
        aqiValue.setText(String.valueOf(r.usAqi()));
        aqiCategory.setText(Theme.aqiCategory(r.usAqi()));
        Color col = Theme.aqiColor(r.usAqi());
        aqiValue.setForeground(col);
        aqiCategory.setForeground(col);
        aqUpdated.setText((r.cached() ? "Cached data" : "Live data") + " - reading for " + r.validTime().format(FMT)
                + ", fetched at " + r.fetchedAt().format(FMT) + "." + result.warning());
        List<Object[]> rows = new ArrayList<>();
        for (AirQualityReading f : result.forecast()) {
            rows.add(new Object[]{f.validTime().format(FMT), f.pm25(), f.pm10(), f.usAqi()});
        }
        Theme.fill(forecastModel, rows);
        refreshAlerts();
    }

    /** Fills the "All regions" table with the latest reading of every region and whether the user subscribed to it. */
    private void loadRegions() {
        loadRegions(false);
    }

    private void loadRegions(boolean forceRefresh) {
        List<String> districts = c.monitoring().activeDistricts();
        UserProfile profile = user;
        Theme.background(this, () -> regionRows(districts, profile, forceRefresh), rows -> {
            Theme.fill(regionModel, rows);
            refreshAlerts();
        });
    }

    private List<Object[]> regionRows(List<String> districts, UserProfile profile, boolean forceRefresh) {
        List<Object[]> rows = new ArrayList<>();
        for (String district : districts) {
            String subscribed = profile.isSubscribedTo(district) ? "Subscribed" : "";
            try {
                String pid = frame.pointForDistrict(district).id();
                AirQualityReading r = forceRefresh ? c.airQuality().refresh(pid) : c.airQuality().getCurrentReading(pid);
                rows.add(new Object[]{district, r.usAqi(), Theme.aqiCategory(r.usAqi()), r.validTime().format(FMT), subscribed});
            } catch (RuntimeException ex) {
                rows.add(new Object[]{district, "n/a", "No data", "-", subscribed});
            }
        }
        return rows;
    }

    private void toggleSubscription() {
        int row = regionTable.getSelectedRow();
        if (row < 0) throw new IllegalArgumentException("Select a region first.");
        String region = String.valueOf(regionModel.getValueAt(row, 0));
        List<String> subs = new ArrayList<>(user.districts());
        boolean removed = subs.removeIf(d -> d.equalsIgnoreCase(region));
        if (!removed) subs.add(region);
        user = c.user().updateProfile(user.fullName(), user.groupType(), subs);
        frame.refreshSession(user);
        showSubscriptions();
        loadRegions();
        if (row < regionModel.getRowCount()) regionTable.setRowSelectionInterval(row, row);
        refreshAlerts();
        Theme.info(this, (removed ? "Unsubscribed from " : "Subscribed to ") + region + ".");
    }

    private void showSubscriptions() {
        subscribedLabel.setText("Subscribed districts: "
                + (user.districts().isEmpty() ? "-" : String.join(", ", user.districts())));
    }

    private void findParks() {
        String pid = pointId(parkDistrict);
        Theme.background(this, () -> c.recommendation().getRecommendedParks(pid), parks -> {
            List<Object[]> rows = new ArrayList<>();
            for (Park park : parks) rows.add(new Object[]{park.name(), park.districtId(), park.latitude(), park.longitude()});
            Theme.fill(parkModel, rows);
            if (rows.isEmpty()) Theme.info(this, "No park recommendations are available for the current monitoring data.");
        });
    }

    private void submitReport() {
        String where = reportLocation.getText().trim();
        String description = reportDescription.getText().trim();
        if (description.isEmpty()) throw new IllegalArgumentException("Describe the environmental issue before submitting.");
        String text = (where.isEmpty() ? "" : "Location: " + where + "\n\n") + description;
        c.report().submitReport(new Report(user.username(), (String) reportType.getSelectedItem(), text, pointId(reportDistrict)));
        Theme.info(this, "Thank you. Your report was saved and is visible to environment officers.");
        reportLocation.setText("");
        reportDescription.setText("");
        reportType.setSelectedIndex(0);
    }

    private void refreshAlerts() {
        List<Object[]> rows = new ArrayList<>();
        for (PollutionAlert a : c.alert().getResidentAlerts(user.username())) {
            rows.add(new Object[]{a.getTimestamp().format(FMT), a.getDistrict(), a.getSeverity(), a.getMessage()});
        }
        Theme.fill(alertModel, rows);
    }

    /** Observer callback: a new alert for a subscribed district. Adds it to the table and shows a notification. */
    public void showAlert(PollutionAlert a) {
        alertModel.insertRow(0, new Object[]{a.getTimestamp().format(FMT), a.getDistrict(), a.getSeverity(), a.getMessage()});
        JOptionPane.showMessageDialog(this, a.getMessage(), "Pollution alert - " + a.getDistrict(), JOptionPane.WARNING_MESSAGE);
    }

    // ---------- helpers ----------

    private JComboBox<String> newDistrictCombo() {
        JComboBox<String> cb = new JComboBox<>();
        cb.setPrototypeDisplayValue("Petaling Jaya (long name)");
        districtCombos.add(cb);
        return cb;
    }

    private static JLabel valueLabel() {
        JLabel l = new JLabel("--");
        l.setFont(Theme.BIG);
        return l;
    }

    private record AirQualityResult(AirQualityReading current, List<AirQualityReading> forecast, String warning) { }

    private static JPanel metricCard(String caption, JLabel value, JLabel extra, JPanel card) {
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel cap = Theme.muted(caption);
        cap.setAlignmentX(LEFT_ALIGNMENT);
        value.setAlignmentX(LEFT_ALIGNMENT);
        card.add(cap);
        card.add(Box.createVerticalStrut(6));
        card.add(value);
        if (extra != null) {
            extra.setAlignmentX(LEFT_ALIGNMENT);
            card.add(Box.createVerticalStrut(4));
            card.add(extra);
        }
        return card;
    }
}
