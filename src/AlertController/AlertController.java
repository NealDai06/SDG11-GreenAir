
package AlertController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlertController {

    // Observer Pattern
    public interface AlertObserver {
        void onAlert(PollutionAlert alert);
    }

    public static class PollutionAlert {
        private final String district;
        private final int aqi;
        private final String severity;
        private final LocalDateTime timestamp;

        public PollutionAlert(String district, int aqi,
                              String severity) {
            this.district = district;
            this.aqi = aqi;
            this.severity = severity;
            this.timestamp = LocalDateTime.now();
        }

        public String getDistrict() {
            return district;
        }

        public int getAqi() {
            return aqi;
        }

        public String getSeverity() {
            return severity;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return timestamp + " | " + district
                    + " | AQI: " + aqi
                    + " | " + severity;
        }
    }

    private final List<AlertObserver> observers =
            new ArrayList<>();

    private final List<PollutionAlert> alertHistory =
            new ArrayList<>();

    private int alertThreshold = 100;

    // Register an observer
    public void addObserver(AlertObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    // Remove an observer
    public void removeObserver(AlertObserver observer) {
        observers.remove(observer);
    }

    // Configure AQI threshold
    public void setAlertThreshold(int threshold) {
        if (threshold < 0 || threshold > 500) {
            throw new IllegalArgumentException(
                    "Threshold must be between 0 and 500");
        }
        this.alertThreshold = threshold;
    }

    public int getAlertThreshold() {
        return alertThreshold;
    }

    // Determine pollution severity
    public String determineSeverity(int aqi) {
        if (aqi <= 50) return "Good";
        if (aqi <= 100) return "Moderate";
        if (aqi <= 150) return "Unhealthy for Sensitive Groups";
        if (aqi <= 200) return "Unhealthy";
        if (aqi <= 300) return "Very Unhealthy";
        return "Hazardous";
    }

    // Check air quality and trigger an alert
    public PollutionAlert checkAirQuality(
            String district, int aqi) {

        if (district == null || district.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "District cannot be empty");
        }

        if (aqi < 0 || aqi > 500) {
            throw new IllegalArgumentException(
                    "AQI must be between 0 and 500");
        }

        if (aqi <= alertThreshold) {
            return null;
        }

        PollutionAlert alert = new PollutionAlert(
                district.trim(),
                aqi,
                determineSeverity(aqi)
        );

        alertHistory.add(alert);
        notifyObservers(alert);

        return alert;
    }

    // Notify registered observers
    private void notifyObservers(PollutionAlert alert) {
        for (AlertObserver observer :
                new ArrayList<>(observers)) {
            observer.onAlert(alert);
        }
    }

    // Retrieve alert history
    public List<PollutionAlert> getAlertHistory() {
        return Collections.unmodifiableList(
                new ArrayList<>(alertHistory));
    }

    // Retrieve alerts by district
    public List<PollutionAlert> getAlertsByDistrict(
            String district) {

        List<PollutionAlert> result = new ArrayList<>();

        if (district == null) {
            return result;
        }

        for (PollutionAlert alert : alertHistory) {
            if (alert.getDistrict()
                    .equalsIgnoreCase(district.trim())) {
                result.add(alert);
            }
        }

        return result;
    }

    // Standalone test
    public static void main(String[] args) {
        AlertController controller = new AlertController();

        controller.addObserver(alert ->
                System.out.println("ALERT: " + alert));

        controller.checkAirQuality("Shah Alam", 165);
        controller.checkAirQuality("Klang", 85);
        controller.checkAirQuality("Petaling Jaya", 220);

        System.out.println("Total alerts: "
                + controller.getAlertHistory().size());
    }
}
