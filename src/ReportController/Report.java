
package ReportController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ReportController {

    public static class CitizenReport {
        private final String reportId;
        private final String username;
        private final String district;
        private final String description;
        private final LocalDateTime timestamp;
        private String status;

        public CitizenReport(String username,
                             String district,
                             String description) {
            this(UUID.randomUUID().toString(),
                    username, district, description,
                    LocalDateTime.now(), "Pending");
        }

        private CitizenReport(String reportId,
                              String username,
                              String district,
                              String description,
                              LocalDateTime timestamp,
                              String status) {
            this.reportId = reportId;
            this.username = username;
            this.district = district;
            this.description = description;
            this.timestamp = timestamp;
            this.status = status;
        }

        public String getReportId() {
            return reportId;
        }

        public String getUsername() {
            return username;
        }

        public String getDistrict() {
            return district;
        }

        public String getDescription() {
            return description;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public String getStatus() {
            return status;
        }

        private void setStatus(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return reportId + " | " + username
                    + " | " + district
                    + " | " + description
                    + " | " + status;
        }
    }

    private final List<CitizenReport> reports =
            new ArrayList<>();

    private final Path storagePath;

    public ReportController() {
        this(Path.of("data", "citizen_reports.txt"));
    }

    public ReportController(Path storagePath) {
        if (storagePath == null) {
            throw new IllegalArgumentException(
                    "Storage path cannot be null");
        }

        this.storagePath = storagePath;
        loadReports();
    }

    // Validate report input
    public boolean validateReport(
            String username,
            String district,
            String description) {

        return username != null
                && !username.trim().isEmpty()
                && district != null
                && !district.trim().isEmpty()
                && description != null
                && !description.trim().isEmpty();
    }

    // Submit a citizen report
    public CitizenReport submitReport(
            String username,
            String district,
            String description) {

        if (!validateReport(username, district, description)) {
            throw new IllegalArgumentException(
                    "All report fields are required");
        }

        CitizenReport report = new CitizenReport(
                username.trim(),
                district.trim(),
                description.trim()
        );

        reports.add(report);
        try {
            saveReports();
        } catch (IllegalStateException e) {
            reports.remove(report);
            throw e;
        }

        return report;
    }

    // Retrieve all reports
    public List<CitizenReport> getAllReports() {
        return Collections.unmodifiableList(
                new ArrayList<>(reports));
    }

    // Retrieve reports by district
    public List<CitizenReport> getReportsByDistrict(
            String district) {

        List<CitizenReport> result = new ArrayList<>();

        if (district == null) {
            return result;
        }

        for (CitizenReport report : reports) {
            if (report.getDistrict()
                    .equalsIgnoreCase(district.trim())) {
                result.add(report);
            }
        }

        return result;
    }

    // Retrieve reports submitted by a user
    public List<CitizenReport> getReportsByUser(
            String username) {

        List<CitizenReport> result = new ArrayList<>();

        if (username == null) {
            return result;
        }

        for (CitizenReport report : reports) {
            if (report.getUsername()
                    .equalsIgnoreCase(username.trim())) {
                result.add(report);
            }
        }

        return result;
    }

    // Update report status
    public boolean updateReportStatus(
            String reportId, String newStatus) {

        if (!"Pending".equals(newStatus)
                && !"Reviewed".equals(newStatus)
                && !"Resolved".equals(newStatus)) {
            throw new IllegalArgumentException(
                    "Invalid report status");
        }

        for (CitizenReport report : reports) {
            if (report.getReportId().equals(reportId)) {
                String oldStatus = report.getStatus();
                report.setStatus(newStatus);

                try {
                    saveReports();
                } catch (IllegalStateException e) {
                    report.setStatus(oldStatus);
                    throw e;
                }

                return true;
            }
        }

        return false;
    }

    // Encode a text field for safe file storage
    private String encode(String value) {
        return Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(
                Base64.getDecoder().decode(value),
                StandardCharsets.UTF_8);
    }

    // Save reports to a local text file
    public void saveReports() {
        List<String> lines = new ArrayList<>();

        for (CitizenReport report : reports) {
            lines.add(
                    encode(report.getReportId()) + "\t"
                    + encode(report.getUsername()) + "\t"
                    + encode(report.getDistrict()) + "\t"
                    + encode(report.getDescription()) + "\t"
                    + report.getTimestamp() + "\t"
                    + encode(report.getStatus())
            );
        }

        try {
            Path parent = storagePath.toAbsolutePath()
                    .getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.write(
                    storagePath,
                    lines,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to save reports", e);
        }
    }

    // Load saved reports
    public void loadReports() {
        if (!Files.exists(storagePath)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(
                    storagePath, StandardCharsets.UTF_8);

            List<CitizenReport> loaded = new ArrayList<>();

            for (String line : lines) {
                if (line.isBlank()) continue;

                String[] fields = line.split("\t", -1);

                if (fields.length != 6) {
                    throw new IOException(
                            "Invalid report record");
                }

                loaded.add(new CitizenReport(
                        decode(fields[0]),
                        decode(fields[1]),
                        decode(fields[2]),
                        decode(fields[3]),
                        LocalDateTime.parse(fields[4]),
                        decode(fields[5])
                ));
            }

            reports.clear();
            reports.addAll(loaded);

        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException(
                    "Failed to load reports", e);
        }
    }

    // Standalone test
    public static void main(String[] args) {
        ReportController controller =
                new ReportController();

        CitizenReport report = controller.submitReport(
                "resident01",
                "Shah Alam",
                "Heavy smoke detected near residential area"
        );

        System.out.println("Report submitted: " + report);

        controller.updateReportStatus(
                report.getReportId(), "Reviewed");

        System.out.println("Total reports: "
                + controller.getAllReports().size());
    }
}
