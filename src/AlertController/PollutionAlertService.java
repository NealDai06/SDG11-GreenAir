package AlertController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PollutionAlertService {
    private final AlertRepository alertRepository;
    private final List<AlertObserver> observerManager = new ArrayList<>();
    private final Map<String, ResidentAlertObserver> residents = new HashMap<>();
    private int threshold = 100;

    public PollutionAlertService(AlertRepository repository) {
        this.alertRepository = Objects.requireNonNull(repository);
    }

    public void addObserver(AlertObserver observer) {
        Objects.requireNonNull(observer);
        if (!observerManager.contains(observer)) observerManager.add(observer);
        if (observer instanceof ResidentAlertObserver) {
            ResidentAlertObserver resident = (ResidentAlertObserver) observer;
            residents.put(resident.getUsername(), resident);
        }
    }

    public void removeObserver(AlertObserver observer) {
        observerManager.remove(observer);
        if (observer instanceof ResidentAlertObserver) {
            ResidentAlertObserver resident = (ResidentAlertObserver) observer;
            residents.remove(resident.getUsername(), resident);
        }
    }

    public void setThreshold(int threshold) {
        if (threshold < 0 || threshold > 500) throw new IllegalArgumentException("Invalid AQI threshold");
        this.threshold = threshold;
    }

    // Integration adapter until AirQualityReading is provided by the other team.
    public void checkThreshold(String district, int aqi) {
        if (district == null || district.isBlank() || aqi < 0 || aqi > 500)
            throw new IllegalArgumentException("Invalid air quality reading");
        if (aqi > threshold) {
            String severity = aqi <= 50 ? "Good" : aqi <= 100 ? "Moderate" : aqi > 300 ? "Hazardous" : aqi > 200 ? "Very Unhealthy"
                    : aqi > 150 ? "Unhealthy" : "Unhealthy for Sensitive Groups";
            publishAlert(new PollutionAlert(district,
                    "AQI " + aqi + " exceeds threshold " + threshold, severity));
        }
    }

    public void publishAlert(PollutionAlert alert) {
        alertRepository.save(alert);
        for (AlertObserver observer : new ArrayList<>(observerManager)) observer.update(alert);
    }

    public List<PollutionAlert> getOfficerAlerts() { return alertRepository.findAll(); }

    public List<PollutionAlert> getResidentAlerts(String username) {
        ResidentAlertObserver resident = residents.get(username);
        List<PollutionAlert> result = new ArrayList<>();
        if (resident == null) return result;
        for (PollutionAlert alert : alertRepository.findAll()) {
            if (resident.subscribesTo(alert.getDistrict())) result.add(alert);
        }
        return result;
    }
}
