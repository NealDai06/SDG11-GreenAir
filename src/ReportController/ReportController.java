package ReportController;
import java.util.*;
public class ReportController {
    private final ReportService service;
    public ReportController() {
        this(new ReportService(new ReportRepository()));
    }
    public ReportController(ReportService service) {
        this.service=Objects.requireNonNull(service);
    }
    public boolean validateReport(String user,String district,String description) {
        return service.validate(user,district,description);
    }
    public Report submitReport(String user,String district,String description) {
        return service.submit(user,district,description);
    }
    public boolean updateReportStatus(String id,String status) {
        return service.updateStatus(id,status);
    }
    public List<Report> getAllReports() {
        return service.all();
    }
    public List<Report> getReportsByDistrict(String district) {
        return service.byDistrict(district);
    }
    public List<Report> getReportsByUser(String user) {
        return service.byUser(user);
    }
    public static void main(String[] args) {
        ReportController c=new ReportController();
        Report r=c.submitReport("resident01","Shah Alam","Smoke detected near residential area");
        System.out.println("Submitted: "+r);
        c.updateReportStatus(r.getId(),"Reviewed");
        System.out.println("Total reports: "+c.getAllReports().size());
    }
}
