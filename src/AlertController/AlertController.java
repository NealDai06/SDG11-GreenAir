package AlertController;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AlertController {
    private final PollutionAlertService alertService;

    public AlertController() {
        this(new PollutionAlertService(new AlertRepository()));
    }

    public AlertController(PollutionAlertService service) {
        this.alertService = Objects.requireNonNull(service);
    }

    public List<PollutionAlert> getResidentAlerts(String username) {
        return alertService.getResidentAlerts(username);
    }

    public List<PollutionAlert> getOfficerAlerts() {
        return alertService.getOfficerAlerts();
    }

    public List<PollutionAlert> getHistory(String username) {
        return getResidentAlerts(username);
    }

    // Integration helpers for the UI and AirQualityController.
    public void registerObserver(AlertObserver observer) {
        alertService.addObserver(observer);
    }

    public void removeObserver(AlertObserver observer) { alertService.removeObserver(observer); }
    public void setAlertThreshold(int threshold) { alertService.setThreshold(threshold); }

    public void checkAirQuality(String district, int aqi) {
        alertService.checkThreshold(district, aqi);
    }

    public static void main(String[] args) {
        AlertController controller = new AlertController();
        controller.registerObserver(new OfficerAlertObserver("officer01"));
        controller.registerObserver(new ResidentAlertObserver(
                "resident01", Arrays.asList("Shah Alam")));
        controller.checkAirQuality("Shah Alam", 165);
        controller.checkAirQuality("Petaling Jaya", 220);
        System.out.println("Officer alerts: " + controller.getOfficerAlerts().size());
        System.out.println("Resident alerts: " + controller.getResidentAlerts("resident01").size());
    }
}
