package AlertController;
import java.util.*;
public class OfficerAlertObserver implements AlertObserver {
    private final String officerName;
    private final List<PollutionAlert> received=new ArrayList<>();
    public OfficerAlertObserver(String officerName) {
        this.officerName=officerName;
    }
    public void onAlert(PollutionAlert alert) {
        received.add(alert);
        System.out.println("OFFICER "+officerName+": "+alert);
    }
    public List<PollutionAlert> getReceivedAlerts() {
        return Collections.unmodifiableList(received);
    }
}
