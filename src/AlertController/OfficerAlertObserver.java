package AlertController;

public class OfficerAlertObserver implements AlertObserver {
    private final String username;
    public OfficerAlertObserver(String username) { this.username = username; }
    public String getUsername() { return username; }
    @Override public void update(PollutionAlert alert) {
        System.out.println("Officer " + username + ": " + alert);
    }
}
