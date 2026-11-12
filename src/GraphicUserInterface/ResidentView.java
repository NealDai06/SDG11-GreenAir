package greenair.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Resident dashboard: air quality, park recommendations, advice, reports and alerts. */
public class ResidentView extends JPanel {

    /** Citizen report captured by the report tab. */
    public record ReportForm(String type, String district, String location, String description) { }

    private static final String[] GROUPS = {"General public", "Children", "Elderly"};
    private static final String[] REPORT_TYPES = {"Open burning", "Smog / haze", "Park maintenance", "Other"};

    private final JLabel userLabel = new JLabel();
    private final JButton logoutButton = Theme.secondaryButton("Log out");
    private final List<JComboBox<String>> districtCombos = new ArrayList<>();

    // Air quality tab
    private final JComboBox<String> aqDistrict = newDistrictCombo();
    private final JLabel pm25Value = valueLabel();
    private final JLabel pm10Value = valueLabel();
    private final JLabel aqiValue = valueLabel();
    private final JLabel aqiCategory = Theme.heading(" ");
    private final JPanel aqiCard = Theme.card();
    private final JLabel aqUpdated = Theme.muted("No data loaded yet.");
    private final DefaultTableModel forecastModel =
            Theme.readOnlyModel("Time", "PM2.5 (µg/m³)", "PM10 (µg/m³)", "US AQI");

    // Parks tab
    private final JComboBox<String> parkDistrict = newDistrictCombo();
    private final JSpinner distanceSpinner = new JSpinner(new SpinnerNumberModel(10.0, 1.0, 50.0, 1.0));
    private final DefaultTableModel parkModel =
            Theme.readOnlyModel("Park", "District", "Distance (km)", "US AQI", "Suitability");

    // Advice tab
    private final JComboBox<String> adviceGroup = new JComboBox<>(GROUPS);
    private final JComboBox<String> adviceDistrict = newDistrictCombo();
    private final JTextArea adviceArea = new JTextArea(8, 40);

    // Report tab
    private final JComboBox<String> reportType = new JComboBox<>(REPORT_TYPES);
    private final JComboBox<String> reportDistrict = newDistrictCombo();
    private final JTextField reportLocation = new JTextField(30);
    private final JTextArea reportDescription = new JTextArea(6, 30);

    // Alerts tab
    private final DefaultTableModel alertModel =
            Theme.readOnlyModel("Time", "District", "Pollutant", "Level", "Message");
    private final JLabel subscribedLabel = Theme.muted("Subscribed districts: -");

    private Consumer<String> onFetchAirQuality = d -> { };
    private BiConsumer<String, Double> onFindParks = (d, km) -> { };
    private BiConsumer<String, String> onGetAdvice = (g, d) -> { };
    private Consumer<ReportForm> onSubmitReport = r -> { };
    private Runnable onRefreshAlerts = () -> { };
    private Runnable onLogout = () -> { };

    public ResidentView() {
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        add(Theme.banner("Resident", userLabel, logoutButton), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.HEADING);
        tabs.addTab("Air quality", buildAirQualityTab());
        tabs.addTab("Park recommendations", buildParksTab());
        tabs.addTab("Activity advice", buildAdviceTab());
        tabs.addTab("Report an issue", buildReportTab());
        tabs.addTab("Alerts", buildAlertsTab());
        add(tabs, BorderLayout.CENTER);

        logoutButton.addActionListener(e -> onLogout.run());
    }

    // ---------- tab builders ----------

    private JPanel buildAirQualityTab() {
        JPanel page = Theme.page();
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        top.setOpaque(false);
        JButton fetch = Theme.primaryButton("Check air quality");
        top.add(Theme.heading("District"));
        top.add(aqDistrict);
        top.add(fetch);
        top.add(aqUpdated);
        fetch.addActionListener(e -> {
            String d = (String) aqDistrict.getSelectedItem();
            if (d == null) { Theme.error(this, "Select a district first."); return; }
            onFetchAirQuality.accept(d);
        });

        JPanel cards = new JPanel(new GridLayout(1, 3, 14, 0));
        cards.setOpaque(false);
        cards.add(metricCard("PM2.5 (µg/m³)", pm25Value, null, Theme.card()));
        cards.add(metricCard("PM10 (µg/m³)", pm10Value, null, Theme.card()));
        cards.add(metricCard("US AQI", aqiValue, aqiCategory, aqiCard));

        JPanel north = new JPanel(new BorderLayout(0, 12));
        north.setOpaque(false);
        north.add(top, BorderLayout.NORTH);
        north.add(cards, BorderLayout.CENTER);

        JTable table = new JTable(forecastModel);
        Theme.styleTable(table);
        table.getColumnModel().getColumn(3).setCellRenderer(new AqiCellRenderer());
        JPanel forecast = new JPanel(new BorderLayout(0, 6));
        forecast.setOpaque(false);
        forecast.add(Theme.heading("Forecast"), BorderLayout.NORTH);
        forecast.add(Theme.scroll(table), BorderLayout.CENTER);

        page.add(north, BorderLayout.NORTH);
        page.add(forecast, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildParksTab() {
        JPanel page = Theme.page();
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        top.setOpaque(false);
        JButton find = Theme.primaryButton("Find parks");
        top.add(Theme.heading("Near district"));
        top.add(parkDistrict);
        top.add(Theme.heading("Within (km)"));
        top.add(distanceSpinner);
        top.add(find);
        find.addActionListener(e -> {
            String d = (String) parkDistrict.getSelectedItem();
            if (d == null) { Theme.error(this, "Select a district first."); return; }
            onFindParks.accept(d, ((Number) distanceSpinner.getValue()).doubleValue());
        });
        JTable table = new JTable(parkModel);
        Theme.styleTable(table);
        table.getColumnModel().getColumn(3).setCellRenderer(new AqiCellRenderer());
        page.add(top, BorderLayout.NORTH);
        page.add(Theme.scroll(table), BorderLayout.CENTER);
        page.add(Theme.muted("Parks are ranked by air quality and distance from the selected district."), BorderLayout.SOUTH);
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

        get.addActionListener(e -> {
            String d = (String) adviceDistrict.getSelectedItem();
            if (d == null) { Theme.error(this, "Select a district first."); return; }
            onGetAdvice.accept((String) adviceGroup.getSelectedItem(), d);
        });
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

        submit.addActionListener(e -> {
            String d = (String) reportDistrict.getSelectedItem();
            if (d == null || reportDescription.getText().isBlank()) {
                Theme.error(this, "Choose a district and describe the issue.");
                return;
            }
            onSubmitReport.accept(new ReportForm((String) reportType.getSelectedItem(), d,
                    reportLocation.getText().trim(), reportDescription.getText().trim()));
        });
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
        table.getColumnModel().getColumn(4).setPreferredWidth(380);
        page.add(top, BorderLayout.NORTH);
        page.add(Theme.scroll(table), BorderLayout.CENTER);
        refresh.addActionListener(e -> onRefreshAlerts.run());
        return page;
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

    // ---------- API for controllers ----------

    public void setUserInfo(String name, String groupType) {
        userLabel.setText("Signed in as " + name + " (" + groupType + ")");
        adviceGroup.setSelectedItem(groupType);
    }

    public void setDistricts(List<String> districts) {
        for (JComboBox<String> cb : districtCombos) {
            cb.removeAllItems();
            districts.forEach(cb::addItem);
        }
    }

    public void setSubscribedDistricts(List<String> districts) {
        subscribedLabel.setText("Subscribed districts: " + (districts.isEmpty() ? "-" : String.join(", ", districts)));
    }

    public void showCurrentReading(String district, double pm25, double pm10, int aqi, String updated) {
        pm25Value.setText(String.format("%.1f", pm25));
        pm10Value.setText(String.format("%.1f", pm10));
        aqiValue.setText(String.valueOf(aqi));
        aqiCategory.setText(Theme.aqiCategory(aqi));
        Color c = Theme.aqiColor(aqi);
        aqiValue.setForeground(c);
        aqiCategory.setForeground(c);
        aqUpdated.setText(district + " - updated " + updated);
    }

    /** Rows: {time, pm2.5, pm10, aqi}. */
    public void setForecast(List<Object[]> rows) {
        Theme.fill(forecastModel, rows);
    }

    /** Rows: {park, district, distanceKm, aqi, suitability}. */
    public void setParks(List<Object[]> rows) { Theme.fill(parkModel, rows); }

    public void showAdvice(String text) { adviceArea.setText(text); adviceArea.setCaretPosition(0); }

    /** Rows: {time, district, pollutant, level, message}. */
    public void setAlerts(List<Object[]> rows) { Theme.fill(alertModel, rows); }

    /** Adds a new alert at the top of the table and pops up a notification (Observer callback target). */
    public void showAlert(Object[] row) {
        SwingUtilities.invokeLater(() -> {
            alertModel.insertRow(0, row);
            JOptionPane.showMessageDialog(this, row[4], "Pollution alert - " + row[1], JOptionPane.WARNING_MESSAGE);
        });
    }

    public void showMessage(String message) { Theme.info(this, message); }

    public void clearReportForm() {
        reportLocation.setText("");
        reportDescription.setText("");
        reportType.setSelectedIndex(0);
    }

    public void setOnFetchAirQuality(Consumer<String> h) { onFetchAirQuality = h; }
    public void setOnFindParks(BiConsumer<String, Double> h) { onFindParks = h; }
    public void setOnGetAdvice(BiConsumer<String, String> h) { onGetAdvice = h; }
    public void setOnSubmitReport(Consumer<ReportForm> h) { onSubmitReport = h; }
    public void setOnRefreshAlerts(Runnable h) { onRefreshAlerts = h; }
    public void setOnLogout(Runnable h) { onLogout = h; }

    /** Colours an AQI cell by its US AQI category. */
    static class AqiCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            setHorizontalAlignment(CENTER);
            if (!sel && v instanceof Number n) {
                comp.setBackground(Theme.aqiColor(n.intValue()));
                comp.setForeground(Color.WHITE);
            } else if (!sel) {
                comp.setBackground(Color.WHITE);
                comp.setForeground(Theme.TEXT);
            }
            return comp;
        }
    }
}
