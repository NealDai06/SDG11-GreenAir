package AlertController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlertRepository {
    private final List<PollutionAlert> alerts = new ArrayList<>();
    public void save(PollutionAlert alert) { alerts.add(alert); }
    public List<PollutionAlert> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(alerts));
    }
    // User-specific filtering requires the user's subscribed districts.
    public List<PollutionAlert> findByUsername(String username) {
        throw new UnsupportedOperationException(
                "Provide user subscriptions via the service before filtering");
    }
}
