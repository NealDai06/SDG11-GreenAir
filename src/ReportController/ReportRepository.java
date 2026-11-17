package ReportController;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
public class ReportRepository {
    private final Path path;
    private final List<Report> reports=new ArrayList<>();
    public ReportRepository() {
        this(Path.of("data","reports.tsv"));
    }
    public ReportRepository(Path path) {
        this.path=Objects.requireNonNull(path);
        load();
    }
    private String enc(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
    private String dec(String value) {
        return new String(Base64.getDecoder().decode(value),StandardCharsets.UTF_8);
    }
    public void load() {
        if(!Files.exists(path))return;
        try {
            List<Report> loaded=new ArrayList<>();
            for(String line:Files.readAllLines(path,StandardCharsets.UTF_8)) {
                if(line.isBlank())continue;
                String[] p=line.split("\t",-1);
                if(p.length!=6)throw new IOException("Invalid report record");
                loaded.add(new Report(dec(p[0]),dec(p[1]),dec(p[2]),dec(p[3]),LocalDateTime.parse(p[4]),dec(p[5])));
            }
            reports.clear();
            reports.addAll(loaded);
        }
        catch(IOException|IllegalArgumentException e) {
            throw new IllegalStateException("Cannot read report file: "+path,e);
        }
    }
    private void persist() {
        List<String> lines=new ArrayList<>();
        for(Report r:reports)lines.add(enc(r.getId())+"\t"+enc(r.getUsername())+"\t"+enc(r.getDistrict())+"\t"+enc(r.getDescription())+"\t"+r.getCreatedAt()+"\t"+enc(r.getStatus()));
        try {
            Path parent=path.toAbsolutePath().getParent();
            if(parent!=null)Files.createDirectories(parent);
            Files.write(path,lines,StandardCharsets.UTF_8);
        }
        catch(IOException e) {
            throw new IllegalStateException("Cannot save report file: "+path,e);
        }
    }
    public void save(Report report) {
        reports.add(Objects.requireNonNull(report));
        try {
            persist();
        }
        catch(RuntimeException e) {
            reports.remove(report);
            throw e;
        }
    }
    public boolean updateStatus(String id,String status) {
        for(Report r:reports)if(r.getId().equals(id)) {
            String old=r.getStatus();
            r.setStatus(status);
            try {
                persist();
            }
            catch(RuntimeException e) {
                r.setStatus(old);
                throw e;
            }
            return true;
        }
        return false;
    }
    public List<Report> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(reports));
    }
    public List<Report> findByDistrict(String district) {
        List<Report> out=new ArrayList<>();
        if(district!=null)for(Report r:reports)if(r.getDistrict().equalsIgnoreCase(district.trim()))out.add(r);
        return out;
    }
    public List<Report> findByUser(String user) {
        List<Report> out=new ArrayList<>();
        if(user!=null)for(Report r:reports)if(r.getUsername().equalsIgnoreCase(user.trim()))out.add(r);
        return out;
    }
}
