package ReportController;

import java.time.LocalDateTime;
import java.util.UUID;

public class Report {
    private final String reportId;
    private final String username;
    private final String type;
    private final String description;
    private final String pointId;
    private final LocalDateTime submittedAt;
    private boolean validated;
    public static final java.util.Set<String> STATUSES = java.util.Set.of(
            "Submitted", "Under review", "Action taken", "Resolved", "Dismissed");
    private String status = "Submitted";

    public Report(String username, String type, String description, String pointId) {
        this(UUID.randomUUID().toString(), username, type, description, pointId,
                LocalDateTime.now(), false, "Submitted");
    }

    /** Restores stored identity, time, verification and processing status. */
    public Report(String reportId, String username, String type, String description,
                  String pointId, LocalDateTime submittedAt, boolean validated, String status) {
        this.reportId = java.util.Objects.requireNonNull(reportId);
        this.username = username;
        this.type = type;
        this.description = description;
        this.pointId = pointId;
        this.submittedAt = java.util.Objects.requireNonNull(submittedAt);
        this.validated = validated;
        setStatus(status);
    }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        if (status == null || !STATUSES.contains(status))
            throw new IllegalArgumentException("Invalid report status");
        this.status = status;
    }

    public String getReportId() { return reportId; }
    public String getUsername() { return username; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public String getPointId() { return pointId; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public boolean isValidated() { return validated; }
    public void setValidated(boolean validated) { this.validated = validated; }

    /** Checks the mandatory fields used when submitting a citizen report. */
    public boolean validate() {
        return username != null && !username.isBlank() && type != null && !type.isBlank()
                && description != null && !description.isBlank() && pointId != null && !pointId.isBlank();
    }
}
