package AlertController;
import java.util.*;
public class AlertController {
    private final PollutionAlertService service;
    public AlertController() {
        this(new PollutionAlertService(new AlertRepository()));
    }
    public AlertController(PollutionAlertService service) {
        this.service=Objects.requireNonNull(service);
    }
    public void addObserver(AlertObserver observer) {
        service.addObserver(observer);
    }
    public void removeObserver(AlertObserver observer) {
        service.removeObserver(observer);
    }
    public void setAlertThreshold(int value) {
        service.setThreshold(value);
    }
    public int getAlertThreshold() {
        return service.getThreshold();
    }
    public PollutionAlert checkAirQuality(String district,int aqi) {
        return service.check(district,aqi);
    }
    public List<PollutionAlert> getAlertHistory() {
        return service.history();
    }
    public List<PollutionAlert> getAlertsByDistrict(String district) {
        return service.historyByDistrict(district);
    }
    public static void main(String[] args) {
        AlertController c=new AlertController();
        c.addObserver(new OfficerAlertObserver("officer01"));
        c.addObserver(new ResidentAlertObserver("resident01",Arrays.asList("Shah Alam")));
        c.checkAirQuality("Shah Alam",165);
        c.checkAirQuality("Klang",85);
        c.checkAirQuality("Petaling Jaya",220);
        System.out.println("Total alerts: "+c.getAlertHistory().size());
    }
}
