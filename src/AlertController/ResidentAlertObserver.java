package AlertController;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ResidentAlertObserver implements AlertObserver {
    private final String username;
    private final Set<String> subscribedDistricts = new HashSet<>();

    public ResidentAlertObserver(String username, Collection<String> districts) {
        this.username = username;
        for (String district : districts == null ? java.util.List.<String>of() : districts) {
            if (district == null || district.isBlank()) continue;
            subscribedDistricts.add(district.trim().toLowerCase(java.util.Locale.ROOT));
        }
    }

    public String getUsername() { return username; }
    public boolean subscribesTo(String district) {
        return district != null && subscribedDistricts.contains(
                district.trim().toLowerCase(java.util.Locale.ROOT));
    }
    @Override public void update(PollutionAlert alert) {
        if (subscribesTo(alert.getDistrict())) {
            System.out.println("Resident " + username + ": " + alert);
        }
    }
}
