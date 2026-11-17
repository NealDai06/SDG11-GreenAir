package AlertController;
import java.util.*;
public class ResidentAlertObserver implements AlertObserver {
    private final String username;
    private final Set<String> districts=new HashSet<>();
    private final List<PollutionAlert> received=new ArrayList<>();
    public ResidentAlertObserver(String username,Collection<String> subscribedDistricts) {
        this.username=username;
        if(subscribedDistricts!=null)for(String d:subscribedDistricts)if(d!=null)districts.add(d.trim().toLowerCase(Locale.ROOT));
    }
    public void onAlert(PollutionAlert alert) {
        if(districts.contains(alert.getDistrict().toLowerCase(Locale.ROOT))) {
            received.add(alert);
            System.out.println("RESIDENT "+username+": "+alert);
        }
    }
    public List<PollutionAlert> getReceivedAlerts() {
        return Collections.unmodifiableList(received);
    }
}
