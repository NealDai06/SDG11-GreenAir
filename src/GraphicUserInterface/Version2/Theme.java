package greenair.view;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

/** Shared look-and-feel helpers for the GreenAir Klang Valley view layer. */
public final class Theme {
    static final Color PRIMARY = new Color(0x2E7D32);
    static final Color PRIMARY_DARK = new Color(0x1B5E20);
    static final Color BG = new Color(0xF3F7F3);
    static final Color CARD = Color.WHITE;
    static final Color TEXT = new Color(0x263238);
    static final Color MUTED = new Color(0x607D8B);
    static final Color LINE = new Color(0xCFD8DC);
    static final Font TITLE = new Font("SansSerif", Font.BOLD, 24);
    static final Font HEADING = new Font("SansSerif", Font.BOLD, 15);
    static final Font BODY = new Font("SansSerif", Font.PLAIN, 13);
    static final Font BIG = new Font("SansSerif", Font.BOLD, 30);

    private Theme() { }

    static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(PRIMARY);
        b.setForeground(Color.WHITE);
        b.setFont(HEADING);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8, 18, 8, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(Color.WHITE);
        b.setForeground(PRIMARY_DARK);
        b.setFont(BODY);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(new CompoundBorder(new LineBorder(PRIMARY, 1, true), new EmptyBorder(6, 14, 6, 14)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(TITLE);
        l.setForeground(PRIMARY_DARK);
        return l;
    }

    static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(HEADING);
        l.setForeground(TEXT);
        return l;
    }

    static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BODY);
        l.setForeground(MUTED);
        return l;
    }

    /** White rounded-looking panel with padding. */
    static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(CARD);
        p.setBorder(new CompoundBorder(new LineBorder(LINE, 1, true), new EmptyBorder(16, 18, 16, 18)));
        return p;
    }

    static JPanel page() {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));
        return p;
    }

    /** Top banner with application title, signed-in user and logout button. */
    static JPanel banner(String subtitle, JLabel userLabel, JButton logout) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(PRIMARY_DARK);
        p.setBorder(new EmptyBorder(12, 18, 12, 18));
        JLabel t = new JLabel("GreenAir Klang Valley  |  " + subtitle);
        t.setForeground(Color.WHITE);
        t.setFont(new Font("SansSerif", Font.BOLD, 18));
        userLabel.setForeground(new Color(0xC8E6C9));
        userLabel.setFont(BODY);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(userLabel);
        right.add(logout);
        p.add(t, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    static void styleTable(JTable t) {
        t.setFont(BODY);
        t.setRowHeight(28);
        t.setGridColor(LINE);
        t.setSelectionBackground(new Color(0xC8E6C9));
        t.setSelectionForeground(TEXT);
        t.setFillsViewportHeight(true);
        JTableHeader h = t.getTableHeader();
        h.setFont(HEADING);
        h.setBackground(new Color(0xE8F5E9));
        h.setForeground(PRIMARY_DARK);
        h.setReorderingAllowed(false);
    }

    static JScrollPane scroll(JComponent c) {
        JScrollPane s = new JScrollPane(c);
        s.setBorder(new LineBorder(LINE));
        s.getViewport().setBackground(Color.WHITE);
        return s;
    }

    static DefaultTableModel readOnlyModel(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    static void fill(DefaultTableModel m, List<Object[]> rows) {
        m.setRowCount(0);
        for (Object[] r : rows) m.addRow(r);
    }

    /** Adds a "label: component" row to a GridBagLayout form. */
    static void addRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(6, 4, 6, 8);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridx = 0;
        form.add(heading(label), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, c);
    }

    /** US AQI category colour (presentation only). */
    static Color aqiColor(int aqi) {
        if (aqi <= 50) return new Color(0x43A047);
        if (aqi <= 100) return new Color(0xF9A825);
        if (aqi <= 150) return new Color(0xEF6C00);
        if (aqi <= 200) return new Color(0xD32F2F);
        if (aqi <= 300) return new Color(0x7B1FA2);
        return new Color(0x6D1B3A);
    }

    public static String aqiCategory(int aqi) {
        if (aqi <= 50) return "Good";
        if (aqi <= 100) return "Moderate";
        if (aqi <= 150) return "Unhealthy for sensitive groups";
        if (aqi <= 200) return "Unhealthy";
        if (aqi <= 300) return "Very unhealthy";
        return "Hazardous";
    }

    static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "GreenAir Klang Valley", JOptionPane.INFORMATION_MESSAGE);
    }

    static void error(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Please check your input", JOptionPane.WARNING_MESSAGE);
    }
}
