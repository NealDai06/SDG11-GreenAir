package AlertController;

import java.time.LocalDateTime;
import java.util.UUID;

public class PollutionAlert {
    private final String alertId;
    private final String district;
    private final String message;
    private final LocalDateTime timestamp;
    private final String severity;

    public PollutionAlert(String district, String message, String severity) {
        this.alertId = UUID.randomUUID().toString();
        this.district = district;
        this.message = message;
        this.severity = severity;
        this.timestamp = LocalDateTime.now();
    }

    public String getAlertId() { return alertId; }
    public String getDistrict() { return district; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getSeverity() { return severity; }
    @Override public String toString() {
        return district + " | " + severity + " | " + message;
    }
}

