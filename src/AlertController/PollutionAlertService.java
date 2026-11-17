package AlertController;
import java.util.*;
public class PollutionAlertService {
    private final AlertRepository repository;
    private final List<AlertObserver> observers=new ArrayList<>();
    private int threshold=100;
    public PollutionAlertService(AlertRepository repository) {
        this.repository=Objects.requireNonNull(repository);
    }
    public void addObserver(AlertObserver observer) {
        if(observer!=null&&!observers.contains(observer))observers.add(observer);
    }
    public void removeObserver(AlertObserver observer) {
        observers.remove(observer);
    }
    public void setThreshold(int threshold) {
        if(threshold<0||threshold>500)throw new IllegalArgumentException("Threshold must be 0..500");
        this.threshold=threshold;
    }
    public int getThreshold() {
        return threshold;
    }
    public String severity(int aqi) {
        if(aqi<=50)return "Good";
        if(aqi<=100)return "Moderate";
        if(aqi<=150)return "Unhealthy for Sensitive Groups";
        if(aqi<=200)return "Unhealthy";
        if(aqi<=300)return "Very Unhealthy";
        return "Hazardous";
    }
    public PollutionAlert check(String district,int aqi) {
        if(district==null||district.isBlank())throw new IllegalArgumentException("District required");
        if(aqi<0||aqi>500)throw new IllegalArgumentException("AQI must be 0..500");
        if(aqi<=threshold)return null;
        PollutionAlert alert=new PollutionAlert(district.trim(),aqi,severity(aqi));
        repository.save(alert);
        for(AlertObserver o:new ArrayList<>(observers))o.onAlert(alert);
        return alert;
    }
    public List<PollutionAlert> history() {
        return repository.findAll();
    }
    public List<PollutionAlert> historyByDistrict(String district) {
        return repository.findByDistrict(district);
    }
}
