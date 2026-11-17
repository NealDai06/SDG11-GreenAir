package UserController;

import java.util.List;

/**
 * One GreenAir account, the {@code UserProfile} of the domain model.
 *
 * <p>Immutable: every change goes through the service, which builds a new instance and lets
 * {@link ProfileRepository} store it. {@code districts} are the districts whose pollution alerts
 * the account is subscribed to; residents keep several, officers and administrators usually none.</p>
 *
 * @param id         primary key in users.json, for example {@code U004}; blank for a profile that is not stored yet
 * @param username   unique log-in name, 3-30 characters of letters, digits, '.', '_' or '-'
 * @param fullName   the person's name as shown on the dashboards
 * @param role       what the account is allowed to do
 * @param groupType  population group used by the advice strategies
 * @param active     {@code false} once an administrator deactivates the account
 * @param districts  districts subscribed to pollution alerts, never {@code null}
 */
public record UserProfile(String id,
                          String username,
                          String fullName,
                          UserRole role,
                          GroupType groupType,
                          boolean active,
                          List<String> districts) {

    /** Validates the mandatory fields and takes a defensive copy of the districts. */
    public UserProfile {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("A username is required.");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("A full name is required.");
        }
        if (role == null) {
            throw new IllegalArgumentException("A role is required.");
        }
        if (groupType == null) {
            throw new IllegalArgumentException("A user group is required.");
        }
        districts = districts == null ? List.of() : List.copyOf(districts);
    }

    public boolean isAdministrator() {
        return role.isAdministrator();
    }

    /** {@code true} when the account wants the alerts of this district. */
    public boolean isSubscribedTo(String district) {
        return district != null && districts.stream().anyMatch(d -> d.equalsIgnoreCase(district));
    }

    /** Name plus role, for the "signed in as" banner of a dashboard. */
    public String displayName() {
        return fullName + " (" + role.label() + ")";
    }
}
