package ReportController;
import java.util.*;
public class ReportService {
    private final ReportRepository repository;
    public ReportService(ReportRepository repository) {
        this.repository=Objects.requireNonNull(repository);
    }
    public boolean validate(String user,String district,String description) {
        return user!=null&&!user.isBlank()&&district!=null&&!district.isBlank()&&description!=null&&!description.isBlank();
    }
    public Report submit(String user,String district,String description) {
        if(!validate(user,district,description))throw new IllegalArgumentException("All fields required");
        Report r=new Report(user.trim(),district.trim(),description.trim());
        repository.save(r);
        return r;
    }
    public boolean updateStatus(String id,String status) {
        if(!Arrays.asList("Pending","Reviewed","Resolved").contains(status))throw new IllegalArgumentException("Invalid status");
        return repository.updateStatus(id,status);
    }
    public List<Report> all() {
        return repository.findAll();
    }
    public List<Report> byDistrict(String district) {
        return repository.findByDistrict(district);
    }
    public List<Report> byUser(String user) {
        return repository.findByUser(user);
    }
}
