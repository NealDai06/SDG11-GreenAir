package UserController;

/**
 * The roles an account can hold, the {@code UserRole} enumeration of the domain model.
 *
 * <p>The three values map one to one to the dashboards of the view layer: a
 * {@link #RESIDENT} uses ResidentView, an {@link #ENVIRONMENT_OFFICER} uses OfficerView and an
 * {@link #ADMINISTRATOR} uses AdminView.</p>
 */
public enum UserRole {

    RESIDENT("Resident"),
    ENVIRONMENT_OFFICER("Environment Officer"),
    ADMINISTRATOR("Administrator");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    /** Readable name for the tables and the confirmation dialogs. */
    public String label() {
        return label;
    }

    public boolean isAdministrator() {
        return this == ADMINISTRATOR;
    }

    /** The value stored in users.json, for example {@code ADMINISTRATOR}. */
    public String storageValue() {
        return name();
    }

    /**
     * Reads a stored role, tolerating the shorter spellings that older files use
     * ({@code ADMIN}, {@code OFFICER}). An unknown value falls back to {@link #RESIDENT}, which is
     * the least privileged role, so a broken field can never hand out administrator rights.
     */
    public static UserRole fromStorage(String stored) {
        if (stored == null) return RESIDENT;
        String value = stored.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        return switch (value) {
            case "ADMINISTRATOR", "ADMIN" -> ADMINISTRATOR;
            case "ENVIRONMENT_OFFICER", "OFFICER" -> ENVIRONMENT_OFFICER;
            default -> RESIDENT;
        };
    }
}
