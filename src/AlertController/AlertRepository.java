package AlertController;
import java.util.*;
public class AlertRepository {
    private final List<PollutionAlert> alerts=new ArrayList<>();
    public void save(PollutionAlert alert) {
        alerts.add(Objects.requireNonNull(alert));
    }
    public List<PollutionAlert> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(alerts));
    }
    public List<PollutionAlert> findByDistrict(String district) {
        List<PollutionAlert> result=new ArrayList<>();
        if(district!=null)for(PollutionAlert a:alerts)if(a.getDistrict().equalsIgnoreCase(district.trim()))result.add(a);
        return result;
    }
}
