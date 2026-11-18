package ReportController;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

/** Local JSON storage; use one repository instance per file in this desktop app. */
public class ReportRepository {
    private final JsonFile file;
    private final List<Report> reports = new ArrayList<>();
    /** In-memory repository for isolated demos and tests. */
    public ReportRepository() { file = null; }
    public ReportRepository(Path path) {
        file = new JsonFile(Objects.requireNonNull(path));
        for (Map<String, String> row : file.load()) {
            reports.add(new Report(row.getOrDefault("reportId", row.get("id")),
                    row.get("username"), row.get("type"), row.get("description"), row.get("pointId"),
                    LocalDateTime.parse(row.get("submittedAt")),
                    Boolean.parseBoolean(row.getOrDefault("validated", "false")),
                    row.getOrDefault("status", "Submitted")));
        }
    }
    public void save(Report report) {
        Objects.requireNonNull(report);
        int index = -1;
        for (int i = 0; i < reports.size(); i++)
            if (reports.get(i).getReportId().equals(report.getReportId())) { index = i; break; }
        Report previous = index < 0 ? null : reports.get(index);
        if (index < 0) reports.add(report); else reports.set(index, report);
        try { persist(); }
        catch (RuntimeException ex) {
            if (index < 0) reports.remove(report); else reports.set(index, previous);
            throw ex;
        }
    }
    private void persist() {
        if (file == null) return;
        List<Map<String, String>> rows = new ArrayList<>();
        for (Report r : reports) rows.add(JsonFile.row(
                "reportId", r.getReportId(), "username", r.getUsername(), "type", r.getType(),
                "description", r.getDescription(), "pointId", r.getPointId(),
                "submittedAt", r.getSubmittedAt().toString(),
                "validated", Boolean.toString(r.isValidated()), "status", r.getStatus()));
        file.save(rows);
    }
    public List<Report> findAll() { return new ArrayList<>(reports); }
    public Report findById(String reportId) {
        for (Report report : reports) if (report.getReportId().equals(reportId)) return report;
        return null;
    }
}
