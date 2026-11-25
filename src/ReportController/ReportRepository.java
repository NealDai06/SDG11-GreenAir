package ReportController;

import AirQualityController.AirQualityJson;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** Local JSON storage; use one repository instance per file in this desktop app. */
public class ReportRepository {
    private final Path file;
    private final List<Report> reports = new ArrayList<>();
    /** In-memory repository for isolated demos and tests. */
    public ReportRepository() { file = null; }
    public ReportRepository(Path path) {
        file = Objects.requireNonNull(path).toAbsolutePath().normalize();
        load();
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
        String json = reports.stream().map(ReportRepository::encode).collect(Collectors.joining(",\n", "[\n", "\n]\n"));
        Path temporary = null;
        try {
            Files.createDirectories(file.getParent());
            temporary = Files.createTempFile(file.getParent(), "reports-", ".tmp");
            Files.writeString(temporary, json);
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException e) {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException cleanupError) { e.addSuppressed(cleanupError); }
            }
            throw new UncheckedIOException("Could not save the citizen reports.", e);
        }
    }

    private void load() {
        if (!Files.exists(file)) return;
        try {
            for (Object value : AirQualityJson.array(AirQualityJson.parse(Files.readString(file)))) {
                Map<?, ?> row = AirQualityJson.object(value);
                reports.add(new Report(text(row, "reportId", text(row, "id", "")),
                        text(row, "username", ""), text(row, "type", ""), text(row, "description", ""),
                        text(row, "pointId", ""), LocalDateTime.parse(text(row, "submittedAt", "")),
                        Boolean.parseBoolean(text(row, "validated", "false")), text(row, "status", "Submitted")));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the citizen reports.", e);
        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            throw new IllegalStateException("The local citizen report file is invalid.", e);
        }
    }

    private static String text(Map<?, ?> row, String key, String fallback) {
        Object value = row.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static String encode(Report report) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("reportId", report.getReportId());
        row.put("username", report.getUsername());
        row.put("type", report.getType());
        row.put("description", report.getDescription());
        row.put("pointId", report.getPointId());
        row.put("submittedAt", report.getSubmittedAt().toString());
        row.put("validated", Boolean.toString(report.isValidated()));
        row.put("status", report.getStatus());
        return row.entrySet().stream().map(entry -> AirQualityJson.quote(entry.getKey())
                + ": " + AirQualityJson.quote(entry.getValue())).collect(Collectors.joining(", ", "  {", "}"));
    }
    public List<Report> findAll() { return new ArrayList<>(reports); }
    public Report findById(String reportId) {
        for (Report report : reports) if (report.getReportId().equals(reportId)) return report;
        return null;
    }
}
