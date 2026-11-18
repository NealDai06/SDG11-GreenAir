package ReportController;

import java.util.List;
import java.util.Objects;

public class ReportService {
    private final ReportRepository reports;
    public ReportService(ReportRepository reports) {
        this.reports = Objects.requireNonNull(reports);
    }

    public void submitReport(Report report) {
        Objects.requireNonNull(report, "Report cannot be null");
        if (blank(report.getUsername()) || blank(report.getType())
                || blank(report.getDescription()) || blank(report.getPointId())) {
            throw new IllegalArgumentException("Report fields cannot be empty");
        }
        // validated is reserved for later verification, not input validation.
        reports.save(report);
    }

    public void updateStatus(String reportId, String status) {
        if (status == null || !Report.STATUSES.contains(status))
            throw new IllegalArgumentException("Invalid report status");
        Report report = reports.findById(reportId);
        if (report == null) throw new IllegalArgumentException("Report not found");
        String previous = report.getStatus();
        report.setStatus(status);
        try { reports.save(report); }
        catch (RuntimeException ex) { report.setStatus(previous); throw ex; }
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    public List<Report> getAllReports() { return reports.findAll(); }
    public Report getReportById(String reportId) { return reports.findById(reportId); }
}
