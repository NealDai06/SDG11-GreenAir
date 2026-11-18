package ReportController;

import java.util.List;
import java.util.Objects;

public class ReportController {
    private final ReportService reportService;

    public ReportController() {
        this(new ReportService(new ReportRepository()));
    }

    public ReportController(ReportService reportService) {
        this.reportService = Objects.requireNonNull(reportService);
    }

    public void submitReport(Report report) {
        reportService.submitReport(report);
    }

    public void updateStatus(String reportId, String status) {
        reportService.updateStatus(reportId, status);
    }

    public List<Report> getAllReports() {
        return reportService.getAllReports();
    }

    public Report getReportById(String reportId) {
        return reportService.getReportById(reportId);
    }

    public static void main(String[] args) {
        ReportController controller = new ReportController();
        Report report = new Report("resident01", "Smoke", "Smoke near park", "MP001");
        controller.submitReport(report);
        controller.updateStatus(report.getReportId(), "Under review");
        System.out.println("Status: " + report.getStatus());
        System.out.println("Reports: " + controller.getAllReports().size());
        System.out.println("Found: " + (controller.getReportById(report.getReportId()) != null));
    }
}

